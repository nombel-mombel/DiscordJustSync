package de.tronka.justsync.events;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import de.tronka.justsync.events.payload.AdvancementPayload;
import de.tronka.justsync.events.payload.CommandPayload;
import de.tronka.justsync.events.payload.DeathPayload;
import de.tronka.justsync.events.payload.JoiningPayload;
import de.tronka.justsync.events.payload.MinecraftChatMessagePayload;

public class CoreEvents {
    public static final Event<MinecraftChatMessagePayload> MINECRAFT_CHAT_MESSAGE = new Event<>();
    public static final Event<AdvancementPayload> ADVANCEMENT_GRANTED = new Event<>();
    public static final Event<DeathPayload> PLAYER_DEATH = new Event<>();
    public static final Event<JoiningPayload> PLAYER_JOINING = new Event<>();
    public static final Event<ServerPlayer> PLAYER_JOIN = new Event<>();
    public static final Event<ServerPlayer> PLAYER_DISCONNECT = new Event<>();
    public static final Event<ServerPlayer> PLAYER_TIMEOUT = new Event<>();
    public static final Event<CommandPayload> COMMAND_EXECUTED = new Event<>();

    // loader specific
    public static final Event<MinecraftServer> SERVER_STARTING = new Event<>();
    public static final Event<MinecraftServer> SERVER_STOPPING = new Event<>();
    public static final Event<MinecraftServer> SERVER_STOPPED = new Event<>();
    public static final Event<CommandDispatcher<CommandSourceStack>> REGISTER_COMMAND = new Event<>();
}
