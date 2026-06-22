package de.tronka.justsync.chat;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import de.tronka.justsync.JustSyncApplication;
import de.tronka.justsync.Utils;
import de.tronka.justsync.config.Config;
import de.tronka.justsync.events.ChatEvents;
import de.tronka.justsync.events.CoreEvents;
import de.tronka.justsync.events.payload.AdvancementPayload;
import de.tronka.justsync.events.payload.CommandPayload;
import de.tronka.justsync.events.payload.DeathPayload;
import de.tronka.justsync.events.payload.MessageType;
import de.tronka.justsync.events.payload.MinecraftChatMessagePayload;
import de.tronka.justsync.events.payload.MinecraftToDiscordMessagePayload;

import java.util.HashMap;
import java.util.Map;


public class MinecraftToDiscordPreprocessor {
    private final JustSyncApplication integration;
    private Config config;

    public MinecraftToDiscordPreprocessor(JustSyncApplication integration) {
        this.integration = integration;
        this.config = integration.getConfig();
        integration.registerConfigReloadHandler(this::onConfigLoaded);

        CoreEvents.SERVER_STOPPING.subscribe(this::onServerStopping);

        CoreEvents.MINECRAFT_CHAT_MESSAGE.subscribe(this::onChatMessage);
        CoreEvents.ADVANCEMENT_GRANTED.subscribe(this::onAdvancement);
        CoreEvents.PLAYER_DEATH.subscribe(this::onPlayerDeath);
        CoreEvents.PLAYER_JOIN.subscribe(this::onPlayerJoin);
        CoreEvents.PLAYER_DISCONNECT.subscribe(this::onPlayerDisconnect);
        CoreEvents.PLAYER_TIMEOUT.subscribe(this::onPlayerTimeout);
        CoreEvents.COMMAND_EXECUTED.subscribe(this::onCommandExecute);

        this.invokeMessageEvent(Map.of(), null, MessageType.SERVER_START);
    }

    private void onConfigLoaded(Config config) {
        this.config = config;
    }

    private void onServerStopping(MinecraftServer server) {
        this.invokeMessageEvent(Map.of(), null, MessageType.SERVER_STOP);
    }

    private void onChatMessage(MinecraftChatMessagePayload payload) {
        Map<String, String> replacements = Map.of("%msg%", this.preProcessMessage(payload.message(), payload.player()));
        this.invokeMessageEvent(replacements, payload.player(), MessageType.CHAT);
    }

    private void onAdvancement(AdvancementPayload payload) {
        if (payload.advancement().display().isEmpty()) {
            return;
        }
        DisplayInfo advancement = payload.advancement().display().get();
        if (!this.config.announceAdvancements || !advancement.shouldAnnounceChat()) {
            return;
        }

        Map<String, String> replacements = Map.of(
                "%title%", advancement.getTitle().getString(),
                "%description%", advancement.getDescription().getString()
        );
        this.invokeMessageEvent(replacements, payload.player(), MessageType.ADVANCEMENT);
    }

    private void onPlayerDeath(DeathPayload payload) {
        if (!this.config.broadCastDeathMessages) {
            return;
        }
        String message = payload.damageSource().getLocalizedDeathMessage(payload.player()).getString();
        if (message.equals("death.attack.badRespawnPoint")) {
            message = "%s was killed by [Intentional Mod Design]".formatted(payload.player().getName().getString());
        }
        this.invokeMessageEvent(Map.of("%msg%", Utils.escapeUnderscores(message)), payload.player(), MessageType.DEATH);
    }

    public void onPlayerJoin(ServerPlayer player) {
        this.invokeMessageEvent(Map.of(), player, MessageType.JOIN);
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        this.invokeMessageEvent(Map.of(), player, MessageType.LEAVE);
    }

    private void onPlayerTimeout(ServerPlayer player) {
        this.invokeMessageEvent(Map.of(), player, MessageType.TIMEOUT);
    }

    public void onCommandExecute(CommandPayload payload) {
        String command = payload.command();
        CommandSourceStack source = payload.source();
        if (!command.startsWith("me") && !command.startsWith("say")) {
            return;
        }
        ServerPlayer sender;
        if (source.getEntity() instanceof ServerPlayer player) {
            sender = player;
        } else {
            sender = null;
        }
        String data = command.split(" ", 2)[1];
        String message;
        if (command.startsWith("me")) {
            message = "*" + data + "*";
        } else {
            message = data;
        }
        Map<String, String> replacements = Map.of(
                "%user%", source.getTextName(),
                "%msg%", this.preProcessMessage(message, sender)
        );
        this.invokeMessageEvent(replacements, sender, MessageType.COMMAND_SAY);
    }

    private String preProcessMessage(String message, ServerPlayer player) {
        if (this.config.waypoints.formatWaypoints && player != null) {
            message = Utils.formatXaero(message, this.config);
            message = Utils.formatVoxel(message, this.config, player);
        }

        message = Utils.escapeMentions(message);
        message = Utils.formatMentions(message, this.integration, player);
        return message;
    }

    private void invokeMessageEvent(Map<String, String> replacements, ServerPlayer player, MessageType type) {
        Map<String, String> map = new HashMap<>();
        map.put("%user%", player != null ? player.getName().getString() : "Server");
        map.putAll(replacements);
        ChatEvents.MINECRAFT_TO_DISCORD_CHAT_MESSAGE.invoke(new MinecraftToDiscordMessagePayload(map, player, type));
    }
}
