package com.mitsuaky.stanleyparable;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import org.json.JSONObject;

@Mod.EventBusSubscriber(modid = "stanleyparable", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EventSubscriber {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        System.out.println("Item crafted event triggered");
        if (event.getEntity() == null || event.getCrafting().isEmpty()) {
            return;
        }

        Player player = event.getEntity();
        JSONObject jsonEvent = new JSONObject();
        jsonEvent.put("event", createEventJson("item.craft", event.getCrafting().getDescriptionId()));

        processApiResponse(player, jsonEvent);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        System.out.println("Block broken event triggered");
        if (event.getPlayer() == null || event.getState().isAir()) {
            return;
        }

        Player player = event.getPlayer();
        JSONObject jsonEvent = new JSONObject();
        jsonEvent.put("event", createEventJson("block.break", event.getState().getBlock().getDescriptionId()));

        processApiResponse(player, jsonEvent);
    }

    private static JSONObject createEventJson(String eventName, String data) {
        JSONObject eventJson = new JSONObject();
        eventJson.put("name", eventName);
        if (eventName.equals("item.craft")) {
            eventJson.put("item", data);
        } else if (eventName.equals("block.break")) {
            eventJson.put("block", data);
        }
        return eventJson;
    }

    private static void processApiResponse(Player player, JSONObject jsonEvent) {
        JSONObject response = APICommunicator.sendEvent(jsonEvent);
        if (response != null) {
            String action = response.optString("action");
            if ("chat".equals(action)) {
                player.sendSystemMessage(Component.literal(response.optString("text")));
            }
        }
    }
}
