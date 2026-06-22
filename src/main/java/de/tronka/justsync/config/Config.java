package de.tronka.justsync.config;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.comments.TomlComment;
import com.moandjiezana.toml.comments.TomlMapComment;
import com.moandjiezana.toml.TomlWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.tronka.justsync.JustSyncApplication;
import de.tronka.justsync.events.payload.MessageType;

public class Config {

    @TomlComment("Discord bot token, required to connect to Discord (get from Discord Developer Portal)")
    public String botToken = "";
    @TomlComment("Discord channel ID to sync with Minecraft chat (required - right-click channel and copy ID)")
    public String serverChatChannel = "";
    @TomlComment({
        "URL template for player profile pictures in webhook messages. Placeholders:",
        "%UUID% - player's uuid",
        "%randomUUID% - random uuid to prevent caching on some apis",
        "%textureId% - texture id instead of uuid (for use with e.g. SkinsRestorer)"
    })
    public String avatarUrl = "https://minotar.net/helm/%UUID%?randomuuid=%randomUUID%";
    @TomlComment("The texture id used when no texture id was found (default is steve)")
    public String defaultAvatarTextureId = "989bf1511ab84c7cbbf3545af36b87dd";


    @TomlComment("Combine multiple identical consecutive messages into one Discord message to reduce spam")
    public boolean stackMessages = false;
    @TomlComment("Time in seconds before sending a new message regardless if it is identical")
    public int stackMessagesTimeoutInSec = 60;

    @TomlComment("Broadcast player death messages to Discord")
    public boolean broadCastDeathMessages = true;
    @TomlComment("Send player advancement/achievement notifications to Discord")
    public boolean announceAdvancements = true;

    @TomlComment("Display online player count in the Discord bot's activity status")
    public boolean showPlayerCountStatus = true;
    @TomlComment("Show the bot as idle when no player is online")
    public boolean showAsIdle = true;

    @TomlComment({"Prevent Discord users from using Minecraft formatting codes (§ symbols)",
        "Reference: https://minecraft.wiki/w/Formatting_codes"})
    public boolean restrictFormattingCodes = false;

    @TomlComment("Character to replace '§' formatting codes with. Leave empty to remove formatting code completely.")
    public String formattingCodeReplacement = "";

    @TomlComment("Discord role IDs that can bypass formatting code restrictions")
    public List<String> formattingCodeRestrictionOverrideRoles = new ArrayList<>();


    @TomlComment("Version of the Config file, do not touch!")
    public int configVersion = 5;

    public MessageStrings messages = new MessageStrings();
    public LinkingOptions linking = new LinkingOptions();
    public DiscordLinkResults linkResults = new DiscordLinkResults();
    public ErrorStrings kickMessages = new ErrorStrings();
    public CommandSettings commands = new CommandSettings();
    public ExternalIntegrations integrations = new ExternalIntegrations();
    public WaypointIntegration waypoints = new WaypointIntegration();


    public static Config loadConfig() {
        Path configDir = JustSyncApplication.getInstance().getConfigFolder();
        File configFile = configDir.resolve(JustSyncApplication.MOD_ID + ".toml").toFile();
        Config instance;
        if (configFile.exists()) {
            Toml toml = new Toml().read(configFile);
            instance = toml.to(Config.class);
            ConfigUpgrade.upgradeConfig(instance, toml);
        } else {
            instance = new Config();
        }

        try {
            Files.createDirectories(configDir);
            new TomlWriter().write(instance, configFile);
        } catch (IOException ignored) {
        }
        return instance;
    }

    public static class LinkingOptions {

        @TomlComment("Require players to link their Minecraft account to a Discord account before joining")
        public boolean enableLinking = true;
        @TomlComment("Automatically unlink players when they leave the Discord server")
        public boolean unlinkOnLeave = true;
        @TomlComment("Log account linking and unlinking events to a Discord channel")
        public boolean logLinking = false;
        @TomlComment("Discord channel ID for logging linking events (only used if logLinking is true)")
        public String linkingLogChannel = "";
        @TomlComment("Discord roles that players must have to join the Minecraft server (list of role IDs)")
        public List<String> requiredRoles = new ArrayList<>();
        @TomlComment({"Minimum number of roles from requiredRoles list that players must have",
            "-1 means all roles are required, 1 means at least one role is required"})
        public int requiredRolesCount = -1;
        @TomlComment("Discord roles to automatically assign when players join Minecraft")
        public List<String> joinRoles = new ArrayList<>();
        @TomlComment("Automatically set Discord nickname to match Minecraft username when players join")
        public boolean renameOnJoin = true;
        @TomlComment("Prevent Discord users with active timeouts from joining Minecraft")
        public boolean disallowTimeoutMembersToJoin = true;
        @TomlComment("How long (in minutes) a linking code remains valid before expiring")
        public long linkCodeExpireMinutes = 10;
        @TomlComment({"Maximum alternate accounts that can be linked to one Discord account",
            "Example: maxAlts=1 allows 1 main account + 1 alt account per Discord user"})
        public int maxAlts = 1;
        @TomlComment("Per-role overrides for maximum alt accounts (Discord role ID -> max alts)")
        public Map<String, Integer> maxAltsForRoles = Map.of("1234567890", 2);
    }

    public static class ErrorStrings {

        @TomlComment("Kick message for missing roles on discord, related to linking.requiredRoles")
        public String kickMissingRoles = "You currently don't have the permission to join the server.";
        @TomlComment("Kick message telling a player to link their account on discord")
        public String kickLinkCode = "Please Link your discord account by using\n/link %s\non discord";
        @TomlComment("Kick message to use when a player is unlinked while online")
        public String kickUnlinked = "Your account has been unlinked, to rejoin the server please relink your account.\nIf you don't know why this happened, please ask an administrator";
        @TomlComment("Kick message to use when a player has a timeout on discord, related to linking.disallowTimeoutMembersToJoin")
        public String kickTimedOut = "You are timed out and currently can't join this server. Please retry when your discord timeout is over.";
        @TomlComment("Kick message to use when the player receives a timeout on discord while being online, related to linking.disallowTimeoutMembersToJoin")
        public String kickOnTimeOut = "You have been timed out on discord. You can rejoin after it is over.";
        @TomlComment("Kick message to use when a player left the discord server while being online in minecraft")
        public String kickOnLeave = "Your associated discord account has left the discord server.";
    }

    public static class MessageStrings {

        @TomlComment({"How a normal discord chat message sent in the serverChatChannel should be displayed ingame",
            "Use https://placeholders.pb4.eu/user/text-format/ for more information on formatting",
            "Placeholders: ",
            "%user%: User who sent the message",
            "%msg%: the message",
            "%attachments%: optional attachments such as images and files"})
        public String chatMessageFormat = "[<blue>Discord</blue>] <%user%> %msg% %attachments%";
        @TomlComment({"How a reply to a message sent in the serverChatChannel should be displayed ingame",
            "Use https://placeholders.pb4.eu/user/text-format/ for more information on formatting",
            "Placeholders: ",
            "%user%: User who sent the message",
            "%msg%: The message",
            "%userRepliedTo%: The user whose message was replied to",
            "%attachments%: Optional attachments such as images and files"})
        public String chatMessageFormatReply = " [<blue>Discord</blue>] <%user% replied to %userRepliedTo%> %msg% %attachments%";
        @TomlComment({"The formatting to use for links/urls",
            "Placeholder: %link%: The url"})
        public String linkFormat = "<blue><underline><i><url:'%link%'>%link%</url></i></underline></blue>";
        @TomlComment({"The formatting to use for attachments",
            "Placeholders",
            "%link%: The url of the attached file",
            "%name%: THe name of the attachment"})
        public String attachmentFormat = "[<blue><url:'%link%'>%name%</url></blue>]";
        @TomlComment({"The formatting to use for commands sent to the console channel",
            "Placeholders:",
            "%user%: The user who executed the command",
            "%cmd%: The command the user executed"})
        public String commandExecutedInfoText = "%user% executed ``%cmd%``";
        @TomlComment({
            "Formatting to use for the online player count status if there is more than 1 player online, related to showPlayerCountStatus",
            "Placeholder: %d: Player count"})
        public String onlineCountPlural = "%d players online";
        @TomlComment("Formatting to use for the online player count status if there is 1 player online, related to showPlayerCountStatus")
        public String onlineCountSingular = "1 player online";
        @TomlComment("Formatting to use for the online player count status if there is no player online, related to showPlayerCountStatus")
        public String onlineCountZero = "Server is lonely :(";

        @TomlComment({"Format for shared waypoints",
                        "Placeholders:",
                        "%abbr%: abbreviation of waypoint (voxel defaults to first letter)",
                        "%name%: name of waypoint",
                        "%x%, %y%, %z%: coordinates",
                        "%dimension%: dimension"})
        public String waypointFormat = "Waypoint: (%abbr%)  %name% `%x% %y% %z%` in %dimension%";

        @TomlComment({"Message formats for different message types sent to Discord",
                "Every message type supports the following sending modes:",
                "- DEFAULT: sends a normal message to the channel",
                "- WEBHOOK: sends the message using a webhook with custom username and avatar",
                "- EMBED: sends the message as an embed, with custom title, avatar and color",
                "- DISABLED: does not send the message to Discord"})
        @TomlMapComment(key = "CHAT", value = {"Format for Minecraft chat messages going to Discord",
                "Placeholders:",
                "- %msg%: message",
                "- %user%: the name of the player who sent the message"})
        @TomlMapComment(key = "SERVER_START", value = "Format for the server start message")
        @TomlMapComment(key = "SERVER_STOP", value = "Format for the server stop message")
        @TomlMapComment(key = "JOIN", value = {"Format for player join messages",
                "Placeholder: %user%: the name of the player who joined"})
        @TomlMapComment(key = "LEAVE", value = {"Format for player leave messages",
                "Placeholder: %user%: the name of the player who left"})
        @TomlMapComment(key = "TIMEOUT", value = {"Format for player timeout messages",
                "Placeholders: %user%: the name of the player who timed out"})
        @TomlMapComment(key = "DEATH", value = {"Format for player death messages",
                "Placeholders:",
                "- %msg%: death message",
                "- %user%: the name of the player who died"})
        @TomlMapComment(key = "ADVANCEMENT", value = {"Format for player advancement messages",
                "Placeholders:",
                "- %title%: advancement title",
                "- %description%: advancement description",
                "- %user%: the name of the player who got the advancement",})
        @TomlMapComment(key = "COMMAND_SAY", value = {"Format for player or console executed /say and /me commands",
                "Placeholders:",
                "- %msg%: the message submitted with the command",
                "- %user%: player name or 'Server' for console"})
        public EnumMap<MessageType, MessageFormat> formats = new EnumMap<>(DEFAULT_FORMATS);

        public static final EnumMap<MessageType, MessageFormat> DEFAULT_FORMATS = new EnumMap<>(Map.of(
            MessageType.CHAT, new MessageFormat(MessageFormat.SendType.WEBHOOK, "%msg%", null, "%user%"),
            MessageType.COMMAND_SAY, new MessageFormat(MessageFormat.SendType.DEFAULT, "%user%: %msg%", null, null),
            MessageType.JOIN, new MessageFormat(MessageFormat.SendType.DEFAULT, "%user% joined", null, null),
            MessageType.LEAVE, new MessageFormat(MessageFormat.SendType.DEFAULT, "%user% left", null, null),
            MessageType.TIMEOUT, new MessageFormat(MessageFormat.SendType.DEFAULT, "%user% timed out", null, null),
            MessageType.DEATH, new MessageFormat(MessageFormat.SendType.DEFAULT, "%msg%", null, null),
            MessageType.ADVANCEMENT, new MessageFormat(MessageFormat.SendType.EMBED, "*%description%*", null, "%user% made the advancement %title%"),
            MessageType.SERVER_START, new MessageFormat(MessageFormat.SendType.DEFAULT, "Server started", null, null),
            MessageType.SERVER_STOP, new MessageFormat(MessageFormat.SendType.DEFAULT, "Server stopped", null, null)
        ));
    }

    public static class DiscordLinkResults {

        @TomlComment("Placeholder: %name%: Player name")
        public String linkSuccess = "Successfully linked to %name%";
        @TomlComment("If the user does not have all of linking.requiredRoles to join")
        public String linkNotAllowed = "You are currently missing the required roles to link your account.";
        @TomlComment("If the provided code was invalid")
        public String failedUnknownCode = "Unknown code, did you copy it correctly?";
        @TomlComment("If the user already has reached the linking.maxAlts limit")
        public String failedTooManyLinked = "You cannot link to another account";
    }

    public static class ExternalIntegrations {

        @TomlComment("Vanish https://modrinth.com/mod/vanish")
        public boolean enableVanishIntegration = true;
        @TomlComment("Luck Perms https://modrinth.com/plugin/luckperms")
        public boolean enableLuckPermsIntegration = true;
        public LuckPermsIntegration luckPerms = new LuckPermsIntegration();
        public FloodGateIntegration floodgate = new FloodGateIntegration();

        public static class FloodGateIntegration {
            @TomlComment({"Floodgate https://modrinth.com/mod/floodgate",
                "set to false to disable java players linking bedrock as alts and vice versa"})
            public boolean allowLinkingMixedAccountTypes = true;
            @TomlComment("Discord roles to bypass above restriction if set to false")
            public List<String> allowLinkingMixedAccountTypesBypass = new ArrayList<>();
            @TomlComment("Deny message for wrong account type (response of /link command)")
            public String linkingMixedAccountTypesDenyMessage = "You are not allowed to mix account types on this server";

            @TomlComment("set false to disallow mixed account types to join at the same time")
            public boolean allowJoiningMixedAccountTypes = true;
            @TomlComment("Discord roles to bypass above restrictions if set to false")
            public List<String> allowJoiningMixedAccountTypesBypass = new ArrayList<>();
            @TomlComment("Kick message when trying to join with mixed account types")
            public String joiningMixedAccountTypesKickMessage = "You are not allowed to join with mixed account types at the same time";
        }

        public static class LuckPermsIntegration {

            @TomlComment("Groups to assign to minecraft accounts that are alts")
            public List<String> altGroups = new ArrayList<>();
            @TomlComment("If a discord member has the specified role they will be given the specified groups")
            public Map<String, List<String>> syncedRoles = Map.of("0123456789", List.of("some.group"));
            @TomlComment("Assign synced roles to alternate accounts")
            public boolean assignSyncedRolesToAlts = true;
        }
    }

    public static class CommandSettings {
        @TomlComment("Discord channel ID where executed commands will be logged")
        public String commandLogChannel = "";
        @TomlComment("Discord channel ID where commands can be executed from")
        public String commandChannel = "";
        @TomlComment("Log all server commands to the command log channel")
        public boolean logCommandsInConsole = true;
        @TomlComment("Include command block executions in command logging")
        public boolean logCommandBlockCommands = false;
        @TomlComment("Commands to exclude from logging (list of command names without slashes)")
        public List<String> ignoredCommands = new ArrayList<>();
        @TomlComment("Text prefix required before commands in Discord (e.g. '//kick player')")
        public String commandPrefix = "//";
        @TomlComment("Discord role ID required to execute commands from Discord")
        public String opRole = "";
        @TomlComment("Redirect specific command outputs to different channels instead of main command log")
        public List<LogRedirectChannel> logRedirectChannels = List.of(
            LogRedirectChannel.of("", List.of("w ", "msg ", "tell ")));
        @TomlComment("Custom Discord commands that execute Minecraft commands when used with the prefix")
        public List<BridgeCommand> commandList = List.of(
            BridgeCommand.of("kick", "kick %args%"),
            BridgeCommand.of("ban", "ban %args%")
        );
    }

    public static class LogRedirectChannel {

        public String channel;
        @TomlComment("Command prefixes to redirect to this channel." +
            "IMPORTANT: add a space behind the command name to block other commands starting with the same String from being selected e.g: \"w \" to not select whitelist too")
        public List<String> redirectPrefixes = new ArrayList<>();

        public static LogRedirectChannel of(String channel, List<String> prefixes) {
            LogRedirectChannel obj = new LogRedirectChannel();
            obj.channel = channel;
            obj.redirectPrefixes = prefixes;
            return obj;
        }
    }

    public static class BridgeCommand {

        @TomlComment("Name of the command on discord")
        public String commandName = "";
        @TomlComment("Command to run ingame, use Placeholder %args% for args")
        public String inGameAction = "";

        public static BridgeCommand of(String name, String action) {
            BridgeCommand obj = new BridgeCommand();
            obj.commandName = name;
            obj.inGameAction = action;
            return obj;
        }
    }

    public static class WaypointIntegration {
        @TomlComment("Format shared waypoints")
        public boolean formatWaypoints = true;
        @TomlComment({"Base URLs of online map (this will add hyperlinks for waypoints in the specified dimensions)",
            "keep empty if none, example formatting:",
            "https://map.example.com/#world:%x%:%y%:%z%:500:0:0:0:0:perspective",
            "Placeholders: ",
            "%x%, %y%, %z%: coordinates"})
        public Map<String, String> mapURLs = new HashMap<>(Map.of("Overworld", "", "Nether", "", "End", ""));
    }

}
