package tronka.justsync.mixin;

import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
//? if >=26.3 {
/*import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import tronka.justsync.events.payload.CommandPayload;
*///?}
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tronka.justsync.events.CoreEvents;
import tronka.justsync.events.payload.MinecraftChatMessagePayload;

@Mixin(ServerGamePacketListenerImpl.class)
public class NetworkHandlerMixin {

    @Shadow
    public ServerPlayer player;

    //? if >=26.3 {
    /*// The switcher now uses a packet instead of executing a command. Log only
    // after vanilla has moved to the server thread and accepted permissions.
    @Inject(method = "handleChangeGameMode", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/commands/GameModeCommand;setGameMode(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/GameType;)V"))
    private void onChangeGameMode(ServerboundChangeGameModePacket packet, CallbackInfo ci) {
        CoreEvents.COMMAND_EXECUTED.invoke(new CommandPayload(
            this.player.createCommandSourceStack(), "gamemode " + packet.mode().getName()));
    }
    *///?}

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onPlayerLeave(DisconnectionDetails info, CallbackInfo ci) {
        if (!info.reason().toString().contains("disconnect.timeout")) {
            CoreEvents.PLAYER_DISCONNECT.invoke(this.player);
        } else {
            CoreEvents.PLAYER_TIMEOUT.invoke(this.player);
        }
    }

    @Inject(method = "getSignedMessage", at = @At("RETURN"))
    private void onMessageValidated(ServerboundChatPacket packet, LastSeenMessages lastSeenMessages,
        CallbackInfoReturnable<PlayerChatMessage> cir) {
        CoreEvents.MINECRAFT_CHAT_MESSAGE.invoke(new MinecraftChatMessagePayload(this.player, packet.message()));
    }

}
