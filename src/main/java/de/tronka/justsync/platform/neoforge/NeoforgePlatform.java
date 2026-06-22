package de.tronka.justsync.platform.neoforge;

//? neoforge {

import de.tronka.justsync.compat.permission.JustSyncPermission;
import de.tronka.justsync.platform.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission.HasCommandLevel;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import java.nio.file.Path;

public class NeoforgePlatform implements Platform {

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public ModLoader loader() {
		return ModLoader.NEOFORGE;
	}

	@Override
	public String mcVersion() {
		return "";
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return !FMLLoader/*? if > 1.21.7 {*/.getCurrent()/*?}*/.isProduction();
	}

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean checkPermission(CommandSourceStack source, JustSyncPermission permission) {
        if (source.getPlayer() != null && PermissionAPI.getPermission(source.getPlayer(), permission.node)) {
            return true;
        }
        return source.permissions().hasPermission(new HasCommandLevel(permission.defaultLevel.getLevel()));
    }
}
//?}
