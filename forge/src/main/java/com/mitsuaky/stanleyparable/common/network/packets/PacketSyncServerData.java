package com.mitsuaky.stanleyparable.common.network.packets;

import com.mitsuaky.stanleyparable.client.ClientSyncHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketSyncServerData implements Packet {
    boolean adventureMode;

    public PacketSyncServerData(boolean adventureMode) {
        this.adventureMode = adventureMode;
    }

    public PacketSyncServerData(FriendlyByteBuf buf) {
        adventureMode = buf.readBoolean();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(adventureMode);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> ClientSyncHandler.handle(adventureMode));
        ctx.setPacketHandled(true);
    }
}
