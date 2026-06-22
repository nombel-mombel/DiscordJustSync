package de.tronka.justsync.config;

import org.slf4j.Logger;

import de.tronka.justsync.config.MessageFormat.SendType;
import de.tronka.justsync.events.payload.MessageType;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ConfigValidator {

    private final Config config;
    private Map<String, String> errors = new HashMap<>();
    private Map<String, String> warnings = new HashMap<>();

    public ConfigValidator(Config config) {
        this.config = config;
    }

    public void validate() {
        // bot token
        if (config.botToken.isBlank()) {
            this.errors.put("botToken", "Bot Token is not set");
        }

        if (!isValidSnowflake(config.serverChatChannel)) {
            this.errors.put("serverChatChannel", "Server Chat Channel Id is invalid");
        }

        if (config.linking.logLinking && !isValidSnowflake(config.linking.linkingLogChannel)) {
            this.errors.put(
                    "linking.linkingLogChannel",
                    "Logging linking events is enabled, but no valid channel id is provided");
        }

        if (config.linking.linkCodeExpireMinutes > 30) {
            this.warnings.put(
                    "linking.linkCodeExpireMinutes",
                    "a lower amount of minutes for code expiry is recommended");
        }

        if (!config.kickMessages.kickLinkCode.contains("%s")) {
            this.errors.put("kickMessages.kickLinkCode", "%s must be specified as placeholder");
        }

        // check for customname
        for (Entry<MessageType, MessageFormat> format : config.messages.formats.entrySet()) {
            if ((format.getValue().mode.equals(SendType.WEBHOOK)
                            || format.getValue().mode.equals(SendType.EMBED))
                    && (format.getValue().customName == null
                            || format.getValue().customName.isBlank())) {
                this.warnings.put(
                        "config.message.format." + format.getKey().toString(),
                        "webhook or embed mode is selected but no custom name is set");
            }
        }

        if (!config.messages.formats.get(MessageType.CHAT).format.contains("%msg%")) {
            this.warnings.put(
                    "config.message.format.CHAT",
                    "placeholder %msg% not found in the specified format");
        }

        if (!config.messages.formats.get(MessageType.COMMAND_SAY).format.contains("%msg%")) {
            this.warnings.put(
                    "config.message.format.COMMAND_SAY",
                    "placeholder %msg% not found in the specified format");
        }

        if (config.linkResults.linkSuccess.isBlank()) {
            this.errors.put("linkResults.linkSuccess", "field is empty");
        }
        if (config.linkResults.linkNotAllowed.isBlank()) {
            this.errors.put("linkResults.linkNotAllowed", "field is empty");
        }
        if (config.linkResults.failedUnknownCode.isBlank()) {
            this.errors.put("linkResults.failedUnknownCode", "field is empty");
        }
        if (config.linkResults.failedTooManyLinked.isBlank()) {
            this.errors.put("linkResults.failedTooManyLinked", "field is empty");
        }

        // floodgate
        if (!config.integrations.floodgate.allowLinkingMixedAccountTypes
                && config.integrations.floodgate.linkingMixedAccountTypesDenyMessage.isBlank()) {
            this.errors.put(
                    "integrations.floodgate.linkingMixedAccountTypesDenyMessage",
                    "deny message is empty");
        }

        if (!config.integrations.floodgate.allowJoiningMixedAccountTypes
                && config.integrations.floodgate.joiningMixedAccountTypesKickMessage.isBlank()) {
            this.errors.put(
                    "integrations.floodgate.joiningMixedAccountTypesKickMessage",
                    "deny message is empty");
        }

        // commands
        for (Config.LogRedirectChannel channel : config.commands.logRedirectChannels) {
            if (!isValidSnowflake(channel.channel)) {
                this.warnings.put("commands.logRedirectChannels", "invalid channel id found");
            }
        }

        for (Config.BridgeCommand command : config.commands.commandList) {
            if (command.commandName.isBlank() || command.inGameAction.isBlank()) {
                this.errors.put("commands.commandList", "empty field found");
            }
        }
    }

    public void sendMessage(Logger logger) {

        if (!this.errors.isEmpty()) {
            logger.error("Config Errors found:");
        }

        for (Entry<String, String> issue : this.errors.entrySet()) {
            logger.error("\t- '{}': {}", issue.getKey(), issue.getValue());
        }

        if (!this.warnings.isEmpty()) {
            logger.warn("Config Warnings:");
        }

        for (Entry<String, String> issue : this.warnings.entrySet()) {
            logger.warn("\t- '{}': {}", issue.getKey(), issue.getValue());
        }
    }

    private boolean isValidSnowflake(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }

        // not valid but used as example, do not give warning
        if (id.equals("1234567890") || id.equals("0123456789")) {
            return true;
        }

        try {
            if (id.startsWith("-")) {
                Long.parseLong(id);
            } else {
                Long.parseUnsignedLong(id);
            }
        } catch (NumberFormatException ignore) {
            return false;
        }
        return true;
    }
}
