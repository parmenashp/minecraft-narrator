package com.mitsuaky.stanleyparable.server;

import com.mitsuaky.stanleyparable.server.player.PlayerData;
import net.minecraft.server.level.ServerPlayer;

public class ServerSyncHandler {
    public static void handle(ServerPlayer player, String vulgo) {
        PlayerData.get(player).updateFromPacket(vulgo);
    }
}
