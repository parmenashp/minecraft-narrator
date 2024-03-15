package com.mitsuaky.stanleyparable.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mitsuaky.stanleyparable.*;
import com.mitsuaky.stanleyparable.screen.ConfigScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientEvents {
    @Mod.EventBusSubscriber(modid = StanleyParableMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        private static final Logger LOGGER = LogManager.getLogger(ClientForgeEvents.class);

        private static final WebSocketClient wsClient = WebSocketClient.getInstance();

        private static boolean isChestOpen = false;
        private static Set<String> lastInventory = null;
        private static boolean isRiding = false;
        private static TimeState timeState = null;
        private static boolean micActivated = false;
        private static String fullText = "";

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
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.player instanceof ServerPlayer) {
                return;
            }

            // Custom chest change event
            if (event.player.containerMenu instanceof ChestMenu) {
                if (!isChestOpen) {
                    // Store inventory on first tick when chest is open
                    lastInventory = new HashSet<>(event.player.getInventory().items.stream().map(ClientForgeEvents::getAsName).toList());
                }
                isChestOpen = true;
            } else {
                // If chest was open and is now closed, send event
                if (isChestOpen) {
                    // Get current inventory
                    Set<String> playerInventory = new HashSet<>(event.player.getInventory().items.stream().map(ClientForgeEvents::getAsName).toList());
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
        public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
            LOGGER.debug("PlayerWakeUpEvent triggered");
            if (event.getEntity() == null) {
                LOGGER.debug("PlayerWakeUpEvent triggered without valid player");
                return;
            }

            wsClient.sendEvent(Event.WAKE_UP.getValue(), String.format("Jogador \"%s\" foi dormir e acordou.", getPlayerName(event.getEntity())));
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
        public static void onPlayerJoin(EntityJoinLevelEvent event) {
            if (!(event.getEntity() instanceof LocalPlayer)) {
                return;
            }
            ClientConfig.applyServerConfig();
            Entity player = event.getEntity();
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
                String data = jsonObject.get("data").getAsString();
                JsonObject personality = JsonParser.parseString(data).getAsJsonObject();
                String voiceID = personality.get("voice_id").getAsString();
                ClientConfig.ELEVENLABS_VOICE_ID.set(voiceID);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.5F, 1.0F);
                return null;
            });

            wsClient.addEventListener("fireworks", jsonObject -> {
                ItemStack itemStack = new ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET);
                CompoundTag fireworksTag = new CompoundTag();
                fireworksTag.putFloat("Flight", 0.4F);
                fireworksTag.putInt("LifeTime", 40);
                fireworksTag.putInt("Life", 10);
                ListTag explosions = new ListTag();
                for (int i = 0; i < 3; i++) {
                    CompoundTag explosion = new CompoundTag();
                    explosion.putByte("Type", (byte) 4);
                    explosion.putBoolean("Flicker", Math.random() > 0.5);
                    explosion.putBoolean("Trail", Math.random() > 0.5);
                    explosion.putIntArray("Colors", new int[]{DyeColor.byId(i).getFireworkColor()});
                    explosions.add(explosion);
                }
                fireworksTag.put("Explosions", explosions);
                CompoundTag NBTTag = new CompoundTag();
                NBTTag.put("Fireworks", fireworksTag);
                itemStack.setTag(NBTTag);
                for (int i = 0; i < 10; i++) {
                    float angle = (i * 36) * ((float) Math.PI / 180);
                    FireworkRocketEntity firework = new FireworkRocketEntity(player.level(), player.getX() + Math.sin(angle) * 2, player.getY(), player.getZ() + Math.cos(angle) * 2, itemStack);
                    player.level().addFreshEntity(firework);
                }
                return null;
            });
            MinecraftServer server = player.getServer();
            if (server != null) {
                String worldName = server.getWorldData().getLevelName();
                String playerName = getPlayerName(player);
                wsClient.sendEvent(Event.JOIN_WORLD.getValue(), String.format("Jogador \"%s\" entrou no mundo \"%s\"", playerName, worldName));
            }
        }

        private enum TimeState {
            DAY, SUNSET, NIGHT
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
                        String playerName = getPlayerName(player);
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
                    String key = KeyBinding.VOICE_KEY.getKey().getDisplayName().getString();
                    player.displayClientMessage(Component.literal("Escutando... Aperte " + key + " novamente para parar."), true);
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = StanleyParableMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(KeyBinding.VOICE_KEY);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}
