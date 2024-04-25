package com.mitsuaky.stanleyparable.common.network;

import com.mitsuaky.stanleyparable.server.ServerSyncHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketSyncPlayerData {
    private static final Logger LOGGER = LogManager.getLogger(PacketEventToClient.class);
    private final String vulgo;

    public PacketSyncPlayerData(String vulgo) {
        this.vulgo = vulgo;
    }

    public PacketSyncPlayerData(FriendlyByteBuf buf) {
        vulgo = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(vulgo);
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            LOGGER.debug("Config received");
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                ServerSyncHandler.handle(player, vulgo);
            }
        });
    }
}
