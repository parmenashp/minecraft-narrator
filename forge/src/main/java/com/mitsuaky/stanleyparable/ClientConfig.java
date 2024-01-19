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
    public static final ForgeConfigSpec.ConfigValue<Integer> NARRATOR_VOLUME;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SEND_TO_CHAT;
    public static final ForgeConfigSpec.ConfigValue<Boolean> TTS;

    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_BASE_URL;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> ELEVENLABS_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> ELEVENLABS_VOICE_ID;

    static {
        BUILDER.push("Configs for Minecraft Narrator");
        COOLDOWN_INDIVIDUAL = BUILDER.comment("Cooldown for individual events in minutes").defineInRange("cooldown_individual", 5, 1, 20);
        COOLDOWN_GLOBAL = BUILDER.comment("Cooldown for global events in seconds").defineInRange("cooldown_global", 30, 30, 60);
        NARRATOR_VOLUME = BUILDER.comment("Narrator Volume").defineInRange("narrator_volume", 100, 1, 130);
        SEND_TO_CHAT = BUILDER.comment("Send events to chat").define("send_to_chat", true);
        TTS = BUILDER.comment("Enable text to speech").define("tts", true);

        OPENAI_API_KEY = BUILDER.comment("OpenAI API Key").define("openai_api_key", "");
        OPENAI_BASE_URL = BUILDER.comment("OpenAI Base URL").define("openai_base_url", "https://api.openai.com/v1");
        OPENAI_MODEL = BUILDER.comment("OpenAI Model").define("openai_model", "gpt-4-1106-preview");
        ELEVENLABS_API_KEY = BUILDER.comment("ElevenLabs API Key").define("elevenlabs_api_key", "");
        ELEVENLABS_VOICE_ID = BUILDER.comment("ElevenLabs Voice ID/Name").define("elevenlabs_voice_id", "");

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void applyServerConfig() {
        JsonObject request = new JsonObject();
        try {
            request.addProperty("cooldown_individual", COOLDOWN_INDIVIDUAL.get());
            request.addProperty("cooldown_global", COOLDOWN_GLOBAL.get());
            request.addProperty("narrator_volume", NARRATOR_VOLUME.get());
            request.addProperty("tts", TTS.get());
            request.addProperty("openai_api_key", OPENAI_API_KEY.get());
            request.addProperty("openai_base_url", OPENAI_BASE_URL.get());
            request.addProperty("openai_model", OPENAI_MODEL.get());
            request.addProperty("elevenlabs_api_key", ELEVENLABS_API_KEY.get());
            request.addProperty("elevenlabs_voice_id", ELEVENLABS_VOICE_ID.get());
            WebSocketClient.getInstance().sendEvent("config", request.toString());
        } catch (Exception ex) {
            LOGGER.error("Could not send config to server: " + ex.getMessage(), ex);
        }
    }
}
