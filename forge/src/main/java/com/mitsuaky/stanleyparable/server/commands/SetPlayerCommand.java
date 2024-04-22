package com.mitsuaky.stanleyparable.server.commands;

import com.mitsuaky.stanleyparable.server.ServerConfig;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SetPlayerCommand {
    private static final Logger LOGGER = LogManager.getLogger(SetPlayerCommand.class);

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("player")
                .then(Commands.literal("set")
                        .then(
                                Commands.argument("player", EntityArgument.player()).executes(SetPlayerCommand::runCmd)
                        ));
    }

    private static int runCmd(CommandContext<CommandSourceStack> ctx) {
        LOGGER.debug("Set player command triggered");

        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ServerConfig.TARGET_PLAYER_UUID.set(player.getStringUUID());
            ctx.getSource().sendSuccess(() -> Component.literal(String.format("Player set to %s", player)), false);
            return 1;
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(Component.literal(String.format("Command failed %s", e)));
            return 0;
        }
    }
}
