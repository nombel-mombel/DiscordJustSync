package de.tronka.justsync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.tronka.justsync.compat.permission.JustSyncPermission;
import de.tronka.justsync.events.CoreEvents;
import net.dv8tion.jda.api.entities.Member;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import de.tronka.justsync.events.ChatEvents;
import de.tronka.justsync.events.payload.MessageType;
import de.tronka.justsync.events.payload.MinecraftToDiscordMessagePayload;
import de.tronka.justsync.linking.PlayerData;
import de.tronka.justsync.linking.PlayerLink;

public class InGameCommand {

    private final JustSyncApplication integration;

    public InGameCommand(JustSyncApplication integration) {
        this.integration = integration;
        CoreEvents.REGISTER_COMMAND.subscribe(this::register);
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("discord")
                .then(this.unlinkSubcommand())
                .then(this.reloadSubcommand())
                .then(this.getInfoSubcommand())
                .then(this.discordMessageSubCommand())
        );
    }

    private LiteralArgumentBuilder<CommandSourceStack> getInfoSubcommand() {
        return Commands.literal("get")
                .executes(this::getSelfLinkInfo)
                .then(
                        Commands.argument("player", GameProfileArgument.gameProfile())
                                .requires(
                                        this.integration.requirePermission(JustSyncPermission.GET))
                                .requires(
                                        this.integration.requirePermission(JustSyncPermission.GET))
                                .executes(this::getLinkInfo));
    }

    private int getLinkInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<CompatUtil.Profile> profiles = GameProfileArgument.getGameProfiles(context, "player")
                .stream().map(CompatUtil.Profile::wrap).toList();
        Collection<String> lines = new ArrayList<>();
        for (CompatUtil.Profile profile : profiles) {
            Optional<PlayerLink> optionalLink = this.integration.getLinkManager().getDataOf(profile.id());
            if (optionalLink.isEmpty()) {
                lines.add("No records for " + profile.name());
            } else {
                Optional<Member> member = this.integration.getLinkManager().getDiscordOf(optionalLink.get());
                if (member.isPresent()) {
                    lines.add(formatPlayerInfo(optionalLink.get(), member.get()));
                } else {
                    lines.add("Unable to load discord member for " + profile.name());

                }
            }

        }
        context.getSource().sendSuccess(() -> Component.literal(String.join("\n", lines)), false);
        return 1;
    }

    private int getSelfLinkInfo(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendSuccess(() -> Component.literal("Player only!"),
                false);
            return 1;
        }
        Optional<PlayerLink> playerLinkOptional = this.integration.getLinkManager()
            .getDataOf(player.getUUID());
        if (playerLinkOptional.isEmpty()) {
            context.getSource().sendSuccess(
                () -> Component.literal("Player is not linked"),
                false);
            return 1;
        }
        PlayerLink playerLink = playerLinkOptional.get();

        Optional<Member> member = this.integration.getLinkManager()
            .getDiscordOf(playerLink.getPlayerId());

        if (member.isEmpty()) {
            context.getSource().sendSuccess(
                () -> Component.literal("Discord member not found"),
                false);
            return 1;
        }

        String message = formatPlayerInfo(playerLink, member.get());
        context.getSource()
            .sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static String formatPlayerInfo(PlayerLink playerLink, Member member) {
        StringBuilder message = new StringBuilder()
            .append(playerLink.getPlayerName())
            .append(" (@")
            .append(member.getEffectiveName())
            .append(")");

        if (playerLink.altCount() > 0) {
            message.append(" Alts: ").append(String.join(", ", playerLink.getAlts()
                .stream().map(PlayerData::getName).toList()));
        }
        return message.toString();
    }

    private LiteralArgumentBuilder<CommandSourceStack> reloadSubcommand() {
        return Commands.literal("reload")
                .requires(
                        this.integration.requirePermission(JustSyncPermission.RELOAD))
                .executes(this::reloadConfigs);
    }

    private int reloadConfigs(CommandContext<CommandSourceStack> context) {
        String result = this.integration.tryReloadConfig();
        final String feedback = result.isEmpty() ? "Successfully reloaded config!" : result;
        context.getSource().sendSuccess(() -> Component.literal(feedback), result.isEmpty());
        return 1;
    }

    private LiteralArgumentBuilder<CommandSourceStack> unlinkSubcommand() {
        return Commands.literal("unlink")
                .requires(this.integration.requirePermission(JustSyncPermission.UNLINK_SELF))
                .executes(this::unlinkSelf)
                .then(
                        Commands.argument("player", GameProfileArgument.gameProfile())
                                .requires(
                                        this.integration.requirePermission(
                                                JustSyncPermission.UNLINK_OTHER))
                                .executes(this::unlinkSpecifiedPlayer));
    }

    private int unlinkSpecifiedPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<CompatUtil.Profile> profiles = GameProfileArgument.getGameProfiles(context, "player")
                .stream().map(CompatUtil.Profile::wrap).toList();
        int count = 0;
        for (CompatUtil.Profile profile : profiles) {
            if (this.integration.getLinkManager().unlinkPlayer(profile.id())) {
                count++;
            }
        }
        if (count > 0) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "Successfully unlinked %d player(s)".formatted(profiles.size())),
                false);
            return count;
        }
        context.getSource().sendSuccess(() -> Component.literal("Found no linked players!"), false);
        return 0;
    }

    private int unlinkSelf(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            this.integration.getLinkManager().unlinkPlayer(player.getUUID());
            context.getSource().sendSuccess(() -> Component.literal("Unlinked!"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("Player Only!"), false);
        }
        return 1;
    }

    private LiteralArgumentBuilder<CommandSourceStack> discordMessageSubCommand() {
        return Commands.literal("sendMsg")
                .requires(this.integration.requirePermission(JustSyncPermission.SEND_MSG))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(this::sendDiscordMessage));
    }

    private int sendDiscordMessage(CommandContext<CommandSourceStack> context) {
        String msg = StringArgumentType.getString(context, "message");

        ChatEvents.MINECRAFT_TO_DISCORD_CHAT_MESSAGE.invoke(
                new MinecraftToDiscordMessagePayload(
                        Map.of("%msg%", msg, "%user%", "Server"), null, MessageType.CHAT));

        context.getSource().sendSuccess(() -> Component.literal("Sent message: ").append(Component.literal(msg).withStyle(
            ChatFormatting.ITALIC, ChatFormatting.GRAY)), false);
        return 1;
    }
}
