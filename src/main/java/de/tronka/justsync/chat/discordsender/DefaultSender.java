package de.tronka.justsync.chat.discordsender;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.concurrent.CompletableFuture;

public class DefaultSender implements SenderStrategy {

    private final TextChannel channel;

    public DefaultSender(TextChannel channel) {
        this.channel = channel;
    }

    @Override
    public CompletableFuture<Long> send(String message) {
        return this.channel.sendMessage(message).submit().thenApply(Message::getIdLong);
    }

    @Override
    public CompletableFuture<Void> edit(String message, Long messageId) {
        return this.channel.editMessageById(messageId, message).submit().thenAccept(m -> {});
    }

    @Override
    public boolean hasChanged(SenderStrategy strategy) {
        return false;
    }
}
