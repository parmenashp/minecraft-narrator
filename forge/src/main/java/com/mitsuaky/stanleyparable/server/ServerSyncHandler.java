package com.mitsuaky.stanleyparable.server;

import com.mitsuaky.stanleyparable.StanleyParableMod;
import net.minecraft.server.level.ServerPlayer;

public class ServerSyncHandler {
    public static void handle(ServerPlayer player, String vulgo) {
        StanleyParableMod.playerVulgo.put(player.getUUID().toString(), vulgo);
    }
}
