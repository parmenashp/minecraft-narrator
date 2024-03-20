package com.mitsuaky.stanleyparable.network;

import com.mitsuaky.stanleyparable.StanleyParableMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Messages {
    private static final Logger LOGGER = LogManager.getLogger(Messages.class);


    private static SimpleChannel INSTANCE;

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = ChannelBuilder
                .named(new ResourceLocation(StanleyParableMod.MOD_ID, "messages"))
                .networkProtocolVersion(1)
                .clientAcceptedVersions((p1, p2) -> true)
                .serverAcceptedVersions((p1, p2) -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(PacketNarrationToClient.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketNarrationToClient::new)
                .encoder(PacketNarrationToClient::toBytes)
                .consumerMainThread(PacketNarrationToClient::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        PacketDistributor.PacketTarget target = PacketDistributor.SERVER.noArg();
        INSTANCE.send(message, target);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        LOGGER.debug("Sending {} to {}", message, player);
        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(player);
        INSTANCE.send(message, target);
    }

}