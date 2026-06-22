package de.tronka.justsync.compat.permission;

public enum PermissionLevel {
    ALL(0),
    MODERATORS(1),
    GAMEMASTERS(2),
    ADMINS(3),
    OWNERS(4);

    private int level;
    PermissionLevel(int level) {
        this.level = level;
    }
    //? if >= 1.21.11 {
    public net.minecraft.server.permissions.PermissionLevel getLevel() {
        return net.minecraft.server.permissions.PermissionLevel.byId(this.level);
    }
    //?} else {
    /*public int getLevel() {
        return this.level;
    }
    *///?}
}
