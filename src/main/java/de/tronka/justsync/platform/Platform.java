package de.tronka.justsync.platform;

import de.tronka.justsync.compat.permission.JustSyncPermission;
import net.minecraft.commands.CommandSourceStack;
import java.nio.file.Path;

public interface Platform {
	boolean isModLoaded(String modId);

	ModLoader loader();

	String mcVersion();

	boolean isDevelopmentEnvironment();

	default boolean isDebug() {
		return isDevelopmentEnvironment();
	}

    Path getConfigDir();

    boolean checkPermission(CommandSourceStack source, JustSyncPermission permission);



	enum ModLoader {
		FABRIC, NEOFORGE, QUILT
	}
}
