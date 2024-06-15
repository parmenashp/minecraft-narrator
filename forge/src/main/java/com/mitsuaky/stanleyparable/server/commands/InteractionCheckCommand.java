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

        if (interactionMap.containsKey(interactionType) && interactionMap.get(interactionType)) {
            if (StanleyParableMod.debugMode) {
                ctx.getSource().sendSuccess(() -> Component.literal("Interaction detected"), true);
            }
            return 1;
        } else {
            if (StanleyParableMod.debugMode) {
                ctx.getSource().sendFailure(Component.literal("Interaction not detected"));
            }
            throw new SimpleCommandExceptionType(Component.translatable("commands.execute.conditional.fail")).create();
        }
    }
}