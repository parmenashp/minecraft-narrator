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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.mitsuaky.stanleyparable.screen.ConfigScreen;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = "stanleyparable", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EventSubscriber {
    private static final Logger LOGGER = LogManager.getLogger(EventSubscriber.class);

    private static final WebSocketClient wsClient = new WebSocketClient();

    private static boolean isChestOpen = false;
    private static Set<String> lastInventory = null;
    private static boolean isRiding = false;
    private static boolean isFishing = false;
    private static TimeState timeState = null;

    public enum Event {
        ITEM_CRAFTED("item_crafted"),
        BLOCK_BROKEN("block_broken"),
        BLOCK_PLACED("block_placed"),
        PLAYER_DEATH("player_death"),
        ADVANCEMENT("advancement"),
        ITEM_PICKUP("item_pickup"),
        CHEST_CHANGE("chest_change"),
        ITEM_SMELTED("item_smelted"),
        MOB_KILLED("mob_killed"),
        DIMENSION_CHANGED("dimension_changed"),
        TIME_CHANGED("time_changed"),
        PLAYER_CHAT("player_chat"),
        PLAYER_ATE("player_ate"),
        RIDING("riding"),
        WAKE_UP("wake_up"),
        ITEM_FISHED("item_fished"),
        ITEM_REPAIR("item_repair"),
        ANIMAL_BREED("animal_breed"),
        ITEM_TOSS("item_toss"),
        JOIN_WORLD("join_world");

        private final String value;

        Event(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static String getAsName(ItemStack itemStack) {
        return Component.translatable(itemStack.getDescriptionId()).getString();
    }

    public static String getAsName(net.minecraft.world.level.block.Block block) {
        return block.getName().getString();
    }

    public static String getAsName(Entity entity) {
        return entity.getName().getString();
    }

    public static String getPlayerName(Entity entity) {
        if (ClientConfig.AKA.get().isEmpty()) {
            return entity.getName().getString();
        } else {
            return ClientConfig.AKA.get();
        }
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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer) {
            return;
        }

        // Custom chest change event
        if (event.player.containerMenu instanceof ChestMenu) {
            if (!isChestOpen) {
                // Store inventory on first tick when chest is open
                lastInventory = new HashSet<>(event.player.getInventory().items.stream().map(EventSubscriber::getAsName).toList());
            }
            isChestOpen = true;
        } else {
            // If chest was open and is now closed, send event
            if (isChestOpen) {
                // Get current inventory
                Set<String> playerInventory = new HashSet<>(event.player.getInventory().items.stream().map(EventSubscriber::getAsName).toList());
                // Compare with last inventory to get added and removed items
                List<String> addedItems = playerInventory.stream().filter(item -> !lastInventory.contains(item)).toList();
                List<String> removedItems = lastInventory.stream().filter(item -> !playerInventory.contains(item)).toList();

                String addedItemsString = String.join(", ", addedItems).replaceAll(", (?=[^,]*$)", " e ");
                String removedItemsString = String.join(", ", removedItems).replaceAll(", (?=[^,]*$)", " e ");
                String player = getPlayerName(event.player);

                if (!addedItems.isEmpty() && !removedItems.isEmpty()) {
                    wsClient.sendEvent(Event.CHEST_CHANGE.getValue(), String.format("Jogador \"%s\" colocou \"%s\" e removeu \"%s\" de um baú", player, addedItemsString, removedItemsString));
                } else if (!addedItems.isEmpty()) {
                    wsClient.sendEvent(Event.CHEST_CHANGE.getValue(), String.format("Jogador \"%s\" removeu \"%s\" de um baú", player, addedItemsString));
                } else if (!removedItems.isEmpty()) {
                    wsClient.sendEvent(Event.CHEST_CHANGE.getValue(), String.format("Jogador \"%s\" colocou \"%s\" em um baú", player, removedItemsString));
                }
            }
            isChestOpen = false;
        }

        // Custom riding event
        if (event.player.isPassenger()) {
            if (!isRiding) {
                String player = getPlayerName(event.player);
                Entity entity = event.player.getVehicle();
                assert entity != null;
                String vehicle = getAsName(entity);
                String vehiclePositionedInBlock = entity.getBlockStateOn().getBlock().getName().getString();
                if (entity instanceof Mob) {
                    wsClient.sendEvent(Event.RIDING.getValue(), String.format("Jogador \"%s\" montou em um(a) \"%s\"", player, vehicle));
                } else {
                    wsClient.sendEvent(Event.RIDING.getValue(), String.format("Jogador \"%s\" montou em um(a) \"%s\" posicionado em um(a) \"%s\"", player, vehicle, vehiclePositionedInBlock));
                }
            }
            isRiding = true;
        } else {
            isRiding = false;
        }


        long time = event.player.level().dayTime();
        if (0 == time && timeState != TimeState.DAY) {
            wsClient.sendEvent(Event.TIME_CHANGED.getValue(), "O sol nasceu no minecraft");
            timeState = TimeState.DAY;
        } else if (12000 == time && timeState != TimeState.SUNSET) {
            wsClient.sendEvent(Event.TIME_CHANGED.getValue(), "O sol está se pondo no minecraft");
            timeState = TimeState.SUNSET;
        } else if (13000 == time && timeState != TimeState.NIGHT) {
            wsClient.sendEvent(Event.TIME_CHANGED.getValue(), "O sol se pôs no minecraft e está escuro");
            timeState = TimeState.NIGHT;
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        LOGGER.debug("ItemCraftedEvent triggered");
        if (event.getEntity() == null || event.getCrafting().isEmpty() || event.getEntity() instanceof ServerPlayer) {
            LOGGER.debug("ItemCraftedEvent triggered without valid entity or crafting item");
            return;
        }

        String item = getAsName(event.getCrafting());
        String player = getPlayerName(event.getEntity());
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
        String tool_name = getAsName(event.getPlayer().getMainHandItem());
        if (tool.getDescriptionId().equals("block.minecraft.air")) {
            tool_name = Component.translatable("item.stanleyparable.bare_hands").getString();
        }
        String block = getAsName(event.getState().getBlock());
        String player = getPlayerName(event.getPlayer());
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
        String player = getPlayerName(event.getEntity());
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
        String originalPlayerName = event.getEntity().getName().getString();
        String player = getPlayerName(event.getEntity());
        wsClient.sendEvent(Event.PLAYER_DEATH.getValue(), String.format("Jogador \"%s\" morreu \"%s\"", player, deathCause.replace(originalPlayerName, player)));
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        LOGGER.debug("PlayerWakeUpEvent triggered");
        if (event.getEntity() == null) {
            LOGGER.debug("PlayerWakeUpEvent triggered without valid player");
            return;
        }

        wsClient.sendEvent(Event.WAKE_UP.getValue(), String.format("Jogador \"%s\" foi dormir e acordou.", getPlayerName(event.getEntity())));
    }

    @SubscribeEvent
    public static void onPlayerFish(ItemFishedEvent event) {
        LOGGER.debug("PlayerFishEvent triggered");
        if (event.getEntity() == null) {
            LOGGER.debug("PlayerFishEvent triggered without valid player");
            return;
        }
        isFishing = true;
        String itemName = getAsName(event.getDrops().get(0));
        wsClient.sendEvent(Event.ITEM_FISHED.getValue(), String.format("Jogador \"%s\" pescou um(a) \"%s\"", getPlayerName(event.getEntity()), itemName));
    }

    @SubscribeEvent
    public static void onPlayerRepair(AnvilRepairEvent event) {
        LOGGER.debug("PlayerRepairEvent triggered");
        if (event.getEntity() == null || event.getEntity() instanceof ServerPlayer) {
            LOGGER.debug("PlayerRepairEvent triggered without valid player");
            return;
        }

        String player = getPlayerName(event.getEntity());
        String itemLeft = getAsName(event.getLeft());
        String itemRight = getAsName(event.getRight());
        String message = String.format("Jogador \"%s\" juntou \"%s\" e \"%s\" na bigorna", player, itemLeft, itemRight);
        wsClient.sendEvent(Event.ITEM_REPAIR.getValue(), message);
    }

    @SubscribeEvent
    public static void onAnimalBreed(BabyEntitySpawnEvent event) {
        LOGGER.debug("AnimalBreedEvent triggered");
        if (event.getCausedByPlayer() == null || event.getParentA() == null || event.getParentB() == null) {
            LOGGER.debug("AnimalBreedEvent triggered without valid parameters");
            return;
        }
        String player = getPlayerName(event.getCausedByPlayer());
        String parentA = getAsName(event.getParentA());
        String message = String.format("Jogador \"%s\" acasalou dois/duas \"%s\"", player, parentA);
        wsClient.sendEvent(Event.ANIMAL_BREED.getValue(), message);
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        LOGGER.debug("ItemTossEvent triggered");
        if (event.getPlayer() == null || event.getEntity() == null) {
            LOGGER.debug("ItemTossEvent triggered without valid parameters");
            return;
        }
        String player = getPlayerName(event.getPlayer());
        String itemName = getAsName(event.getEntity().getItem());

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            ItemEntity item = event.getEntity();
            if (item.wasOnFire) {
                wsClient.sendEvent(Event.ITEM_TOSS.getValue(), String.format("Jogador \"%s\" queimou \"%s\"", player, itemName));
            } else if (item.onGround()) {
                wsClient.sendEvent(Event.ITEM_TOSS.getValue(), String.format("Jogador \"%s\" jogou \"%s\" no chão", player, itemName));
            } else if (item.isInWater()) {
                wsClient.sendEvent(Event.ITEM_TOSS.getValue(), String.format("Jogador \"%s\" jogou \"%s\" na água", player, itemName));
            } else if (item.isInLava()) {
                wsClient.sendEvent(Event.ITEM_TOSS.getValue(), String.format("Jogador \"%s\" jogou \"%s\" na lava", player, itemName));
            }
            scheduler.shutdown();
        }, 2, TimeUnit.SECONDS);
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
        String player = getPlayerName(event.getEntity());
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
        String player = getPlayerName(event.getEntity());
        wsClient.sendEvent(Event.DIMENSION_CHANGED.getValue(), String.format("Jogador \"%s\" entrou na dimensão \"%s\"", player, dimension));
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        LOGGER.debug("ItemPickupEvent triggered");
        if (event.getEntity() == null || event.getStack().isEmpty()) {
            LOGGER.debug("ItemPickupEvent triggered without valid player or item");
            return;
        }

        String item = getAsName(event.getStack());
        int amount = event.getStack().getCount();
        String player = getPlayerName(event.getEntity());
        if (isFishing) {
            isFishing = false;
            return;
        }
        wsClient.sendEvent(Event.ITEM_PICKUP.getValue(), String.format("Jogador \"%s\" pegou \"%d\" \"%s\"", player, amount, item));
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        LOGGER.debug("ItemSmeltedEvent triggered");
        if (event.getEntity() == null || event.getSmelting().isEmpty() || event.getEntity() instanceof ServerPlayer) {
            LOGGER.debug("ItemSmeltedEvent triggered without valid player or item");
            return;
        }

        String item = getAsName(event.getSmelting());
        String player = getPlayerName(event.getEntity());
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
        String weapon_name = getAsName(player.getMainHandItem());
        if (weapon.getDescriptionId().equals("block.minecraft.air")) {
            weapon_name = Component.translatable("item.stanleyparable.bare_hands").getString();
        }
        String playerName = getPlayerName(player);
        wsClient.sendEvent(Event.MOB_KILLED.getValue(), String.format("Jogador \"%s\" matou \"%s\" com \"%s\"", playerName, mob, weapon_name));
    }

    @SubscribeEvent
    public static void onClientChat(ServerChatEvent event) {
        LOGGER.debug("ClientChatEvent triggered");
        if (event.getRawText().startsWith("/")) {
            LOGGER.debug("ClientChatEvent triggered but is a command");
            return;
        }
        String message = event.getRawText();
        String player = getPlayerName(event.getPlayer());
        wsClient.sendEvent(Event.PLAYER_CHAT.getValue(), String.format("Jogador \"%s\" escreveu no chat do jogo \"%s\"", player, message));
    }

    @SubscribeEvent
    public static void onPlayerAte(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() == null || !(event.getEntity() instanceof Player) || event.getEntity() instanceof ServerPlayer) {
            LOGGER.debug("PlayerAteEvent triggered without valid player");
            return;
        }

        String item_name = getAsName(event.getItem());
        String player = getPlayerName(event.getEntity());
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
            if (ClientConfig.SEND_TO_CHAT.get()) {
                player.sendSystemMessage(Component.nullToEmpty(jsonObject.get("data").getAsString()));
            }
            return null;
        });

        wsClient.addEventListener("new_personality", jsonObject -> {
            if (ClientConfig.SEND_TO_CHAT.get()) {
                player.sendSystemMessage(Component.nullToEmpty("Personalidade alterada!"));
            }

            JsonObject personality = jsonObject.getAsJsonObject("data");
            String voiceID = personality.get("voice_id").getAsString();
            ClientConfig.ELEVENLABS_VOICE_ID.set(voiceID);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 1.0F, 1.0F);
            return null;
        });

        String worldName = Objects.requireNonNull(player.getServer()).getWorldData().getLevelName();
        String playerName = getPlayerName(player);
        wsClient.sendEvent(Event.JOIN_WORLD.getValue(), String.format("Jogador \"%s\" entrou no mundo \"%s\"", playerName, worldName));
    }

    private enum TimeState {
        DAY, SUNSET, NIGHT
    }
}
