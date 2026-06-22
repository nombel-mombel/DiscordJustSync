package de.tronka.justsync.platform.neoforge;

//? neoforge {

import de.tronka.justsync.JustSyncApplication;
import de.tronka.justsync.compat.permission.JustSyncPermission;
import de.tronka.justsync.events.CoreEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

@Mod(JustSyncApplication.MOD_ID)
public class NeoforgeEntrypoint {

	public NeoforgeEntrypoint() {
        NeoForge.EVENT_BUS.register(this);
		new JustSyncApplication(new NeoforgePlatform()).onInitialize();
	}

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CoreEvents.REGISTER_COMMAND.invoke(event.getDispatcher());
    }

    @SubscribeEvent
    public void serverStarting(ServerStartingEvent event) {
        CoreEvents.SERVER_STARTING.invoke(event.getServer());
    }

    @SubscribeEvent
    public void serverStopped(ServerStoppedEvent event) {
        CoreEvents.SERVER_STOPPED.invoke(event.getServer());
    }

    @SubscribeEvent
    public void serverStopping(ServerStoppingEvent event) {
        CoreEvents.SERVER_STOPPING.invoke(event.getServer());
    }

    @SubscribeEvent
    public void gatherPermissions(PermissionGatherEvent.Nodes event) {
        for (JustSyncPermission permission : JustSyncPermission.values()) {
            event.addNodes(permission.node);
        }
    }
}
//?}
