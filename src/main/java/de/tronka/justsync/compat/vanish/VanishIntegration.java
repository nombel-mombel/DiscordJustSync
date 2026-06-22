package de.tronka.justsync.compat.vanish;

import net.minecraft.server.level.ServerPlayer;
import de.tronka.justsync.JustSyncApplication;
import de.tronka.justsync.config.Config;
import de.tronka.justsync.events.CoreEvents;
import de.tronka.justsync.events.payload.MinecraftToDiscordMessagePayload;

public class VanishIntegration {

    private boolean loaded = false;
    private boolean priorityMessageSending = false;
    private VanishProvider provider;

    public VanishIntegration(JustSyncApplication integration) {
        //? fabric {
        /*if (integration.getPlatform().isModLoaded("melius-vanish")) {
            provider = new MeliusVanishProvider();
        } else {
            return;
        }

        integration.registerConfigReloadHandler(this::onConfigLoaded);
        provider.registerVanishChangedHandler(this::onVanishChanged);
        CoreEvents.PLAYER_JOIN.addFilter(this::filterJoinLeave);
        CoreEvents.PLAYER_DISCONNECT.addFilter(this::filterJoinLeave);
        CoreEvents.PLAYER_TIMEOUT.addFilter(this::filterJoinLeave);
        ChatEvents.MINECRAFT_TO_DISCORD_CHAT_MESSAGE.addFilter(this::filterVanishChat);
        *///? }
    }

    private void onVanishChanged(ServerPlayer player, boolean isVanished) {
        if (!this.loaded) {
            return;
        }
        this.priorityMessageSending = true;
        if (isVanished) {
            CoreEvents.PLAYER_DISCONNECT.invoke(player);
        } else {
            CoreEvents.PLAYER_JOIN.invoke(player);
        }
        this.priorityMessageSending = false;
    }

    private boolean filterJoinLeave(ServerPlayer player) {
        return !this.isVanished(player) || this.priorityMessageSending;
    }

    private boolean filterVanishChat(MinecraftToDiscordMessagePayload payload) {
        return !this.isVanished(payload.player()) || this.priorityMessageSending;
    }

    private void onConfigLoaded(Config config) {
        this.loaded = config.integrations.enableVanishIntegration;
    }


    public boolean isVanished(ServerPlayer player) {
        return this.loaded && provider.isVanished(player);
    }
}
