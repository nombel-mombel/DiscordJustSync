package de.tronka.justsync.events.payload;

import net.minecraft.commands.CommandSourceStack;

public record CommandPayload(CommandSourceStack source, String command) {
}
