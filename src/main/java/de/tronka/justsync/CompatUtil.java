package de.tronka.justsync;


import com.mojang.authlib.GameProfile;
import java.util.UUID;

public class CompatUtil {

    private CompatUtil() {}

    public record Profile(String name, UUID id) {
        //? if >= 1.21.9 {
        public static Profile wrap(net.minecraft.server.players.NameAndId profile) {
            return new Profile(profile.name(), profile.id());
        }
        //?}
        public static Profile wrap(GameProfile profile) {
            //? if >= 1.21.9 {
            return new Profile(profile.name(), profile.id());
            //?} else {
            /*return new Profile(profile.getName(), profile.getId());
            *///?}
        }
    }
}
