package de.tronka.justsync.compat.permission;

//? neoforge {
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
//? }

public enum JustSyncPermission {
    GET("get", PermissionLevel.ADMINS),
    RELOAD("reload", PermissionLevel.OWNERS),
    UNLINK_SELF("unlink", PermissionLevel.ALL),
    UNLINK_OTHER("unlink.other", PermissionLevel.ADMINS),
    SEND_MSG("sendmsg", PermissionLevel.ADMINS);

    private static final String PERMISSION_PREFIX = "justsync";
    public PermissionLevel defaultLevel;
    public String permissionId;
    JustSyncPermission(String name, PermissionLevel defaultLevel) {
        this.defaultLevel = defaultLevel;
        this.permissionId = PERMISSION_PREFIX + "." + name;
        //? neoforge {
        this.node = new PermissionNode<>("justsync",
            name,
            PermissionTypes.BOOLEAN,
            (player, uuid, contexts) -> false);
        //? }
    }

    //? neoforge {
    public final PermissionNode<Boolean> node;
    //? }
}
