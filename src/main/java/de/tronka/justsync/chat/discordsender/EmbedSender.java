package de.tronka.justsync.chat.discordsender;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.concurrent.CompletableFuture;

public class EmbedSender implements SenderStrategy {

    private final int color;
    private final String avatarUrl;
    private final String user;
    private final TextChannel channel;

    public EmbedSender(int color, String avatarUrl, String name, TextChannel channel) {
        this.color = color;
        this.avatarUrl = avatarUrl;
        this.user = name;
        this.channel = channel;
    }

    @Override
    public CompletableFuture<Long> send(String message) {
        EmbedBuilder builder =
                new EmbedBuilder()
                        .setAuthor(this.user, null, this.avatarUrl)
                        .setDescription(message);

        if (this.color != -1) {
            builder.setColor(this.color);
        }

        return this.channel.sendMessageEmbeds(builder.build()).submit().thenApply(Message::getIdLong);
    }

    @Override
    public CompletableFuture<Void> edit(String message, Long messageId) {
        EmbedBuilder builder =
                new EmbedBuilder()
                        .setAuthor(this.user, null, this.avatarUrl)
                        .setDescription(message);

        if (this.color != -1) {
            builder.setColor(this.color);
        }

        return this.channel.editMessageEmbedsById(messageId, builder.build()).submit().thenAccept(m -> {});
    }

    @Override
    public boolean hasChanged(SenderStrategy strategy) {
        return !this.equals(strategy);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + color;
        result = prime * result + ((avatarUrl == null) ? 0 : avatarUrl.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
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
        EmbedSender other = (EmbedSender) obj;
        if (color != other.color) {
            return false;
        }
        if (avatarUrl == null) {
            if (other.avatarUrl != null) {
                return false;
            }
        } else if (!avatarUrl.equals(other.avatarUrl)) {
            return false;
        }
        if (user == null) {
            return other.user == null;
        }
        return user.equals(other.user);
    }
}
