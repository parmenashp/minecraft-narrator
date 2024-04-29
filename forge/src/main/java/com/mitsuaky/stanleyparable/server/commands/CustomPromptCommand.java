package com.mitsuaky.stanleyparable.server.commands;

import com.mitsuaky.stanleyparable.StanleyParableMod;
import com.mitsuaky.stanleyparable.common.events.SystemEventType;
import com.mitsuaky.stanleyparable.common.network.PacketHandler;
import com.mitsuaky.stanleyparable.common.network.packets.PacketSystemEventToClient;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomPromptCommand {
    private static final Logger LOGGER = LogManager.getLogger(CustomPromptCommand.class);

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("prompt")
                .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("userMessage", StringArgumentType.greedyString())
                        .executes(CustomPromptCommand::runCmd)));
    }

    private static int runCmd(CommandContext<CommandSourceStack> ctx) {
        LOGGER.debug("Custom prompt command triggered");
        String msg = StringArgumentType.getString(ctx, "userMessage");
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            if (StanleyParableMod.debugMode) {
                player.sendSystemMessage(Component.literal("PROMPT -> " + msg));
            }
            String event = SystemEventType.CUSTOM_PROMPT.getValue();
            PacketHandler.sendToPlayer(new PacketSystemEventToClient(event, msg), player);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }
}
