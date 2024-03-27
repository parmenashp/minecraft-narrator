package com.mitsuaky.stanleyparable.common.network;

import com.mitsuaky.stanleyparable.StanleyParableMod;
import com.mitsuaky.stanleyparable.server.ServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.network.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;


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

        net.messageBuilder(PacketEventToClient.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketEventToClient::new)
                .encoder(PacketEventToClient::toBytes)
                .consumerMainThread(PacketEventToClient::handle)
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

    public static <MSG> void sendToTargetPlayer(MSG message, MinecraftServer server) throws Exception {

        PlayerList players = server.getPlayerList();
        UUID targetPlayerUUID = UUID.fromString(ServerConfig.TARGET_PLAYER_UUID.get());
        ServerPlayer targetPlayer = players.getPlayer(targetPlayerUUID);
        if (targetPlayer == null) {
            throw new Exception(String.format("Player with uuid %s not found", targetPlayerUUID));
        }

        LOGGER.debug("Sending {} to {}", message, targetPlayer);
        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(targetPlayer);
        INSTANCE.send(message, target);

    }
}