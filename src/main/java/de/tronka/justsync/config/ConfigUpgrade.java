package de.tronka.justsync.config;

import com.moandjiezana.toml.Toml;
import de.tronka.justsync.config.Config.MessageStrings;
import de.tronka.justsync.events.payload.MessageType;
import java.util.Map.Entry;

public class ConfigUpgrade {
    public static void upgradeConfig(Config config, Toml toml) {


        // 0 -> 1: migrate deprecated waypointURL and formatWaypoints fields
        if (!toml.contains("version")) {
            config.configVersion = 1;

            if (toml.contains("formatWaypoints")) {
                config.waypoints.formatWaypoints = toml.getBoolean("formatWaypoints");
            }
            String waypointURL = toml.getString("waypointURL", "");

            if (!waypointURL.isEmpty()) {
                config.waypoints.mapURLs.put("Overworld", waypointURL);
            }
        }

        // 1 -> 2: migrate deprecated consoleChannel -> commandChannel/commandLogChannel
        if (config.configVersion < 2) {
            String consoleChannel = toml.getString("consoleChannel", "");
            if (!consoleChannel.isEmpty()) {
                config.commands.commandLogChannel = consoleChannel;
                config.commands.commandChannel = consoleChannel;
            }

        }
        // 2 -> 3: migrate deprecated floodgate fields
        if (config.configVersion < 3) {
            if (toml.contains("integrations.floodgate.allowMixedAccountTypes")) {
                config.integrations.floodgate.allowLinkingMixedAccountTypes =
                        toml.getBoolean("integrations.floodgate.allowMixedAccountTypes");
            }
            if (toml.contains("integrations.floodgate.allowMixedAccountTypesBypass")) {
                config.integrations.floodgate.allowLinkingMixedAccountTypesBypass = toml.getList("integrations.floodgate.allowMixedAccountTypesBypass");
            }
            if (toml.contains("integrations.floodgate.mixedAccountTypeDenyMessage")) {
                config.integrations.floodgate.linkingMixedAccountTypesDenyMessage =
                        toml.getString("integrations.floodgate.mixedAccountTypeDenyMessage");
            }
            config.configVersion = 3;
        }

        // 3 -> 4: keep previous behavior for old users
        if (config.configVersion < 4) {
            config.integrations.luckPerms.assignSyncedRolesToAlts = false;
            config.configVersion = 4;
        }

        // restore missing format items
        for (Entry<MessageType, MessageFormat> entries : MessageStrings.DEFAULT_FORMATS.entrySet()) {
            config.messages.formats.putIfAbsent(entries.getKey(), entries.getValue());
        }

        // 4 -> 5: migrate message-format fields
        if (config.configVersion < 5) {
            if (toml.contains("useWebHooks")) {
                config.messages.formats.get(MessageType.CHAT).mode =
                        toml.getBoolean("useWebHooks") ? MessageFormat.SendType.WEBHOOK : MessageFormat.SendType.DEFAULT;
            }

            if (toml.contains("messages.playerJoinMessage")) {
                config.messages.formats.get(MessageType.JOIN).format = toml.getString("messages.playerJoinMessage");
            }
            if (toml.contains("messages.playerLeaveMessage")) {
                config.messages.formats.get(MessageType.LEAVE).format = toml.getString("messages.playerLeaveMessage");
            }
            if (toml.contains("messages.playerTimeOutMessage")) {
                config.messages.formats.get(MessageType.TIMEOUT).format = toml.getString("messages.playerTimeOutMessage");
            }
            if (toml.contains("messages.advancementMessage")) {
                config.messages.formats.get(MessageType.ADVANCEMENT).format = toml.getString("messages.advancementMessage");
            }
            if (toml.contains("messages.startMessage")) {
                config.messages.formats.get(MessageType.SERVER_START).format = toml.getString("messages.startMessage");
            }
            if (toml.contains("messages.stopMessage")) {
                config.messages.formats.get(MessageType.SERVER_STOP).format = toml.getString("messages.stopMessage");
            }

            config.configVersion = 5;
        }

    }
}
