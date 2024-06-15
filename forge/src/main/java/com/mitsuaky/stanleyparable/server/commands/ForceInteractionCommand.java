package com.mitsuaky.stanleyparable.server.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.mitsuaky.stanleyparable.server.commands.InteractionCheckCommand.interactionMap;

public class ForceInteractionCommand {
    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("forceinteraction")
                .then(Commands.argument("interactionType", StringArgumentType.word())
                        .executes(ForceInteractionCommand::runCmdOnOff)
                        .then(Commands.argument("interactionValue", BoolArgumentType.bool())
                                .executes(ForceInteractionCommand::runCmdForced)));
    }


    private static int runCmdForced(CommandContext<CommandSourceStack> ctx) {
        String interactionType = StringArgumentType.getString(ctx, "interactionType");
        Boolean interactionValue = BoolArgumentType.getBool(ctx, "interactionValue");
        interactionMap.put(interactionType, interactionValue);
        ctx.getSource().sendSuccess(() -> Component.literal("Interaction " + interactionType + " setado para " + interactionValue), false);
        return 1;
    }

    private static int runCmdOnOff(CommandContext<CommandSourceStack> ctx) {
        String interactionType = StringArgumentType.getString(ctx, "interactionType");
        interactionMap.put(interactionType, true);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> interactionMap.put(interactionType, false), 3, TimeUnit.SECONDS);
        ctx.getSource().sendSuccess(() -> Component.literal("Interaction " + interactionType + " ativada, daqui 3 segundos irá desativar."), false);
        return 1;
    }
}
