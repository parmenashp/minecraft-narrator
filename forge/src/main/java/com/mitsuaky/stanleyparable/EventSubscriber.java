package com.mitsuaky.stanleyparable;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.mitsuaky.stanleyparable.screen.ConfigScreen;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Mod.EventBusSubscriber(modid = "stanleyparable", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EventSubscriber {
    private static final Logger LOGGER = LogManager.getLogger(EventSubscriber.class);

    public enum Event {
        ITEM_CRAFTED("item_crafted"),
        BLOCK_BROKEN("block_broken"),
        BLOCK_PLACED("block_placed"),
        PLAYER_DEATH("player_death"),
        ADVANCEMENT("advancement"),
        ITEM_PICKUP("item_pickup"),
        MOB_KILLED("mob_killed"),
        DIMENSION_CHANGED("dimension_changed"),
        PLAYER_CHAT("player_chat"),
        PLAYER_ATE("player_ate"),
        JOIN_WORLD("join_world");

        private final String value;

        Event(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Action {
        IGNORE("ignore"),
        CANCEL_EVENT("cancel_event"),
        SEND_CHAT("send_chat");

        private final String value;

        Action(String value) {
            this.value = value;
        }

        public static Action fromString(String text) {
            for (Action b : Action.values()) {
                if (b.value.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    public static String getAsName(Item item, ItemStack stack) {
        if (stack == null) {
            stack = new ItemStack(item);
        }
        return item.getName(stack).getString();
    }

    public static String getAsName(net.minecraft.world.level.block.Block block) {
        return block.getName().getString();
    }

    public static String getAsName(Entity entity) {
        return entity.getName().getString();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("minecraftnarrator")
                .then(Commands.literal("config")
                        .executes(context -> {
                            Minecraft.getInstance().setScreen(new ConfigScreen(Minecraft.getInstance().screen));
                            return 1;
                        })
                )
        );
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ClientConfig.applyServerConfig();
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        LOGGER.debug("ItemCraftedEvent triggered");
        if (event.getEntity() == null || event.getCrafting().isEmpty() || event.getEntity() instanceof ServerPlayer) {
            LOGGER.debug("ItemCraftedEvent triggered without valid entity or crafting item");
            return;
        }

        String item = getAsName(event.getCrafting().getItem(), event.getCrafting());
        int amount = event.getCrafting().getCount();
        Player player = event.getEntity();
        ItemCraftedEventData eventData = new ItemCraftedEventData(item, amount);
        IncomingEvent<ItemCraftedEventData> incomingEvent = new IncomingEvent<>(Event.ITEM_CRAFTED, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        LOGGER.debug("BlockBreakEvent triggered");
        if (event.getPlayer() == null || event.getState().isAir()) {
            LOGGER.debug("BlockBreakEvent triggered without valid player or block state");
            return;
        }
        Item tool = event.getPlayer().getMainHandItem().getItem();
        String tool_name = getAsName(event.getPlayer().getMainHandItem().getItem(), event.getPlayer().getMainHandItem());
        if (tool.getDescriptionId().equals("block.minecraft.air")) {
            tool_name = Component.translatable("item.stanleyparable.bare_hands").getString();
        }
        String block = getAsName(event.getState().getBlock());
        Player player = event.getPlayer();
        BlockBrokenEventData eventData = new BlockBrokenEventData(block, tool_name);
        IncomingEvent<BlockBrokenEventData> incomingEvent = new IncomingEvent<>(Event.BLOCK_BROKEN, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        LOGGER.debug("BlockPlaceEvent triggered");
        if (event.getEntity() == null || event.getPlacedBlock().isAir() || !(event.getEntity() instanceof Player)) {
            LOGGER.debug("BlockPlaceEvent triggered without valid player or block state");
            return;
        }

        String block = getAsName(event.getPlacedBlock().getBlock());
        Player player = event.getEntity() instanceof Player ? (Player) event.getEntity() : null;
        BlockPlacedEventData eventData = new BlockPlacedEventData(block);
        IncomingEvent<BlockPlacedEventData> incomingEvent = new IncomingEvent<>(Event.BLOCK_PLACED, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        event.setCanceled(false);
        LOGGER.debug("Player LivingDeathEvent triggered");
        if (event.getEntity() == null || !(event.getEntity() instanceof Player player)) {
            LOGGER.debug("LivingDeathEvent triggered but is not a player");
            return;
        }

        String deathCause = event.getSource().getLocalizedDeathMessage(event.getEntity()).getString();
        PlayerDeathEventData eventData = new PlayerDeathEventData(deathCause);
        IncomingEvent<PlayerDeathEventData> incomingEvent = new IncomingEvent<>(Event.PLAYER_DEATH, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onAchievement(AdvancementEarnEvent event) {
        LOGGER.debug("AdvancementEvent triggered");
        if (event.getEntity() == null) {
            LOGGER.debug("AdvancementEvent triggered without valid player");
            return;
        }

        AdvancementHolder advancement = event.getAdvancement();
        if (advancement.value().rewards().getRecipes().length > 0) {
            LOGGER.debug("AdvancementEvent triggered but is a recipe");
            return;
        }

        if (advancement.id().toString().endsWith("/root")){
            LOGGER.debug("AdvancementEvent triggered but is a root advancement");
            return;
        }

        String advancementTitle = event.getAdvancement().value().display().map(DisplayInfo::getTitle).map(Component::getString).orElse("");
        String advancementDescription = event.getAdvancement().value().display().map(DisplayInfo::getDescription).map(Component::getString).orElse("");
        AdvancementEventData eventData = new AdvancementEventData(advancementTitle + ": " + advancementDescription);
        IncomingEvent<AdvancementEventData> incomingEvent = new IncomingEvent<>(Event.ADVANCEMENT, eventData);
        processApiResponse(event.getEntity(), event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        LOGGER.debug("DimensionChangeEvent triggered");
        if (event.getEntity() == null || event.getEntity() == null) {
            LOGGER.debug("DimensionChangeEvent triggered without valid player");
            return;
        }
        String dimension = event.getTo().location().toString();
        DimensionChangeEventData eventData = new DimensionChangeEventData(dimension);
        IncomingEvent<DimensionChangeEventData> incomingEvent = new IncomingEvent<>(Event.DIMENSION_CHANGED, eventData);
        processApiResponse(event.getEntity(), event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        LOGGER.debug("ItemPickupEvent triggered");
        if (event.getEntity() == null || event.getStack().isEmpty()) {
            LOGGER.debug("ItemPickupEvent triggered without valid player or item");
            return;
        }

        String item = getAsName(event.getStack().getItem(), event.getStack());
        int amount = event.getStack().getCount();
        Player player = event.getEntity();
        ItemPickupEventData eventData = new ItemPickupEventData(item, amount);
        IncomingEvent<ItemPickupEventData> incomingEvent = new IncomingEvent<>(Event.ITEM_PICKUP, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        LOGGER.debug("MobKilledEvent triggered");
        if (event.getEntity() == null || event.getSource().getEntity() == null || !(event.getSource().getEntity() instanceof Player player)) {
            LOGGER.debug("MobKilledEvent triggered without valid player or mob");
            return;
        }

        String mob = getAsName(event.getEntity());
        Item weapon = player.getMainHandItem().getItem();
        String weapon_name = getAsName(player.getMainHandItem().getItem(), player.getMainHandItem());
        if (weapon.getDescriptionId().equals("block.minecraft.air")) {
            weapon_name = Component.translatable("item.stanleyparable.bare_hands").getString();
        }

        MobKilledEventData eventData = new MobKilledEventData(mob, weapon_name);
        IncomingEvent<MobKilledEventData> incomingEvent = new IncomingEvent<>(Event.MOB_KILLED, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onClientChat(ServerChatEvent event) {
        LOGGER.debug("ClientChatEvent triggered");
        if (event.getRawText().startsWith("/")) {
            LOGGER.debug("ClientChatEvent triggered but is a command");
            return;
        }
        String message = event.getRawText();
        ClientChatEventData eventData = new ClientChatEventData(message);
        IncomingEvent<ClientChatEventData> incomingEvent = new IncomingEvent<>(Event.PLAYER_CHAT, eventData);
        processApiResponse(event.getPlayer(), event, incomingEvent.toJson());

    }

    @SubscribeEvent
    public static void onPlayerAte(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() == null || !(event.getEntity() instanceof Player player) || event.getEntity() instanceof ServerPlayer) {
            LOGGER.debug("PlayerAteEvent triggered without valid player");
            return;
        }

        String item_name = getAsName(event.getItem().getItem(), event.getItem());

        PlayerAteEventData eventData = new PlayerAteEventData(item_name);
        IncomingEvent<PlayerAteEventData> incomingEvent = new IncomingEvent<>(Event.PLAYER_ATE, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() == null || !(event.getEntity() instanceof ServerPlayer)) {
            LOGGER.debug("PlayerJoinEvent triggered without valid player");
            return;
        }
        Player player = event.getEntity();
        String worldName = player.getServer().getWorldData().getLevelName();

        JoinWorldEventData eventData = new JoinWorldEventData(worldName);
        IncomingEvent<JoinWorldEventData> incomingEvent = new IncomingEvent<>(Event.JOIN_WORLD, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    private static void processApiResponse(Player player, net.minecraftforge.eventbus.api.Event event, JsonObject jsonEvent) {
        CompletableFuture<JsonObject> future = APICommunicator.sendRequestAsync("POST", "event", jsonEvent);
        future.whenComplete(
                (response, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof TimeoutException) {
                            LOGGER.error("Timeout sending event to API: " + throwable.getMessage(), throwable);
                        } else {
                            LOGGER.error("Error sending event to API: " + throwable.getMessage(), throwable);
                        }
                        player.sendSystemMessage(Component.translatable("chat.stanleyparable.network_error"));
                    } else {
                        if (response == null) {
                            LOGGER.error("Received null response from API");
                            return;
                        }
                        LOGGER.debug("Received response from API: " + response);
                        handleResponse(player, event, response);
                    }
                }
        );
    }

    private static void handleResponse(Player player, net.minecraftforge.eventbus.api.Event event, JsonObject response) {
        Action action = Action.fromString(response.get("action").getAsString());
        if (action == null) {
            LOGGER.error("Received invalid action from API");
            return;
        }
        switch (action) {
            case IGNORE:
                LOGGER.debug("Ignoring event: " + event.getClass().getSimpleName());
                break;
            case CANCEL_EVENT:
                LOGGER.debug("Cancelling event: " + event.getClass().getSimpleName());
                if (event.isCancelable()) {
                    event.setCanceled(true);
                }
                break;
            case SEND_CHAT:
                if (ClientConfig.SEND_TO_CHAT.get()) {
                    LOGGER.debug("Sending chat message: " + response.getAsJsonObject("data").get("text").getAsString());
                    String chatMessage = response.getAsJsonObject("data").get("text").getAsString();
                    player.sendSystemMessage(Component.literal(chatMessage));
                }
                break;
            default:
                LOGGER.warn("Unhandled action: " + action);
        }
    }
}

class BaseEventData {
    JsonObject toJson() {
        return new JsonObject();
    }
}

class ItemCraftedEventData extends BaseEventData {
    String item;
    int amount;

    ItemCraftedEventData(String item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("item", item);
        json.addProperty("amount", amount);
        return json;
    }
}

class ClientChatEventData extends BaseEventData {
    String message;

    ClientChatEventData(String message) {
        this.message = message;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("message", message);
        return json;
    }

}

class PlayerAteEventData extends BaseEventData {
    String item;

    PlayerAteEventData(String item) {
        this.item = item;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("item", item);
        return json;
    }
}

class BlockBrokenEventData extends BaseEventData {
    String block;
    String tool;

    BlockBrokenEventData(String block, String tool) {
        this.block = block;
        this.tool = tool;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("block", block);
        json.addProperty("tool", tool);
        return json;
    }
}

class BlockPlacedEventData extends BaseEventData {
    String block;

    BlockPlacedEventData(String block) {
        this.block = block;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("block", block);
        return json;
    }
}

class PlayerDeathEventData extends BaseEventData {
    String cause;

    PlayerDeathEventData(String cause) {
        this.cause = cause;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("cause", cause);
        return json;
    }
}

class AdvancementEventData extends BaseEventData {
    String advancement;

    AdvancementEventData(String advancement) {
        this.advancement = advancement;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("advancement", advancement);
        return json;
    }
}

class JoinWorldEventData extends BaseEventData {
    String world;

    JoinWorldEventData(String world) {
        this.world = world;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("world", world);
        return json;
    }
}

class DimensionChangeEventData extends BaseEventData {
    String dimension;

    DimensionChangeEventData(String dimension) {
        this.dimension = dimension;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("dimension", dimension);
        return json;
    }
}

class ItemPickupEventData extends BaseEventData {
    String item;
    int amount;

    ItemPickupEventData(String item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("item", item);
        json.addProperty("amount", amount);
        return json;
    }
}

class MobKilledEventData extends BaseEventData {
    String mob;
    String weapon;

    MobKilledEventData(String mob, String weapon) {
        this.mob = mob;
        this.weapon = weapon;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("mob", mob);
        json.addProperty("weapon", weapon);
        return json;
    }
}

class IncomingEvent<T extends BaseEventData> {
    String event;
    T data;

    IncomingEvent(EventSubscriber.Event event, T data) {
        this.event = event.getValue();
        this.data = data;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("event", event);
        json.add("data", data.toJson());
        return json;
    }
}
