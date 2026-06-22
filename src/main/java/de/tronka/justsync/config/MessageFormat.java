package de.tronka.justsync.config;

import com.moandjiezana.toml.comments.TomlComment;
import com.moandjiezana.toml.comments.TomlNullComment;

public class MessageFormat {
    @TomlComment("The way the message is sent, look at the beginning of this section for more info")
    public SendType mode;
    @TomlComment("The format of the message")
    public String format;
    @TomlComment("For webhook and embed messages only, customName sets the webhook's username or embed's title")
    @TomlNullComment("\"%user%\"")
    public String customName;
    @TomlComment("For embed messages only, embedColor sets the color of the embed in HEX format")
    @TomlNullComment("\"FF0000\"")
    public String embedColor;

    public MessageFormat(SendType type, String format, String embedColor, String customName) {
        this.mode = type;
        this.format = format;
        this.embedColor = embedColor;
        this.customName = customName;
    }

    public enum SendType {
        WEBHOOK,
        DEFAULT,
        EMBED,
        DISABLED
    }
}
