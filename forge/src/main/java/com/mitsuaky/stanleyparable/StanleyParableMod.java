package com.mitsuaky.stanleyparable;

import com.mitsuaky.stanleyparable.client.ClientConfig;
import com.mitsuaky.stanleyparable.client.screen.ConfigScreen;
import com.mitsuaky.stanleyparable.common.network.PacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.util.HashMap;
import java.util.Map;

@Mod(StanleyParableMod.MOD_ID)
public class StanleyParableMod {

    public static final String MOD_ID = "stanleyparable";

    public static final Map<String, String> playerVulgo = new HashMap<>();

    public StanleyParableMod() {
        PacketHandler.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> StanleyParableMod::initClient);
    }

    @OnlyIn(Dist.CLIENT)
    public static void initClient() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent)));
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "minecraftnarrator-client.toml");
    }
}
