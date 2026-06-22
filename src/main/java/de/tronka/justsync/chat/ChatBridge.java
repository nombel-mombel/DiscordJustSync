package de.tronka.justsync.chat;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import eu.pb4.placeholders.api.node.TextNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import de.tronka.justsync.JustSyncApplication;
import de.tronka.justsync.Utils;
import de.tronka.justsync.chat.discordsender.DefaultSender;
import de.tronka.justsync.chat.discordsender.DiscordMessageDispatcher;
import de.tronka.justsync.chat.discordsender.DiscordSenderState;
import de.tronka.justsync.chat.discordsender.EmbedSender;
import de.tronka.justsync.chat.discordsender.SenderStrategy;
import de.tronka.justsync.chat.discordsender.WebhookSender;
import de.tronka.justsync.config.Config;
import de.tronka.justsync.config.MessageFormat;
import de.tronka.justsync.config.MessageFormat.SendType;
import de.tronka.justsync.events.ChatEvents;
import de.tronka.justsync.events.payload.MinecraftToDiscordMessagePayload;

public class ChatBridge extends ListenerAdapter {

    private static final String WEBHOOK_ID = "justsync-hook";
    private final JustSyncApplication integration;
    private TextChannel channel;
    private DiscordMessageDispatcher messageDispatcher;
    private JDAWebhookClient webhookClient;

    public ChatBridge(JustSyncApplication integration) {
        this.integration = integration;
        ChatEvents.MINECRAFT_TO_DISCORD_CHAT_MESSAGE.subscribe(this::onMcMessage);
        integration.registerConfigReloadHandler(this::onConfigLoaded);
    }

    private void onConfigLoaded(Config config) {
        this.channel = Utils.getTextChannel(this.integration.getJda(), config.serverChatChannel, "serverChatChannel");
        this.messageDispatcher = null;
        this.setWebhook(null);
        if (this.integration.getConfig().messages.formats.values().stream()
                .anyMatch(format -> format.mode.equals(MessageFormat.SendType.WEBHOOK))) {
            this.channel.retrieveWebhooks().onSuccess((webhooks -> {
                Optional<Webhook> hook = webhooks.stream()
                    .filter(w -> w.getOwner() == this.integration.getGuild().getSelfMember()).findFirst();
                if (hook.isPresent()) {
                    this.setWebhook(hook.get());
                } else {
                    this.channel.createWebhook(WEBHOOK_ID).onSuccess(this::setWebhook).queue();
                }
            })).queue();
        }
    }
    
    public void onMcMessage(MinecraftToDiscordMessagePayload payload) {
        MessageFormat format = this.integration.getConfig().messages.formats.get(payload.type());

        SendType mode = format.mode;

        if (mode == SendType.DISABLED) {
            return;
        }

        if (mode == SendType.WEBHOOK && this.webhookClient == null) {
            mode = SendType.DEFAULT;
        }

        String message = applyReplacements(format.format, payload.replacements(), true);
        if (message == null || message.isBlank()) {
            // Discord does not accept empty messages
            return;
        }

        String customName = applyReplacements(format.customName, payload.replacements(), false);
        if ((customName == null || customName.isBlank())
                && (mode == SendType.WEBHOOK || mode == SendType.EMBED)) {
            customName = this.integration.getGuild().getSelfMember().getEffectiveName();
        }

        String avatarUrl =
                payload.player() != null
                        ? Utils.getAvatarUrl(payload.player(), this.integration.getConfig())
                        : null;

        SenderStrategy strategy =
                switch (mode) {
                    case SendType.WEBHOOK ->
                            new WebhookSender(customName, avatarUrl, this.webhookClient);
                    case SendType.EMBED ->
                            new EmbedSender(
                                    Utils.hexStringToInt(format.embedColor),
                                    avatarUrl,
                                    customName,
                                    this.channel);
                    case SendType.DEFAULT -> new DefaultSender(this.channel);
                    default -> null;
                };

        DiscordSenderState state = new DiscordSenderState(message, this.channel);
        DiscordMessageDispatcher dispatcher = new DiscordMessageDispatcher(strategy, state);

        if (this.integration.getConfig().stackMessages
                && this.messageDispatcher != null
                        && !this.messageDispatcher.hasChanged(
                                dispatcher,
                                this.integration.getConfig().stackMessagesTimeoutInSec)) {
            this.messageDispatcher.incrementCountAndEdit();
            return;
        }

        dispatcher.send();
        this.messageDispatcher = dispatcher;
    }

    private String applyReplacements(String message, Map<String, String> replacementsOriginal, boolean escapeUser) {
        if (message == null || message.isBlank()) {
            return message;
        }

        if (replacementsOriginal.isEmpty()) {
            return message;
        }

        Map<String, String> replacements = new HashMap<>(replacementsOriginal);
        replacements.remove("%msg%");
        if (escapeUser && replacements.containsKey("%user%")) {
            replacements.put("%user%", Utils.escapeUnderscores(replacements.get("%user%")));
        }

        StringBuilder builder = new StringBuilder(message);
        for (Entry<String, String> entry : replacements.entrySet()) {
            int index = builder.indexOf(entry.getKey());
            if (index == -1) {
                continue;
            }
            builder.replace(index, index + entry.getKey().length(), entry.getValue());
        }

        // replace %msg% last to prevent user injection of formatting codes
        if (replacementsOriginal.containsKey("%msg%")) {
            int index = builder.indexOf("%msg%");
            if (index != -1) {
                builder.replace(index, index + "%msg%".length(), replacementsOriginal.get("%msg%"));
            }
        }

        return builder.toString();
    }

    private void setWebhook(Webhook webhook) {
        if (this.webhookClient != null) {
            this.webhookClient.close();
            this.webhookClient = null;
        }
        if (webhook != null) {
            this.webhookClient = JDAWebhookClient.from(webhook);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // discord message
        if (event.getChannel() != this.channel) {
            return;
        }
        Member member = event.getMember();
        if (member == null || event.getAuthor().isBot()) {
            return;
        }

        Message repliedMessage = event.getMessage().getReferencedMessage();

        String baseText = repliedMessage == null ? this.integration.getConfig().messages.chatMessageFormat
            : this.integration.getConfig().messages.chatMessageFormatReply;

        TextNode attachmentInfo;
        if (!event.getMessage().getAttachments().isEmpty()) {
            List<TextNode> attachments = new ArrayList<>(List.of(TextNode.of("\nAttachments:")));
            for (Message.Attachment attachment : event.getMessage().getAttachments()) {
                attachments.add(
                    TextReplacer.create().replace("link", attachment.getUrl()).replace("name", attachment.getFileName())
                        .applyNode(this.integration.getConfig().messages.attachmentFormat));
            }
            attachmentInfo = TextNode.wrap(attachments);
        } else {
            attachmentInfo = TextNode.empty();
        }

        String messageText = event.getMessage().getContentDisplay();
        messageText = replaceFormattingCodes(messageText, member, true);

        String replyUser = "";
        if (repliedMessage != null) {
            replyUser =
                    repliedMessage.getMember() == null
                            ? repliedMessage.getAuthor().getEffectiveName()
                            : repliedMessage.getMember().getEffectiveName();
        }

        this.sendMcChatMessage(
                TextReplacer.create()
                        .replace("msg", Utils.parseUrls(messageText, this.integration.getConfig()))
                        .replace(
                                "user",
                                replaceFormattingCodes(member.getEffectiveName(), member, false))
                        .replace("userRepliedTo", replaceFormattingCodes(replyUser, member, false))
                        .replace("attachments", attachmentInfo)
                        .apply(baseText));
    }

    private String replaceFormattingCodes(String text, Member member, boolean withBypass) {
        if (text.contains("§") && this.integration.getConfig().restrictFormattingCodes) {
            if (withBypass
                    && member.getRoles().stream()
                            .anyMatch(
                                    Utils.parseRoleList(
                                                    this.integration.getGuild(),
                                                    this.integration.getConfig()
                                                            .formattingCodeRestrictionOverrideRoles)
                                            ::contains)) {
                return text;
            }

            if (this.integration.getConfig().formattingCodeReplacement.isEmpty()) {
                text = Utils.removeFormattingCode(text);
            } else {
                text =
                        Utils.replaceFormattingCode(
                                text, this.integration.getConfig().formattingCodeReplacement);
            }
        }
        return text;
    }

    public void sendMcChatMessage(Component message) {
        if (this.integration.getServer() == null) {
            return;
        }
        this.integration.getServer().getPlayerList().broadcastSystemMessage(message, false);
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        this.messageDispatcher = null;
    }
}
