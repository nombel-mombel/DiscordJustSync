package de.tronka.justsync;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import de.tronka.justsync.chat.DiscordChatMessageSender;
import de.tronka.justsync.config.Config;
import de.tronka.justsync.events.CoreEvents;
import de.tronka.justsync.events.payload.CommandPayload;

public class ConsoleBridge extends ListenerAdapter {

    private final JustSyncApplication integration;
    private TextChannel commandLogChannel;
    private TextChannel commandChannel;
    private Role opRole;
    private List<LogRedirect> logRedirects;
    private DiscordChatMessageSender messageSender;

    public ConsoleBridge(JustSyncApplication integration) {
        this.integration = integration;
        integration.registerConfigReloadHandler(this::onConfigLoaded);
        CoreEvents.COMMAND_EXECUTED.subscribe(this::onCommandExecute);
    }

    private void onConfigLoaded(Config config) {
        this.commandLogChannel =
            Utils.getTextChannel(
                this.integration.getJda(),
                config.commands.commandLogChannel,
                "commandLogChannel");
        this.commandChannel =
            Utils.getTextChannel(
                this.integration.getJda(),
                config.commands.commandChannel,
                "commandChannel");
        this.messageSender = null;
        String opRoleId = this.integration.getConfig().commands.opRole;
        if (this.commandChannel != null && !opRoleId.isEmpty()) {
            this.opRole = this.commandChannel.getGuild().getRoleById(opRoleId);
        } else {
            this.opRole = null;
        }
        this.logRedirects = new ArrayList<>();
        for (Config.LogRedirectChannel logRedirectChannel : config.commands.logRedirectChannels) {
            TextChannel channel =
                Utils.getTextChannel(
                    this.integration.getJda(),
                    logRedirectChannel.channel,
                    "logRedirectChannel");
            if (channel != null) {
                this.logRedirects.add(
                    new LogRedirect(channel, logRedirectChannel.redirectPrefixes));
            } else {
                String join = String.join(", ", logRedirectChannel.redirectPrefixes);
                LogUtils.getLogger()
                    .info(
                        "Could not load log redirect: ID: \"{}\", redirects: [{}]",
                        logRedirectChannel.channel,
                        join);
            }
        }
    }

    public void onCommandExecute(CommandPayload payload) {
        CommandSourceStack source = payload.source();
        String command = payload.command();
        if (this.commandLogChannel == null) {
            return;
        }
        if (!this.integration.getConfig().commands.logCommandsInConsole) {
            return;
        }

        if (source.getEntity() == null
            && !source.getTextName().equals("Server")
            && !this.integration.getConfig().commands.logCommandBlockCommands) {
            return;
        }

        if (Utils.startsWithAny(command, this.integration.getConfig().commands.ignoredCommands)) {
            return;
        }
        TextChannel target = this.commandLogChannel;
        for (LogRedirect redirect : this.logRedirects) {
            if (Utils.startsWithAny(command, redirect.prefixes)) {
                target = redirect.channel;
                break;
            }
        }
        String message =
            this.integration
                .getConfig()
                .messages
                .commandExecutedInfoText
                .replace("%user%", Utils.escapeUnderscores(source.getTextName()))
                .replace("%cmd%", command);

        if (this.messageSender == null || this.messageSender.hasChanged(message, null)) {
            this.messageSender =
                new DiscordChatMessageSender(
                    null, target, this.integration.getConfig(), message, null);
        }
        this.messageSender.sendMessage();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannel() != this.commandChannel || event.getMember() == null) {
            return;
        }

        String message = event.getMessage().getContentStripped();
        if (!message.startsWith(this.integration.getConfig().commands.commandPrefix)) {
            return;
        }

        if (!PermissionUtil.checkPermission(event.getMember(), Permission.ADMINISTRATOR) && !event.getMember()
            .getRoles().contains(this.opRole)) {
            event.getChannel().sendMessage("You don't have permission to use this command").queue();
            return;
        }
        message = message.substring(this.integration.getConfig().commands.commandPrefix.length());
        if (message.equals("help")) {
            event.getChannel().sendMessageEmbeds(this.getHelpEmbed()).queue();
            return;
        }
        String[] commandParts = message.split(" ", 2);
        String commandName = commandParts[0].toLowerCase();
        String commandArgs = commandParts.length == 2 ? commandParts[1] : "";
        Optional<Config.BridgeCommand> commandOptional = this.integration.getConfig().commands.commandList.stream()
            .filter(cmd -> cmd.commandName.equals(commandName)).findFirst();
        if (commandOptional.isPresent()) {
            Config.BridgeCommand command = commandOptional.get();
            DiscordCommandSender commandSender = new DiscordCommandSender(this.integration.getServer(),
                event.getAuthor().getAsMention(), feedback -> {
                if (feedback.length() > 2000) {
                    feedback = feedback.substring(0, 2000);
                }
                event.getChannel().sendMessage(feedback).queue();
            });
            String inGameCommand = command.inGameAction.replace("%args%", commandArgs);
            try {
                this.integration.getServer().getCommands().getDispatcher().execute(inGameCommand, commandSender);
            } catch (CommandSyntaxException e) {
                String messageToSend = "Error executing command: %s".formatted(e.getMessage());
                if (messageToSend.length() > 2000) {
                    messageToSend = messageToSend.substring(0, 2000);
                }
                event.getChannel().sendMessage(messageToSend).queue();
            }
        } else {
            event.getChannel().sendMessage("Unknown command: \"%s\", use %shelp for a list of commands"
                .formatted(commandName, this.integration.getConfig().commands.commandPrefix)).queue();
        }
    }

    private MessageEmbed getHelpEmbed() {
        String prefix = this.integration.getConfig().commands.commandPrefix;
        List<String> lines = this.integration.getConfig().commands.commandList.stream().map(
            command -> "%s%s <args> (`%s`)".formatted(prefix, command.commandName, command.inGameAction.replace("%args%", "<args>").trim())
        ).toList();

        EmbedBuilder embed = new EmbedBuilder();

        List<String> chunk = new ArrayList<>();
        int chunkSize = 0;
        for (String line : lines) {
            if (chunkSize + line.length() < 2000) {
                chunk.add(line);
                chunkSize += line.length() + 1;
            } else {
                embed.addField("Commands", String.join("\n", chunk), false);
                chunk = new ArrayList<>();
                chunk.add(line);
                chunkSize = line.length();
            }
        }
        embed.addField("Commands", String.join("\n", chunk), false);

        return embed.build();
    }

    private record LogRedirect(TextChannel channel, List<String> prefixes) {

    }
}
