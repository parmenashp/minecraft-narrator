package com.mitsuaky.stanleyparable;

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

import java.util.Objects;

@Mod.EventBusSubscriber(modid = "stanleyparable", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EventSubscriber {
    private static final Logger LOGGER = LogManager.getLogger(EventSubscriber.class);

    private static final WebSocketClient wsClient = new WebSocketClient();

    public enum Event {
        ITEM_CRAFTED("item_crafted"),
        BLOCK_BROKEN("block_broken"),
        BLOCK_PLACED("block_placed"),
        PLAYER_DEATH("player_death"),
        ADVANCEMENT("advancement"),
        ITEM_PICKUP("item_pickup"),
        ITEM_SMELTED("item_smelted"),
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
        dispatcher.register(Commands.literal("minecraftnarrator").then(Commands.literal("config").executes(context -> {
            Minecraft.getInstance().setScreen(new ConfigScreen(Minecraft.getInstance().screen));
            return 1;
        })));
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        LOGGER.debug("ItemCraftedEvent triggered");
        if (event.getEntity() == null || event.getCrafting().isEmpty() || event.getEntity() instanceof ServerPlayer) {
            LOGGER.debug("ItemCraftedEvent triggered without valid entity or crafting item");
            return;
        }

        String item = getAsName(event.getCrafting().getItem(), event.getCrafting());
        String player = event.getEntity().getName().getString();
        wsClient.sendEvent(Event.ITEM_CRAFTED.getValue(), String.format("Jogador \"%s\" craftou \"%s\"", player, item));
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
        String player = event.getPlayer().getName().getString();
        wsClient.sendEvent(Event.BLOCK_BROKEN.getValue(), String.format("Jogador \"%s\" quebrou \"%s\" com \"%s\"", player, block, tool_name));
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        LOGGER.debug("BlockPlaceEvent triggered");
        if (event.getEntity() == null || event.getPlacedBlock().isAir() || !(event.getEntity() instanceof Player)) {
            LOGGER.debug("BlockPlaceEvent triggered without valid player or block state");
            return;
        }

        String block = getAsName(event.getPlacedBlock().getBlock());
        String player = event.getEntity().getName().getString();
        wsClient.sendEvent(Event.BLOCK_PLACED.getValue(), String.format("Jogador \"%s\" colocou \"%s\"", player, block));
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        event.setCanceled(false);
        LOGGER.debug("Player LivingDeathEvent triggered");
        if (event.getEntity() == null || !(event.getEntity() instanceof Player)) {
            LOGGER.debug("LivingDeathEvent triggered but is not a player");
            return;
        }

        String deathCause = event.getSource().getLocalizedDeathMessage(event.getEntity()).getString();
        String player = event.getEntity().getName().getString();
        wsClient.sendEvent(Event.PLAYER_DEATH.getValue(), String.format("Jogador \"%s\" morreu \"%s\"", player, deathCause));
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

        if (advancement.id().toString().endsWith("/root")) {
            LOGGER.debug("AdvancementEvent triggered but is a root advancement");
            return;
        }

        String advancementTitle = event.getAdvancement().value().display().map(DisplayInfo::getTitle).map(Component::getString).orElse("");
        String advancementDescription = event.getAdvancement().value().display().map(DisplayInfo::getDescription).map(Component::getString).orElse("");
        String player = event.getEntity().getName().getString();
        wsClient.sendEvent(Event.ADVANCEMENT.getValue(), String.format("Jogador \"%s\" obteve a conquista \"%s\": \"%s\"", player, advancementTitle, advancementDescription));
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        LOGGER.debug("DimensionChangeEvent triggered");
        if (event.getEntity() == null || event.getEntity() == null) {
            LOGGER.debug("DimensionChangeEvent triggered without valid player");
            return;
        }
        String dimension = event.getTo().location().toString();
        String player = event.getEntity().getName().getString();
        wsClient.sendEvent(Event.DIMENSION_CHANGED.getValue(), String.format("Jogador \"%s\" entrou na dimensão \"%s\"", player, dimension));
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        LOGGER.debug("ItemPickupEvent triggered");
        if (event.getEntity() == null || event.getStack().isEmpty() || event.getEntity() instanceof ServerPlayer) {
            LOGGER.debug("ItemPickupEvent triggered without valid player or item");
            return;
        }

        String item = getAsName(event.getStack().getItem(), event.getStack());
        int amount = event.getStack().getCount();
        String player = event.getEntity().getName().getString();
        wsClient.sendEvent(Event.ITEM_PICKUP.getValue(), String.format("Jogador \"%s\" pegou \"%d\" \"%s\"", player, amount, item));
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        LOGGER.debug("ItemSmeltedEvent triggered");
        if (event.getEntity() == null || event.getSmelting().isEmpty()) {
            LOGGER.debug("ItemSmeltedEvent triggered without valid player or item");
            return;
        }

        String item = getAsName(event.getSmelting().getItem(), event.getSmelting());
        String player = event.getEntity().getName().getString();
        wsClient.sendEvent(Event.ITEM_SMELTED.getValue(), String.format("Jogador \"%s\" fundiu/cozinhou \"%s\"", player, item));
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

        wsClient.sendEvent(Event.MOB_KILLED.getValue(), String.format("Jogador \"%s\" matou \"%s\" com \"%s\"", player.getName().getString(), mob, weapon_name));
    }

    @SubscribeEvent
    public static void onClientChat(ServerChatEvent event) {
        LOGGER.debug("ClientChatEvent triggered");
        if (event.getRawText().startsWith("/")) {
            LOGGER.debug("ClientChatEvent triggered but is a command");
            return;
        }
        String message = event.getRawText();
        String player = event.getPlayer().getName().getString();
        wsClient.sendEvent(Event.PLAYER_CHAT.getValue(), String.format("Jogador \"%s\" escreveu no chat do jogo \"%s\"", player, message));
    }

    @SubscribeEvent
    public static void onPlayerAte(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() == null || !(event.getEntity() instanceof Player) || event.getEntity() instanceof ServerPlayer) {
            LOGGER.debug("PlayerAteEvent triggered without valid player");
            return;
        }

        String item_name = getAsName(event.getItem().getItem(), event.getItem());
        String player = event.getEntity().getName().getString();
        wsClient.sendEvent(Event.PLAYER_ATE.getValue(), String.format("Jogador \"%s\" comeu/bebeu \"%s\"", player, item_name));
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ClientConfig.applyServerConfig();
        if (event.getEntity() == null || !(event.getEntity() instanceof ServerPlayer)) {
            LOGGER.debug("PlayerJoinEvent triggered without valid player");
            return;
        }
        Player player = event.getEntity();
        wsClient.addEventListener("send_chat", jsonObject -> {
            player.sendSystemMessage(Component.nullToEmpty(jsonObject.get("data").getAsString()));
            return null;
        });
        String worldName = Objects.requireNonNull(player.getServer()).getWorldData().getLevelName();
        String playerName = player.getName().getString();
        wsClient.sendEvent(Event.JOIN_WORLD.getValue(), String.format("Jogador \"%s\" entrou no mundo \"%s\"", playerName, worldName));
    }
}
