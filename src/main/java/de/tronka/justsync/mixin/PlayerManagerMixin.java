package de.tronka.justsync.mixin;

import java.net.SocketAddress;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.tronka.justsync.CompatUtil;
import de.tronka.justsync.JustSyncApplication;
import de.tronka.justsync.events.CoreEvents;
import de.tronka.justsync.events.payload.JoiningPayload;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {

    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void canJoin(
            SocketAddress address, 
            /*$ profile_class {*/ net.minecraft.server.players.NameAndId/*$}*/ profile,
            CallbackInfoReturnable<Component> cir) {
        JustSyncApplication integration = JustSyncApplication.getInstance();
        if (!integration.isReady()) {
            cir.setReturnValue(Component.nullToEmpty("DiscordJS not ready, please try again in a few seconds."));
            return;
        }
        JoiningPayload payload = new JoiningPayload(CompatUtil.Profile.wrap(profile));
        CoreEvents.PLAYER_JOINING.invoke(payload);
        if (payload.isCanceled()) {
            cir.setReturnValue(Component.nullToEmpty(payload.getCancelReason()));
        }
    }

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void onPlayerJoin(Connection connection, ServerPlayer player, CommonListenerCookie clientData,
        CallbackInfo ci) {
        CoreEvents.PLAYER_JOIN.invoke(player);
    }

}
