package de.tronka.justsync;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import de.tronka.justsync.config.Config;

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
            this.integration.getLinkManager().kickAccounts(event.getMember(),
                this.integration.getConfig().kickMessages.kickMissingRoles);
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (event.getMember() == null) {
            return;
        }
        this.integration.getLinkManager().onMemberRemoved(event.getMember());
    }
}
