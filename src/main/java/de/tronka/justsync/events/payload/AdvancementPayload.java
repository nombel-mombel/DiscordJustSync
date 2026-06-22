package de.tronka.justsync.events.payload;

import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;

public record AdvancementPayload(ServerPlayer player, Advancement advancement) {
}
