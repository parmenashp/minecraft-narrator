package com.mitsuaky.stanleyparable.common.network;

import com.mitsuaky.stanleyparable.StanleyParableMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketHandler {
    private static final Logger LOGGER = LogManager.getLogger(PacketHandler.class);


    private static SimpleChannel INSTANCE;

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = ChannelBuilder.named(new ResourceLocation(StanleyParableMod.MOD_ID, "messages")).networkProtocolVersion(1).clientAcceptedVersions((p1, p2) -> true).serverAcceptedVersions((p1, p2) -> true).simpleChannel();

        INSTANCE = net;

        net.messageBuilder(PacketEventToClient.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(PacketEventToClient::new).encoder(PacketEventToClient::toBytes).consumerMainThread(PacketEventToClient::handle).add();

        net.messageBuilder(PacketSyncPlayerData.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(PacketSyncPlayerData::new).encoder(PacketSyncPlayerData::toBytes).consumerMainThread(PacketSyncPlayerData::handle).add();
    }

    public static <Packet> void sendToServer(Packet message) {
        PacketDistributor.PacketTarget target = PacketDistributor.SERVER.noArg();
        INSTANCE.send(message, target);
    }

    public static <Packet> void sendToPlayer(Packet message, ServerPlayer player) {
        LOGGER.debug("Sending {} to {}", message, player);
        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(player);
        INSTANCE.send(message, target);
    }
}
