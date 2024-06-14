package com.mitsuaky.stanleyparable.common.network.packets;

import com.mitsuaky.stanleyparable.common.network.PacketHandler;
import com.mitsuaky.stanleyparable.server.commands.InteractionCheckCommand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PacketInteractionToServer implements Packet {

    private final String interactionType;
    private final Boolean interactionValue;

    public PacketInteractionToServer(String interactionType, Boolean interactionValue) {
        this.interactionType = interactionType;
        this.interactionValue = interactionValue;
    }

    public PacketInteractionToServer(FriendlyByteBuf buf) {
        this.interactionType = buf.readUtf(32);
        this.interactionValue = buf.readBoolean();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(interactionType);
        buf.writeBoolean(interactionValue);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            InteractionCheckCommand.interactionMap.put(interactionType, interactionValue);
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.schedule(() -> InteractionCheckCommand.interactionMap.put(interactionType, false), 3, TimeUnit.SECONDS);
        });
        ctx.setPacketHandled(true);
    }
}
