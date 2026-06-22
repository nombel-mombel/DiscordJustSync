package de.tronka.justsync.events.payload;

import net.minecraft.server.level.ServerPlayer;

public record MinecraftChatMessagePayload(ServerPlayer player, String message) {
}
