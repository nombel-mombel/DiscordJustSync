package de.tronka.justsync.mixin;


import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.tronka.justsync.events.CoreEvents;
import de.tronka.justsync.events.payload.CommandPayload;

@Mixin(Commands.class)
public class CommandManagerMixin {

    @Inject(method = "performCommand", at = @At("HEAD"))
    private void onExecuteCommand(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo ci) {
        CoreEvents.COMMAND_EXECUTED.invoke(new CommandPayload(parseResults.getContext().getSource(), command));
    }
}
