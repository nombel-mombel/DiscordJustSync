package de.tronka.justsync.platform.fabric;

//? fabric {

/*import com.mojang.brigadier.CommandDispatcher;
import de.tronka.justsync.JustSyncApplication;
import de.tronka.justsync.events.CoreEvents;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands.CommandSelection;

@Entrypoint("server")
public class FabricEntrypoint implements ModInitializer {

	@Override
	public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(CoreEvents.SERVER_STARTING::invoke);
        ServerLifecycleEvents.SERVER_STOPPING.register(CoreEvents.SERVER_STOPPING::invoke);
        ServerLifecycleEvents.SERVER_STOPPED.register(CoreEvents.SERVER_STOPPED::invoke);
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
        new JustSyncApplication(new FabricPlatform()).onInitialize();
	}

    private void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, CommandSelection selection) {
        CoreEvents.REGISTER_COMMAND.invoke(dispatcher);
    }
}
*///?}
