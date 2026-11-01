package tronka.justsync;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import tronka.justsync.config.Config;
import tronka.justsync.linking.PlayerLink;

import java.time.OffsetDateTime;
import java.util.Optional;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;

public class DiscordEvents extends ListenerAdapter {

    private final JustSyncApplication integration;

    public DiscordEvents(JustSyncApplication integration) {
        this.integration = integration;
        this.integration.registerConfigReloadHandler(this::onConfigLoaded);

    }

    private void onConfigLoaded(Config config) {
        for (Member member : this.integration.getGuild().getMembers()) {
            this.integration.getLuckPermsIntegration().evaluateRolesFor(member);
        }
    }

    @Override
    public void onGuildMemberUpdate(GuildMemberUpdateEvent event) {
        this.integration.getLuckPermsIntegration().evaluateRolesFor(event.getMember());

        if (!this.integration.getLinkManager().hasRequiredRoles(event.getMember())) {
            this.integration.getLinkManager().kickAccounts(event.getMember().getIdLong(),
                this.integration.getConfig().kickMessages.kickMissingRoles);
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (event.getGuild().getIdLong() != this.integration.getGuild().getIdLong()
                || event.getMember() == null) {
            return;
        }

        User user = event.getUser();
        Optional<PlayerLink> link = this.integration.getLinkManager().getDataOf(user.getIdLong());

        if (link.isEmpty()) {
            return;
        }

        if (!this.integration.getConfig().linking.banOnDiscordBan) {
            this.integration.getLinkManager().processMemberRemoval(user, false);
            return;
        }

        retrieveBanStatusAndProcessRemoval(event.getGuild(), user);
    }

    private void retrieveBanStatusAndProcessRemoval(Guild guild, User user) {
        if (!guild.getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            JustSyncApplication.LOGGER.warn(
                    "View Audit Logs permission is required to sync Discord bans, unlinking {}"
                            + " without checking for a ban",
                    user.getId());
            this.integration.getLinkManager().processMemberRemoval(user, false);
            return;
        }

        guild.retrieveAuditLogs()
                .type(ActionType.BAN)
                .limit(10)
                .queue(
                        entries -> {
                            OffsetDateTime timeCutoff = OffsetDateTime.now().minusMinutes(3);
                            Optional<AuditLogEntry> entry =
                                    entries.stream()
                                            .filter(e -> e.getTargetId().equals(user.getId()))
                                            .filter(e -> !e.getTimeCreated().isBefore(timeCutoff))
                                            .findFirst();

                            if (entry.isEmpty()) {
                                this.integration.getLinkManager().processMemberRemoval(user, false);
                                return;
                            }

                            this.integration
                                    .getLinkManager()
                                    .processMemberRemoval(
                                            user, true, entry.get().getReason());
                        });
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        if (event.getGuild().getIdLong() != this.integration.getGuild().getIdLong()
                || !this.integration.getConfig().linking.banOnDiscordBan) {
            return;
        }
        this.retrieveBanStatusAndProcessRemoval(event.getGuild(), event.getUser());
    }
}
