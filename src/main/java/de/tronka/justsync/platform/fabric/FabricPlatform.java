package de.tronka.justsync.platform.fabric;

//? fabric {

/*import de.tronka.justsync.compat.permission.JustSyncPermission;
import de.tronka.justsync.platform.Platform;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import java.nio.file.Path;

public class FabricPlatform implements Platform {

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public ModLoader loader() {
		return ModLoader.FABRIC;
	}

	@Override
	public String mcVersion() {
		return FabricLoader.getInstance().getRawGameVersion();
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean checkPermission(CommandSourceStack source, JustSyncPermission permission) {
        return Permissions.check(source, permission.permissionId, permission.defaultLevel.getLevel());
    }
}
*///?}
