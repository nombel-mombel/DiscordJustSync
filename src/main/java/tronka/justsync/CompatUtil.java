package tronka.justsync;


import com.mojang.authlib.GameProfile;
import java.util.UUID;

public class CompatUtil {

    private CompatUtil() {}

    public enum PermissionLevel {
       ALL(0),
       MODERATORS(1),
       GAMEMASTERS(2),
       ADMINS(3),
       OWNERS(4);

        private int level;
        private PermissionLevel(int level) {
            this.level = level;
        }
    }

    //? if >= 1.21.11 {
    public static net.minecraft.server.permissions.PermissionLevel getPermissionLevel(
            PermissionLevel level) {
        return net.minecraft.server.permissions.PermissionLevel.byId(level.level);
    }
    //?} else {
    /*public static int getPermissionLevel(PermissionLevel level) {
        return level.level;
    }
    *///?}

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
