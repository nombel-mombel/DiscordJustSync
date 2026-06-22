package de.tronka.justsync.compat;

import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

public class LuckPermsHelper {

    private LuckPermsHelper() {}

    public static Node getNode(String name) {
        return InheritanceNode.builder(name).build();
    }
}
