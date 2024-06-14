package com.mitsuaky.stanleyparable.server.commands;


import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class InteractionCheckCommand {
    private static final Logger LOGGER = LogManager.getLogger(InteractionCheckCommand.class);

    public static Map<String, Boolean> interactionMap = new HashMap<>();

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("interaction")
                .then(Commands.argument("interactionType", StringArgumentType.greedyString())
                        .executes(InteractionCheckCommand::runCmd));
    }

    private static int runCmd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        //LOGGER.debug("Interaction command triggered");
        String interactionType = StringArgumentType.getString(ctx, "interactionType");

        if (interactionMap.containsKey(interactionType) && interactionMap.get(interactionType)) {
            ctx.getSource().sendSuccess(() -> Component.literal("Interaction detected"), false);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("Interaction not detected"));
            throw new SimpleCommandExceptionType(Component.translatable("commands.execute.conditional.fail")).create();
        }

    }
}
