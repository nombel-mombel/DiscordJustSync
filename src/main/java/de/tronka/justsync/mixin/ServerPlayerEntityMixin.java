package de.tronka.justsync.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.tronka.justsync.events.CoreEvents;
import de.tronka.justsync.events.payload.DeathPayload;

@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void onDeath(DamageSource damageSource, CallbackInfo ci) {
        CoreEvents.PLAYER_DEATH.invoke(new DeathPayload((ServerPlayer) (Object) this, damageSource));
    }
}
