package com.mitsuaky.stanleyparable.server.commands;

import com.mitsuaky.stanleyparable.common.events.Event;
import com.mitsuaky.stanleyparable.common.network.Messages;
import com.mitsuaky.stanleyparable.common.network.PacketEventToClient;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomPromptCommand {
    private static final Logger LOGGER = LogManager.getLogger(CustomPromptCommand.class);

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("prompt")
                .then(Commands.argument("userMessage", StringArgumentType.greedyString())
                        .executes(CustomPromptCommand::runCmd));
    }

    private static int runCmd(CommandContext<CommandSourceStack> ctx) {
        LOGGER.debug("Custom prompt command triggered");
        MinecraftServer server = ctx.getSource().getServer();
        String msg = StringArgumentType.getString(ctx, "userMessage");
        String event = Event.CUSTOM_PROMPT.getValue();
        try {
            Messages.sendToTargetPlayer(new PacketEventToClient(event, msg), server);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }
}
