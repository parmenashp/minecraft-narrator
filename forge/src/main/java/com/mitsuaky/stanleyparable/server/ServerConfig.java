package com.mitsuaky.stanleyparable.server;


import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerConfig {
    private static final Logger LOGGER = LogManager.getLogger(ServerConfig.class);

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> TARGET_PLAYER_UUID;

    static {
        BUILDER.push("Server configs for Minecraft Narrator");
        TARGET_PLAYER_UUID = BUILDER.comment("Player that will receive events to narrator").define("target_player_uuid", "");
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
