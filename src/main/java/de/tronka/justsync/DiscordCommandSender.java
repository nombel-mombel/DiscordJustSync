package de.tronka.justsync;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class DiscordCommandSender extends CommandSourceStack {

    private final Consumer<String> feedbackConsumer;
    private final String sender;

    public DiscordCommandSender(MinecraftServer server, String sender, Consumer<String> feedback) {
        super(
                CommandSource.NULL,
                Vec3.ZERO,
                Vec2.ZERO,
                server.overworld(),
                //? if >= 1.21.11 {
                net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER,
                //?} else {
                /*4,
                *///?}
                "Discord-JustSync",
                Component.nullToEmpty("Discord-JustSync"),
                server,
                null);
        this.feedbackConsumer = feedback;
        this.sender = sender;
    }

    @Override
    public void sendSuccess(Supplier<Component> feedbackSupplier, boolean broadcastToOps) {
        String message = feedbackSupplier.get().getString();
        this.feedbackConsumer.accept(message);
    }

    @Override
    public void sendFailure(Component message) {
        this.feedbackConsumer.accept(message.getString());
    }

    public String getSender() {
        return this.sender;
    }
}
