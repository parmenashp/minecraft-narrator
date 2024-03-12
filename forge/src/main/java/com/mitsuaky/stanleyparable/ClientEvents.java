package com.mitsuaky.stanleyparable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ClientEvents {
    @Mod.EventBusSubscriber(modid = "stanleyparable", value = Dist.CLIENT)
    public static class ClientForgeEvents {
        private static final WebSocketClient wsClient = WebSocketClient.getInstance();
        private static boolean micActivated = false;
        private static String fullText = "";

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {

            if (KeyBinding.VOICE_KEY.consumeClick()) {
                LocalPlayer player = Minecraft.getInstance().player;
                assert player != null;
                if (micActivated) {
                    micActivated = false;
                    player.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 1.0F, 0.6F);
                    player.displayClientMessage(Component.literal("Você desativou o microfone."), true);
                    if (!fullText.isEmpty()) {
                        player.sendSystemMessage(Component.literal(fullText).withStyle(ChatFormatting.GREEN));
                        String playerName = EventSubscriber.getPlayerName(player);
                        String msg = "Jogador " + playerName + " falou para você: " + fullText;
                        wsClient.sendEvent("voice_complete", msg);
                    }
                    fullText = "";
                } else {
                    micActivated = true;
                    wsClient.addEventListener("speech_data", jsonObject -> {
                        if (!micActivated) {
                            return null;
                        }
                        String data = jsonObject.get("data").getAsString();
                        JsonObject speech = JsonParser.parseString(data).getAsJsonObject();
                        String text = speech.get("text").getAsString();
                        Component msg = Component.literal(text).withStyle(ChatFormatting.YELLOW);
                        player.displayClientMessage(msg, true);
                        if (speech.get("final").getAsBoolean()) {
                            if (fullText.isEmpty()) {
                                fullText = text;
                            } else {
                                fullText += ". " + text;
                            }
                        }

                        return null;
                    });
                    player.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 1.0F, 1.0F);
                    wsClient.sendEvent("voice_activate", "");
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = "stanleyparable", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(KeyBinding.VOICE_KEY);
        }
    }
}
