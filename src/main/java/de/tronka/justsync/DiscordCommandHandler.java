package de.tronka.justsync;

import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import de.tronka.justsync.linking.PlayerData;
import de.tronka.justsync.linking.PlayerLink;


public class DiscordCommandHandler extends ListenerAdapter {

    JustSyncApplication integration;

    public DiscordCommandHandler(JustSyncApplication integration) {
        this.integration = integration;
        integration.getJda().updateCommands().queue();
        integration.getGuild().updateCommands().addCommands(
                Commands.slash("link", "Link your minecraft with the code you got when joining")
                        .addOption(OptionType.STRING, "code", "Link code", true),
                Commands.slash("unlink", "Unlink your discord account from every linked minecraft account"),
                Commands.slash("list", "List the currently online players"),

                // restricted
                Commands.slash("justsync", "Misc linking stuff").addSubcommands(
                        new SubcommandData("get", "Retrieve linking information").addOption(OptionType.USER, "user",
                                "whose data to get")
                                .addOption(OptionType.STRING, "mc-name", "minecraft username to get data"),
                        new SubcommandData("unlink", "Unlink your account")
                                .addOption(OptionType.USER, "user", "user to unlink")
                                .addOption(OptionType.STRING, "mc-name", "minecraft account to unlink"),
                        new SubcommandData("alts", "shows all alts"))
                        .setDefaultPermissions(
                                DefaultMemberPermissions.enabledFor(
                                        Permission.MODERATE_MEMBERS)),
                Commands.slash("reload", "Reload the config file")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)))
                .queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getMember() == null || event.getMember().getGuild() != this.integration.getGuild()) {
            return;
        }
        switch (event.getName()) {

            case "link" -> {
                String code = event.getOptions().getFirst().getAsString();
                String message = this.integration.getLinkManager().confirmLink(event.getUser().getIdLong(), code);
                event.reply(message).setEphemeral(true).queue();
            }

            case "unlink" -> {
                Optional<PlayerLink> dataOptional = this.integration.getLinkManager()
                        .getDataOf(event.getMember().getIdLong());
                if (dataOptional.isEmpty()) {
                    event.reply(event.getMember().getAsMention() + " is not linked to any player")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                String message;
                if (this.integration.getLinkManager().unlinkPlayer(event.getMember().getIdLong())) {
                    message = "Successfully unlinked";
                } else {
                    message = "No linked player found";
                }
                event.reply(message).setEphemeral(true).queue();

            }

            case "list" -> this.listCommand(event);

            case "justsync" -> this.linkingCommand(event);

            case "reload" -> {
                String result = this.integration.tryReloadConfig();
                if (result.isEmpty()) {
                    result = "Reloaded config!";
                }
                event.reply(result).setEphemeral(true).queue();
            }
            default -> {}
        }
    }

    private void listCommand(SlashCommandInteractionEvent event) {
        List<String> namesList = this.integration.getServer().getPlayerList().getPlayers().stream()
                .filter(player -> !this.integration.getVanishIntegration().isVanished(player))
                .map(player -> player.getName().tryCollapseToString())
                .map(Utils::escapeUnderscores)
                .toList();

        String nameList = String.join(", ", namesList);
        String message;

        if (namesList.size() > 1) {
            message = "There are currently " + namesList.size() + " players online:";
        } else if (namesList.size() == 1) {
            message = "There is currently " + namesList.size() + " player online:";
        } else {
            message = "There are currently no players online";
        }

        event.reply(message + "\n" + nameList).setEphemeral(true).queue();

    }

    private void linkingCommand(SlashCommandInteractionEvent event) {
        if (event.getMember() == null) {
            event.reply("Invalid Context").setEphemeral(true).queue();
            return;
        }

        if (event.getSubcommandName().equals("alts")) {
            this.listAlts(event);
            return;
        }


        Member target = event.getOption("user", OptionMapping::getAsMember);
        String minecraftName = event.getOption("mc-name", OptionMapping::getAsString);

        if (minecraftName != null) {
            if (PermissionUtil.checkPermission(event.getMember(), Permission.MODERATE_MEMBERS)) {
                event.deferReply().setEphemeral(true).queue();
                CompatUtil.Profile profile = Utils.fetchProfile(minecraftName);
                this.linkingWithPlayer(event.getSubcommandName(), event.getHook(), profile);
                return;
            } else {
                event.reply("Insufficient permissions").setEphemeral(true).queue();
            }
            return;
        } else if (target == null) {
            target = event.getMember();
        }
        boolean isSelf = event.getMember().equals(target);
        if (!isSelf && !PermissionUtil.checkPermission(event.getMember(), Permission.MODERATE_MEMBERS)) {
            event.reply("Insufficient permissions").setEphemeral(true).queue();
            return;
        }
        this.linkingWithMember(event, target);
    }

    private void listAlts(SlashCommandInteractionEvent event) {
        List<PlayerLink> linksWithAlts = this.integration.getLinkManager().getAllLinks()
                .filter(link -> link.altCount() > 0).toList();
        StringBuilder message = new StringBuilder();
        for (PlayerLink link : linksWithAlts) {
            List<String> alts = link.getAlts().stream().map(PlayerData::getName).toList();
            String altsList = String.join("`, `", alts);
            message.append("`").append(link.getPlayerName()).append("`: `").append(altsList).append("`\n");
        }
        String finalMessage = message.isEmpty() ? "No alts linked" : message.toString();
        event.reply(finalMessage).setEphemeral(true).queue();
    }

	// run with checked permissions
    private void linkingWithMember(SlashCommandInteractionEvent event, Member target) {
        Optional<PlayerLink> dataOptional = this.integration.getLinkManager().getDataOf(target.getIdLong());
        if (dataOptional.isEmpty()) {
            event.reply(target.getAsMention() + " is not linked to any player").setEphemeral(true).queue();
            return;
        }
        PlayerLink data = dataOptional.get();
        if (Objects.equals(event.getSubcommandName(), "get")) {
            event.deferReply().setEphemeral(true).queue();
            String text = target.getAsMention() + " is linked to " + data.getPlayerName();
            if (data.altCount() != 0) {
                text += "\nwith " + data.altCount() + " alts: " + String.join(", ",
                    data.getAlts().stream().map(PlayerData::getName).toList());
            }
            event.getHook().editOriginal(text).queue();
        } else if (Objects.equals(event.getSubcommandName(), "unlink")) {
            String message;
            if (this.integration.getLinkManager().unlinkPlayer(target.getIdLong())) {
                message = "Successfully unlinked";
            } else {
                message = "No linked player found";
            }
            event.reply(message).setEphemeral(true).queue();
        }
    }

    // run with checked permissions
    private void linkingWithPlayer(String subCommand, InteractionHook hook, CompatUtil.Profile profile) {
        if (profile == null) {
            hook.editOriginal("Could not find a player with that name").queue();
            return;
        }

        Optional<PlayerLink> data = this.integration.getLinkManager().getDataOf(profile.id());
        if (data.isEmpty()) {
            hook.editOriginal("Could not find a linked account").queue();
            return;
        }
        if (Objects.equals(subCommand, "get")) {
            Optional<Member> member = this.integration.getLinkManager().getDiscordOf(data.get());
            if (member.isPresent()) {
                hook.editOriginal(profile.name() + " is linked to " + member.get().getAsMention()).queue();
            } else {
                hook.editOriginal("Could not find a linked discord account").queue();
            }
        } else if (Objects.equals(subCommand, "unlink")) {
            if (data.get().getPlayerName().equalsIgnoreCase(profile.name())) {
                this.integration.getLinkManager().unlinkPlayer(data.get());
            } else {
                UUID altUuid = data.get().getAlts().stream()
                    .filter((alt) -> alt.getName().equalsIgnoreCase(profile.name()))
                    .findFirst().get().getId();
                this.integration.getLinkManager().unlinkAlt(data.get(), altUuid);
            }
            hook.editOriginal("Successfully unlinked").queue();
        }
    }
}
