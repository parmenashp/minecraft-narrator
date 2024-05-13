package com.mitsuaky.stanleyparable.common.network.packets;

import com.mitsuaky.stanleyparable.client.ClientEventHandler;
import com.mitsuaky.stanleyparable.common.events.GameEventType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketGameEventToClient implements Packet {
    private static final Logger LOGGER = LogManager.getLogger(PacketGameEventToClient.class);
    private final String event;
    private final String msg;

    public PacketGameEventToClient(String event, String msg) {
        this.event = event;
        this.msg = msg;
    }

    public PacketGameEventToClient(FriendlyByteBuf buf) {
        event = buf.readUtf();
        msg = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(event);
        buf.writeUtf(msg);
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            LOGGER.debug("Received packet for {}", event);
            GameEventType e = GameEventType.valueOf(event.toUpperCase());
            ClientEventHandler.handle(e, msg);
        });
        ctx.setPacketHandled(true);
    }
}
