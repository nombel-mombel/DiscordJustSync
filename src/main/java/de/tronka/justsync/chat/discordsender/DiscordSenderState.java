package de.tronka.justsync.chat.discordsender;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.concurrent.CompletableFuture;

public class DiscordSenderState {
    private String message;
    private TextChannel channel;
    private long messageId;
    private long lastMessageEdit;
    private int count;
    private int sentCount;
    private CompletableFuture<Void> future;
    private boolean isEditPending;

    public DiscordSenderState(String message, TextChannel channel) {
        this.message = message;
        this.channel = channel;
    }

    public TextChannel getChannel() {
        return channel;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public long getLastMessageEdit() {
        return lastMessageEdit;
    }

    public void setLastMessageEdit(long lastMessageEdit) {
        this.lastMessageEdit = lastMessageEdit;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getSentCount() {
        return sentCount;
    }

    public void setSentCount(int sentCount) {
        this.sentCount = sentCount;
    }

    public CompletableFuture<Void> getFuture() {
        return future;
    }

    public void setFuture(CompletableFuture<Void> future) {
        this.future = future;
    }

    public String getMessage() {
        return this.message;
    }

    public boolean isEditPending() {
        return isEditPending;
    }

    public void setEditPending(boolean isEditPending) {
        this.isEditPending = isEditPending;
    }
}
