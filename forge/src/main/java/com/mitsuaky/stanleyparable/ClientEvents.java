package com.mitsuaky.stanleyparable;

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

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {

            if (KeyBinding.VOICE_KEY.consumeClick()) {

                LocalPlayer player = Minecraft.getInstance().player;
                assert player != null;
                if (micActivated) {
                    micActivated = false;
                    player.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 1.0F, 0.6F);
                    String s = "botão" + micActivated;
                    player.sendSystemMessage(Component.literal(s));
                    wsClient.sendEvent("voice_deactivate", "");
                } else {
                    micActivated = true;
                    player.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 1.0F, 1.0F);
                    String s = "botão" + micActivated;
                    player.sendSystemMessage(Component.literal(s));
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
