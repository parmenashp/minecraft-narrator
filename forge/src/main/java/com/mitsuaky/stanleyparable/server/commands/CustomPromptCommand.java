package com.mitsuaky.stanleyparable.server.commands;

import com.mitsuaky.stanleyparable.events.Event;
import com.mitsuaky.stanleyparable.network.Messages;
import com.mitsuaky.stanleyparable.network.PacketNarrationToClient;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomPromptCommand {
    private static final Logger LOGGER = LogManager.getLogger(CustomPromptCommand.class);

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("custom")
                .then(Commands.argument("userMessage", StringArgumentType.greedyString())
                        .executes(CustomPromptCommand::runCmd));
    }

    private static int runCmd(CommandContext<CommandSourceStack> ctx) {
        LOGGER.debug("Custom prompt command triggered");
        ServerPlayer player = ctx.getSource().getPlayer();
        String msg = StringArgumentType.getString(ctx, "userMessage");
        String event = Event.CUSTOM_PROMPT.getValue();
        Messages.sendToPlayer(new PacketNarrationToClient(event, msg), player);
        return 1;
    }
}
