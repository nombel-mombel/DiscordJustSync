package tronka.justsync.compat;

import eu.pb4.banhammer.api.BanHammer;
import eu.pb4.banhammer.api.PunishmentType;

import net.fabricmc.loader.api.FabricLoader;

import tronka.justsync.events.CoreEvents;
import tronka.justsync.events.payload.MinecraftChatMessagePayload;

import java.util.UUID;

public class BanHammerIntegration {

    private boolean banhammerInstalled;

    public BanHammerIntegration() {
        this.banhammerInstalled = FabricLoader.getInstance().isModLoaded("banhammer");
        CoreEvents.MINECRAFT_CHAT_MESSAGE.addFilter(this::shouldRelay);
    }

    private boolean shouldRelay(MinecraftChatMessagePayload payload) {
        return !isMuted(payload.player().getUUID());
    }

    public boolean isMuted(UUID uuid) {
        if (!this.banhammerInstalled) {
            return false;
        }
        return BanHammer.isPunished(uuid, PunishmentType.MUTE);
    }
}
