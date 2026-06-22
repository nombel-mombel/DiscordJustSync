package de.tronka.justsync.chat.discordsender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DiscordMessageDispatcher {

    private final DiscordSenderState state;
    private final SenderStrategy strategy;

    public DiscordMessageDispatcher(SenderStrategy strategy, DiscordSenderState state) {
        this.state = state;
        this.strategy = strategy;
    }

    public void send() {
        this.state.setCount(1);
        this.state.setSentCount(1);
        this.state.setLastMessageEdit(System.currentTimeMillis());
        this.state.setFuture(this.strategy.send(this.state.getMessage()).thenAccept(this::setId));
    }

    private void setId(long messageId) {
        this.state.setMessageId(messageId);
    }

    public void incrementCountAndEdit() {
        this.state.setCount(this.state.getCount() + 1);
        if (this.state.isEditPending()) {
            return;
        }
        this.state.setEditPending(true);

        tryEditMessage();
    }

    private void tryEditMessage() {
        if (this.shouldEdit()) {
            editMessage();
            return;
        }

        CompletableFuture.runAsync(
                this::tryEditMessage,
                CompletableFuture.delayedExecutor(
                        Math.max(
                                100,
                                1000
                                        - (System.currentTimeMillis()
                                                - this.state.getLastMessageEdit())),
                        TimeUnit.MILLISECONDS));
    }

    private boolean shouldEdit() {
        return this.state.getFuture() != null
                && this.state.getFuture().isDone()
                && this.state.getLastMessageEdit() + 1000 <= System.currentTimeMillis();
    }

    private void editMessage() {
        String message = this.state.getMessage() + " *(" + this.state.getCount() + ")*";
        this.state.setSentCount(this.state.getCount());
        this.state.setLastMessageEdit(System.currentTimeMillis());
        this.state.setEditPending(false);
        this.state.setFuture(this.strategy.edit(message, this.state.getMessageId()));
    }

    public boolean hasChanged(DiscordMessageDispatcher sender, long timeout) {
        return this.state.getChannel().getLatestMessageIdLong() != this.state.getMessageId()
                || !this.state.getMessage().equals(sender.state.getMessage())
                || this.strategy.hasChanged(sender.strategy)
                || System.currentTimeMillis() - this.state.getLastMessageEdit() > timeout * 1000;
    }
}
