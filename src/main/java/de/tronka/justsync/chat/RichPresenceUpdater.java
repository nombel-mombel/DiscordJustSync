package de.tronka.justsync.chat;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.minecraft.server.level.ServerPlayer;
import de.tronka.justsync.JustSyncApplication;
import de.tronka.justsync.config.Config;
import de.tronka.justsync.events.CoreEvents;

public class RichPresenceUpdater {
    private final JustSyncApplication integration;
    private int onlineCount = 0;
    public RichPresenceUpdater(JustSyncApplication integration) {
        this.integration = integration;
        integration.registerConfigReloadHandler(this::onConfigLoaded);

        CoreEvents.PLAYER_JOIN.subscribe(this::onPlayerJoin);
        CoreEvents.PLAYER_DISCONNECT.subscribe(this::onPlayerLeave);
        CoreEvents.PLAYER_TIMEOUT.subscribe(this::onPlayerLeave);
    }

    private void onConfigLoaded(Config config) {
        if (this.integration.getServer() != null && this.integration.getServer().getPlayerList() != null) {
            this.onlineCount = (int) this.integration.getServer().getPlayerList()
                    .getPlayers().stream()
                    .filter(p -> !this.integration.getVanishIntegration().isVanished(p))
                    .count();
        }
        this.updateRichPresence();
        if (!config.showPlayerCountStatus) {
            this.integration.getJda().getPresence().setActivity(null);
        }
        if (!config.showAsIdle) {
            this.integration.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
        }
    }

    private void onPlayerJoin(ServerPlayer player) {
        this.onlineCount++;
        this.updateRichPresence();
    }

    private void onPlayerLeave(ServerPlayer player) {
        this.onlineCount--;
        this.updateRichPresence();
    }

    private void updateRichPresence() {
        if (this.integration.getConfig().showPlayerCountStatus) {
            String message = switch (this.onlineCount) {
                case 0 -> this.integration.getConfig().messages.onlineCountZero;
                case 1 -> this.integration.getConfig().messages.onlineCountSingular;
                default -> this.integration.getConfig().messages.onlineCountPlural.formatted(this.onlineCount);
            };
            Activity activity = message.isBlank() ? null : Activity.playing(message);
            this.integration.getJda().getPresence().setActivity(activity);
        }
        if (this.integration.getConfig().showAsIdle) {
            OnlineStatus status = this.onlineCount == 0 ? OnlineStatus.IDLE : OnlineStatus.ONLINE;
            this.integration.getJda().getPresence().setStatus(status);
        }
    }
}
