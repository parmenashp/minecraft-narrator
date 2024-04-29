package com.mitsuaky.stanleyparable.common.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public interface Packet {
    void toBytes(FriendlyByteBuf buf);
    void handle(CustomPayloadEvent.Context ctx);
}
