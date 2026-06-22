package de.tronka.justsync.chat;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.minecraft.server.level.ServerPlayer;
import de.tronka.justsync.Utils;
import de.tronka.justsync.config.Config;

public class DiscordChatMessageSender {

    private final String message;
    private final ServerPlayer sender;
    private final JDAWebhookClient webhookClient;
    private final TextChannel channel;
    private final Config config;
    private long messageId;
    private long lastMessageEdit;
    private int repetitionCount;
    private int lastUpdatedCount;
    private CompletableFuture<Void> readyFuture;

    public DiscordChatMessageSender(JDAWebhookClient webhookClient, TextChannel channel, Config config, String message,
        ServerPlayer sender) {
        this.webhookClient = webhookClient;
        this.channel = channel;
        this.config = config;
        this.message = message;
        this.sender = sender;
        this.repetitionCount = 0;
    }

    public boolean hasChanged(String message, ServerPlayer sender) {
        return this.sender != sender || !message.equals(this.message);
    }


    public void sendMessage() {
        this.repetitionCount += 1;
        if (this.config.stackMessages && this.repetitionCount >= 2 && this.canUpdateMessage()) {
            this.updateMessage(this.messageId);
            return;
        }

        this.sendMessageToDiscord();
        this.lastMessageEdit = System.currentTimeMillis();
        this.repetitionCount = 1;
        this.lastUpdatedCount = 1;
    }

    public void onMessageDelete(long messageId) {
        if (this.messageId == messageId) {
            this.messageId = 0;
            this.repetitionCount = 0;
        }
    }

    private boolean canUpdateMessage() {
        return this.readyFuture == null || !this.readyFuture.isDone() || (
            this.channel.getLatestMessageIdLong() == this.messageId
                && this.lastMessageEdit + this.config.stackMessagesTimeoutInSec * 1000L > System.currentTimeMillis());
    }

    private void updateMessage(long messageId) {
        this.messageId = messageId;
        if (this.readyFuture != null && !this.readyFuture.isDone()) {
            return;
        }
        if (this.lastUpdatedCount == this.repetitionCount) {
            return;
        }
        if (this.lastMessageEdit + 1000 > System.currentTimeMillis()) {
            if (this.readyFuture == null || this.readyFuture.isDone()) {
                this.readyFuture = CompletableFuture.runAsync(() -> this.updateMessage(messageId),
                    CompletableFuture.delayedExecutor(System.currentTimeMillis() - this.lastMessageEdit + 1,
                        TimeUnit.MILLISECONDS));
            }
            return;
        }

        if (this.channel.getLatestMessageIdLong() != this.messageId) {
            this.repetitionCount = 0;
            this.sendMessage();
            return;
        }

        this.lastUpdatedCount = this.repetitionCount;
        this.lastMessageEdit = System.currentTimeMillis();
        this.editDiscordMessage();
    }

    private String getMessage() {
        if (this.sender != null) {
            return Utils.escapeUnderscores(this.sender.getName().tryCollapseToString()) + ": " + this.message;
        }
        return this.message;
    }

    private void sendMessageToDiscord() {
        if (this.sender != null && this.webhookClient != null) {
            this.sendAsWebhook();
            return;
        }
        this.readyFuture = this.channel.sendMessage(this.getMessage()).submit().thenApply(Message::getIdLong)
            .thenAccept(this::updateMessage).exceptionally(this::handleFailure);
    }

    private void editDiscordMessage() {
        String displayedCount = " (" + this.repetitionCount + ")";
        if (this.sender != null && this.webhookClient != null) {
            this.editAsWebhook(this.message + displayedCount);
            return;
        }
        this.channel.editMessageById(this.messageId, this.getMessage() + displayedCount).submit()
            .exceptionally(this::handleFailure);
    }

    private String getAvatarUrl(ServerPlayer player) {
        String avatarUrl = this.config
                .avatarUrl
                .replace("%UUID%", player.getUUID().toString())
                .replace("%randomUUID%", UUID.randomUUID().toString());
        if (avatarUrl.contains("%textureId%")) {
            Optional<String> textureId = Utils.getTextureId(player);
            avatarUrl =
                    avatarUrl.replace(
                            "%textureId%", textureId.orElse(this.config.defaultAvatarTextureId));
        }
        return avatarUrl;
    }

    private void sendAsWebhook() {
        String avatarUrl = this.getAvatarUrl(this.sender);
        WebhookMessage msg = new WebhookMessageBuilder().setUsername(this.sender.getName().tryCollapseToString())
            .setAvatarUrl(avatarUrl).setContent(this.message).build();
        this.readyFuture = this.webhookClient.send(msg).thenApply(ReadonlyMessage::getId)
            .thenAccept(this::updateMessage).exceptionally(this::handleFailure);

    }

    private void editAsWebhook(String message) {
        this.webhookClient.edit(this.messageId, message).exceptionally(this::handleFailure);
    }

    private <T> T handleFailure(Throwable t) {
        this.repetitionCount = 0;
        return null;
    }
}
