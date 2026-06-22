package de.tronka.justsync.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.tronka.justsync.events.CoreEvents;
import de.tronka.justsync.events.payload.AdvancementPayload;

@Mixin(PlayerAdvancements.class)
public class AdvancementMixin {

    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerAdvancements;markForVisibilityUpdate(Lnet/minecraft/advancements/AdvancementHolder;)V"))
    private void receiveAdvancement(AdvancementHolder advancementEntry, String criterionName,
        CallbackInfoReturnable<Boolean> cir) {
        CoreEvents.ADVANCEMENT_GRANTED.invoke(new AdvancementPayload(this.player, advancementEntry.value()));
    }
}
