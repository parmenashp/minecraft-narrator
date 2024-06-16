package com.mitsuaky.stanleyparable.common.network.packets;


import com.mitsuaky.stanleyparable.StanleyParableMod;
import com.mitsuaky.stanleyparable.common.events.GameEventType;
import com.mitsuaky.stanleyparable.common.network.PacketHandler;
import com.mitsuaky.stanleyparable.server.ServerEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.Objects;

public class PacketPlayerJoin implements Packet {
    private final String vulgo;

    public PacketPlayerJoin(String vulgo) {
        this.vulgo = vulgo;
    }

    public PacketPlayerJoin(FriendlyByteBuf buf) {
        vulgo = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(vulgo);
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            assert player != null;
            StanleyParableMod.playerVulgo.put(player.getUUID().toString(), vulgo);
            String worldName = Objects.requireNonNull(player.getServer()).getWorldData().getLevelName();
            String playerName = ServerEvents.getPlayerName(player);
            String msg = String.format("Jogador \"%s\" entrou no mundo \"%s\"", playerName, worldName);
            PacketHandler.sendToPlayer(new PacketGameEventToClient(GameEventType.JOIN_WORLD.getValue(), msg), player);
        });
        ctx.setPacketHandled(true);
    }
}
