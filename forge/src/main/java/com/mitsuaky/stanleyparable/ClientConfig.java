package com.mitsuaky.stanleyparable;

import com.google.gson.JsonObject;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientConfig {
    private static final Logger LOGGER = LogManager.getLogger(ClientConfig.class);
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Integer> COOLDOWN_INDIVIDUAL;
    public static final ForgeConfigSpec.ConfigValue<Integer> COOLDOWN_GLOBAL;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SEND_TO_CHAT;

    static {
        BUILDER.push("Configs for Minecraft Narrator");
        COOLDOWN_INDIVIDUAL = BUILDER.comment("Cooldown for individual events in minutes").defineInRange("cooldown_individual", 5, 1, 20);
        COOLDOWN_GLOBAL = BUILDER.comment("Cooldown for global events in seconds").defineInRange("cooldown_global", 30, 30, 60);
        SEND_TO_CHAT = BUILDER.comment("Send events to chat").define("send_to_chat", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void applyCooldowns(Integer cooldownIndividual, Integer cooldownGlobal) {
        COOLDOWN_INDIVIDUAL.set(cooldownIndividual);
        COOLDOWN_GLOBAL.set(cooldownGlobal);
        applyCooldowns();
    }

    public static void applyCooldowns() {
        ConfigRequest request = new ConfigRequest(COOLDOWN_INDIVIDUAL.get(), COOLDOWN_GLOBAL.get());
        try {
            APICommunicator.sendRequestAsync("POST", "config", request.toJson());
        } catch (Exception ex) {
            LOGGER.error("Could not send config to server: " + ex.getMessage(), ex);
        }
    }
}

class ConfigRequest {
    private final Integer cooldownIndividual;
    private final Integer cooldownGlobal;

    ConfigRequest(Integer cooldownIndividual, Integer cooldownGlobal) {
        this.cooldownIndividual = cooldownIndividual;
        this.cooldownGlobal = cooldownGlobal;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("cooldown_individual", cooldownIndividual);
        json.addProperty("cooldown_global", cooldownGlobal);
        return json;
    }
}
