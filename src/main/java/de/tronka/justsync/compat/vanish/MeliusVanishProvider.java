package de.tronka.justsync.compat.vanish;

//? fabric {
/*import me.drex.vanish.api.VanishAPI;
import me.drex.vanish.api.VanishEvents;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.BiConsumer;

public class MeliusVanishProvider implements VanishProvider{

    @Override
    public void registerVanishChangedHandler(BiConsumer<ServerPlayer, Boolean> handler) {
        VanishEvents.VANISH_EVENT.register(handler::accept);
    }

    @Override
    public boolean isVanished(ServerPlayer player) {
        return VanishAPI.isVanished(player);
    }
}
*///? }
