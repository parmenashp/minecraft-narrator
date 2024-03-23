package com.mitsuaky.stanleyparable;

import com.mitsuaky.stanleyparable.client.ClientConfig;
import com.mitsuaky.stanleyparable.network.Messages;
import com.mitsuaky.stanleyparable.client.screen.ConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(StanleyParableMod.MOD_ID)
public class StanleyParableMod {

    public static final String MOD_ID = "stanleyparable";

    public StanleyParableMod() {
        Messages.register();
        try {
            initClient();
        } catch (NoSuchMethodError e) {
            // Running on server, do nothing.
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void initClient() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent)));
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "minecraftnarrator-client.toml");
    }
}
