package de.tronka.justsync.chat.discordsender;

import java.util.concurrent.CompletableFuture;

public interface SenderStrategy {

    CompletableFuture<Long> send(String message);

    CompletableFuture<Void> edit(String message, Long messageId);

    boolean hasChanged(SenderStrategy strategy);
}
