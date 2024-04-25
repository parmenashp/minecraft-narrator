package com.mitsuaky.stanleyparable.server;

import com.mitsuaky.stanleyparable.StanleyParableMod;
import com.mitsuaky.stanleyparable.server.commands.NarratorCommands;
import com.mitsuaky.stanleyparable.common.events.Event;
import com.mitsuaky.stanleyparable.common.network.PacketHandler;
import com.mitsuaky.stanleyparable.common.network.PacketEventToClient;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = StanleyParableMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = {Dist.DEDICATED_SERVER, Dist.CLIENT})
public class ServerEvents {

    private static final Logger LOGGER = LogManager.getLogger(ServerEvents.class);
    private static boolean isFishing = false;

    public static String getAsName(ItemStack itemStack) {
        return Component.translatable(itemStack.getDescriptionId()).getString();
    }

    public static String getAsName(net.minecraft.world.level.block.Block block) {
        return block.getName().getString();
    }

    public static String getAsName(Entity entity) {
        return entity.getName().getString();
    }

    public static String getPlayerName(Player player) {
        String vulgo = StanleyParableMod.playerVulgo.get(player.getUUID().toString());
        if (vulgo == null || vulgo.isEmpty()) {
            return player.getName().getString();
        }
        return vulgo;
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        LOGGER.debug("RegisterCommandsEvent triggered");
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        NarratorCommands.register(dispatcher);
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
        ServerPlayer serverPlayer = (ServerPlayer) event.getPlayer();
        String e = Event.BLOCK_BROKEN.getValue();
        String msg = String.format("Jogador \"%s\" quebrou \"%s\" com \"%s\"", player, block, tool_name);
        PacketHandler.sendToPlayer(new PacketEventToClient(e, msg), serverPlayer);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        LOGGER.debug("BlockPlaceEvent triggered");
        if (event.getEntity() == null || event.getPlacedBlock().isAir() || !(event.getEntity() instanceof Player player)) {
            LOGGER.debug("BlockPlaceEvent triggered without valid player or block state");
            return;
        }


        String block = getAsName(event.getPlacedBlock().getBlock());
        String playerName = getPlayerName(player);
        LOGGER.debug(String.format("SERVER -> CLIENT: BlockPlaceEvent: \"%s\" colocou \"%s\"", playerName, block));
        String e = Event.BLOCK_PLACED.getValue();
        String msg = String.format("Jogador \"%s\" colocou \"%s\"", playerName, block);
        ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
        PacketHandler.sendToPlayer(new PacketEventToClient(e, msg), serverPlayer);
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
        LOGGER.debug(String.format("SERVER -> CLIENT: ItemFishEvent: \"%s\" pescou um(a) \"%s\"", getPlayerName(event.getEntity()), itemName));
        String e = Event.ITEM_FISHED.getValue();
        String msg = String.format("Jogador \"%s\" pescou um(a) \"%s\"", getPlayerName(event.getEntity()), itemName);
        ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
        PacketHandler.sendToPlayer(new PacketEventToClient(e, msg), serverPlayer);
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
        LOGGER.debug(String.format("SERVER -> CLIENT: ItemPickupEvent: \"%s\" pegou \"%d\" \"%s\"", player, amount, item));
        String e = Event.ITEM_PICKUP.getValue();
        String msg = String.format("Jogador \"%s\" pegou \"%d\" \"%s\"", player, amount, item);
        ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
        PacketHandler.sendToPlayer(new PacketEventToClient(e, msg), serverPlayer);
    }

    @SubscribeEvent
    public static void onClientChat(ServerChatEvent event) {
        LOGGER.debug("ClientChatEvent triggered");
        if (event.getPlayer() == null) {
            LOGGER.debug("ClientChatEvent triggered without valid player");
            return;
        }
        String message = event.getRawText();
        String player = getPlayerName(event.getPlayer());
        LOGGER.debug(String.format("SERVER -> CLIENT: ClientChatEvent: \"%s\" escreveu no chat do jogo \"%s\"", player, message));
        String e = Event.PLAYER_CHAT.getValue();
        String msg = String.format("Jogador \"%s\" escreveu no chat do jogo \"%s\"", player, message);
        ServerPlayer serverPlayer = event.getPlayer();
        PacketHandler.sendToPlayer(new PacketEventToClient(e, msg), serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        LOGGER.debug("Player LivingDeathEvent triggered");
        // check if is a player
        if (event.getEntity() == null || !(event.getEntity() instanceof Player player)) {
            LOGGER.debug("LivingDeathEvent triggered but is not a valid player");
            return;
        }

        String deathCause = event.getSource().getLocalizedDeathMessage(event.getEntity()).getString();
        String originalPlayerName = event.getEntity().getName().getString();
        String playerName = getPlayerName(player);
        LOGGER.debug(String.format("SERVER -> CLIENT: PlayerDeathEvent: \"%s\" morreu \"%s\"", playerName, deathCause.replace(originalPlayerName, playerName)));
        String e = Event.PLAYER_DEATH.getValue();
        String msg = String.format("Jogador \"%s\" morreu \"%s\"", player, deathCause.replace(originalPlayerName, playerName));
        ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
        PacketHandler.sendToPlayer(new PacketEventToClient(e, msg), serverPlayer);
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
        LOGGER.debug(String.format("SERVER -> CLIENT: AnimalBreedEvent: %s", message));
        String e = Event.ANIMAL_BREED.getValue();
        ServerPlayer serverPlayer = (ServerPlayer) event.getCausedByPlayer();
        PacketHandler.sendToPlayer(new PacketEventToClient(e, message), serverPlayer);
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        LOGGER.debug("ItemTossEvent triggered");
        if (event.getPlayer() == null || event.getEntity() == null || !(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            LOGGER.debug("ItemTossEvent triggered without valid parameters");
            return;
        }
        String player = getPlayerName(event.getPlayer());
        String itemName = getAsName(event.getEntity().getItem());

        String e = Event.ITEM_TOSS.getValue();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            ItemEntity item = event.getEntity();
            if (item.wasOnFire) {
                LOGGER.debug(String.format("SERVER -> CLIENT: ItemTossEvent: \"%s\" queimou \"%s\"", player, itemName));
                String msg = String.format("Jogador \"%s\" queimou \"%s\"", player, itemName);
                PacketHandler.sendToPlayer(new PacketEventToClient(e, msg), serverPlayer);
            } else if (item.onGround()) {
                LOGGER.debug(String.format("SERVER -> CLIENT: ItemTossEvent: \"%s\" jogou \"%s\" no chão", player, itemName));
                String msg = String.format("Jogador \"%s\" jogou \"%s\" no chão", player, itemName);
                PacketHandler.sendToPlayer(new PacketEventToClient(e, msg), serverPlayer);
            } else if (item.isInWater()) {
                LOGGER.debug(String.format("SERVER -> CLIENT: ItemTossEvent: \"%s\" jogou \"%s\" na água", player, itemName));
                String msg = String.format("Jogador \"%s\" jogou \"%s\" na água", player, itemName);
                PacketHandler.sendToPlayer(new PacketEventToClient(e, msg), serverPlayer);
            } else if (item.isInLava()) {
                LOGGER.debug(String.format("SERVER -> CLIENT: ItemTossEvent: \"%s\" jogou \"%s\" na lava", player, itemName));
                String msg = String.format("Jogador \"%s\" jogou \"%s\" na lava", player, itemName);
                PacketHandler.sendToPlayer(new PacketEventToClient(e, msg), serverPlayer);
            }
            scheduler.shutdown();
        }, 2, TimeUnit.SECONDS);
    }

    @SubscribeEvent
    public static void onAchievement(AdvancementEvent.AdvancementEarnEvent event) {
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
        LOGGER.debug(String.format("SERVER -> CLIENT: AdvancementEvent: \"%s\" obteve a conquista \"%s\": \"%s\"", player, advancementTitle, advancementDescription));
        String e = Event.ADVANCEMENT.getValue();
        ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
        PacketHandler.sendToPlayer(new PacketEventToClient(e, String.format("Jogador \"%s\" obteve a conquista \"%s\": \"%s\"", player, advancementTitle, advancementDescription)), serverPlayer);
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
        LOGGER.debug(String.format("SERVER -> CLIENT: MobKilledEvent: \"%s\" matou \"%s\" com \"%s\"", playerName, mob, weapon_name));
        String e = Event.MOB_KILLED.getValue();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        PacketHandler.sendToPlayer(new PacketEventToClient(e, String.format("Jogador \"%s\" matou \"%s\" com \"%s\"", playerName, mob, weapon_name)), serverPlayer);
    }
}
