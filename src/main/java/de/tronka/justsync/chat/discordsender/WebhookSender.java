package de.tronka.justsync.chat.discordsender;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;

import java.util.concurrent.CompletableFuture;

public class WebhookSender implements SenderStrategy {

    private final String webhookName;
    private final String avatarUrl;
    private final WebhookClient webhookClient;

    public WebhookSender(String webhookName, String avatarUrl, WebhookClient webhookClient) {
        this.webhookName = webhookName;
        this.avatarUrl = avatarUrl;
        this.webhookClient = webhookClient;
    }

    @Override
    public CompletableFuture<Long> send(String message) {
        WebhookMessage msg =
                new WebhookMessageBuilder()
                        .setUsername(this.webhookName)
                        .setAvatarUrl(this.avatarUrl)
                        .setContent(message)
                        .build();

        return this.webhookClient.send(msg).thenApply(ReadonlyMessage::getId);
    }

    @Override
    public CompletableFuture<Void> edit(String message, Long messageId) {
        return this.webhookClient.edit(messageId, message).thenAccept(m -> {});
    }

    @Override
    public boolean hasChanged(SenderStrategy strategy) {
        return !this.equals(strategy);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((webhookName == null) ? 0 : webhookName.hashCode());
        result = prime * result + ((avatarUrl == null) ? 0 : avatarUrl.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        WebhookSender other = (WebhookSender) obj;
        if (webhookName == null) {
            if (other.webhookName != null) {
                return false;
            }
        } else if (!webhookName.equals(other.webhookName)) {
            return false;
        }
        if (avatarUrl == null) {
            return other.avatarUrl == null;
        }
        return avatarUrl.equals(other.avatarUrl);
    }
}
