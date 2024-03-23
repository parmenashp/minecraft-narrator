package com.mitsuaky.stanleyparable.network;

import com.mitsuaky.stanleyparable.client.ClientEventHandler;
import com.mitsuaky.stanleyparable.client.ClientEvents;
import com.mitsuaky.stanleyparable.events.Event;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketNarrationToClient {
    private static final Logger LOGGER = LogManager.getLogger(ClientEvents.ClientForgeEvents.class);
    private final String event;
    private final String msg;

    public PacketNarrationToClient(String event, String msg) {
        this.event = event;
        this.msg = msg;
    }

    public PacketNarrationToClient(FriendlyByteBuf buf) {
        event = buf.readUtf();
        msg = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(event);
        buf.writeUtf(msg);
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            //Client-side
            LOGGER.debug("Received packet for " + event);
            Event e = Event.valueOf(event.toUpperCase());
            ClientEventHandler.handle(e, msg);
        });
    }
}
