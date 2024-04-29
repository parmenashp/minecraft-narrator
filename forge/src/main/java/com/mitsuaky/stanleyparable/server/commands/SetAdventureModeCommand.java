package com.mitsuaky.stanleyparable.server.commands;

import com.mitsuaky.stanleyparable.common.network.PacketHandler;
import com.mitsuaky.stanleyparable.common.network.packets.PacketSyncServerData;
import com.mitsuaky.stanleyparable.server.WorldSavedData;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class SetAdventureModeCommand {
    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("adventureMode").then(Commands.argument("boolean", BoolArgumentType.bool()).executes(SetAdventureModeCommand::runCmd));
    }

    private static int runCmd(CommandContext<CommandSourceStack> ctx) {
        boolean adventureMode = BoolArgumentType.getBool(ctx, "boolean");
        ServerLevel level = ctx.getSource().getServer().overworld();
        WorldSavedData data = WorldSavedData.get(level);
        data.setAdventureMode(adventureMode);
        ServerPlayer player = ctx.getSource().getPlayer();
        PacketHandler.sendToPlayer(new PacketSyncServerData(data.isAdventureMode()), player);
        ctx.getSource().sendSystemMessage(Component.literal("Adventure mode: " + adventureMode));
        return 1;
    }
}
