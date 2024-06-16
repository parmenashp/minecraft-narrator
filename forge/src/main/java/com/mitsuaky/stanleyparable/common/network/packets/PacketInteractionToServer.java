package com.mitsuaky.stanleyparable.common.network.packets;

import com.mitsuaky.stanleyparable.server.commands.InteractionCheckCommand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

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
        ctx.enqueueWork(() -> InteractionCheckCommand.interactionMap.put(interactionType, interactionValue));
        ctx.setPacketHandled(true);
    }
}
