package de.tronka.justsync.events.payload;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public record MinecraftToDiscordMessagePayload(
        Map<String, String> replacements, ServerPlayer player, MessageType type) {}
