package de.tronka.justsync.events.payload;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public record DeathPayload(ServerPlayer player, DamageSource damageSource) {
}
