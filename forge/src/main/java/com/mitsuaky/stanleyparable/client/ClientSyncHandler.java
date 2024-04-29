package com.mitsuaky.stanleyparable.client;

import com.mitsuaky.stanleyparable.StanleyParableMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientSyncHandler {
    public static void handle(boolean adventureMode) {
        StanleyParableMod.adventureMode = adventureMode;
    }
}
