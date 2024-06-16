package com.mitsuaky.stanleyparable.server.commands;


import com.mitsuaky.stanleyparable.StanleyParableMod;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class InteractionCheckCommand {
    public static Map<String, Boolean> interactionMap = new HashMap<>();

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("interaction")
                .then(Commands.argument("interactionType", StringArgumentType.greedyString())
                        .executes(InteractionCheckCommand::runCmd));
    }

    private static int runCmd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String interactionType = StringArgumentType.getString(ctx, "interactionType");

        if (interactionMap.get(interactionType)) {
            if (StanleyParableMod.debugMode) {
                ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(Component.literal("Interaction " + interactionType + " detected"), false);
            }
            ctx.getSource().sendSuccess(() -> Component.literal("Interaction detected"), false);
            interactionMap.put(interactionType, false);
            return 1;
        } else {
            throw new SimpleCommandExceptionType(Component.translatable("commands.execute.conditional.fail")).create();
        }
    }
}