package de.tronka.justsync.compat.vanish;

import net.minecraft.server.level.ServerPlayer;
import java.util.function.BiConsumer;

public interface VanishProvider {
    void registerVanishChangedHandler(BiConsumer<ServerPlayer, Boolean> asd);
    boolean isVanished(ServerPlayer player);
}
