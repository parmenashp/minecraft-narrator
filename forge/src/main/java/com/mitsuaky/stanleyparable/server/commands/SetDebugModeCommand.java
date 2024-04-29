package com.mitsuaky.stanleyparable.server.commands;

import com.mitsuaky.stanleyparable.StanleyParableMod;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SetDebugModeCommand {
    private static final Logger LOGGER = LogManager.getLogger(SetDebugModeCommand.class);

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("debugMode").then(Commands.argument("boolean", BoolArgumentType.bool()).executes(SetDebugModeCommand::runCmd));
    }

    private static int runCmd(CommandContext<CommandSourceStack> ctx) {
        LOGGER.debug("Toggle debug mode command triggered");
        boolean debugMode = BoolArgumentType.getBool(ctx, "boolean");
        StanleyParableMod.debugMode = debugMode;
        ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(Component.literal("Debug mode: " + debugMode), false);
        return 1;
    }
}
