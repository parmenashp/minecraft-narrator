package com.mitsuaky.stanleyparable.screen;

import com.google.gson.JsonObject;
import com.mitsuaky.stanleyparable.APICommunicator;
import com.mitsuaky.stanleyparable.ClientConfig;
import com.mitsuaky.stanleyparable.screen.widget.PingWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

@OnlyIn(Dist.CLIENT)
public class ConfigScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger(ConfigScreen.class);
    private final Screen parent;
    private final int commonWidth = 200;
    private final int commonHeight = 20;
    private int coolDownIndividual = ClientConfig.COOLDOWN_INDIVIDUAL.get();
    private int coolDownGlobal = ClientConfig.COOLDOWN_GLOBAL.get();
    private boolean sendToChat = ClientConfig.SEND_TO_CHAT.get();
    private boolean tts = ClientConfig.TTS.get();
    private String ping = "Offline";

    private PingWidget pingWidget;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("gui.stanleyparable.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        long time = System.currentTimeMillis();
        APICommunicator.sendRequestAsync("GET", "ping", null).whenComplete((response, throwable) -> {
            check_connection(time, response, throwable);
            this.pingWidget.setMessage(Component.literal("Ping: " + this.ping));
        });


        int commonX = (this.width / 2) - (commonWidth / 2);
        int commonMargin = 5;
        int commonY = 30;

        this.addRenderableWidget(
                new AbstractSliderButton(
                        commonX,
                        commonY,
                        commonWidth,
                        commonHeight,
                        Component.nullToEmpty(Component.translatable("gui.stanleyparable.cooldown_event").getString() + coolDownIndividual),
                        mapToSlideDouble(coolDownIndividual, 1, 20)
                ) {
                    @Override
                    protected void updateMessage() {
                        this.setMessage(Component.nullToEmpty(Component.translatable("gui.stanleyparable.cooldown_event").getString() + coolDownIndividual));
                    }

                    @Override
                    protected void applyValue() {
                        coolDownIndividual = mapToRealInt(this.value, 1, 20);
                    }
                }
        );

        commonY += commonHeight + commonMargin;

        this.addRenderableWidget(
                new AbstractSliderButton(
                        commonX,
                        commonY,
                        commonWidth,
                        commonHeight,
                        Component.nullToEmpty(Component.translatable("gui.stanleyparable.cooldown_global").getString() + coolDownGlobal),
                        mapToSlideDouble(coolDownGlobal, 30, 60)
                ) {
                    @Override
                    protected void updateMessage() {
                        this.setMessage(Component.nullToEmpty(Component.translatable("gui.stanleyparable.cooldown_global").getString() + coolDownGlobal));
                    }

                    @Override
                    protected void applyValue() {
                        coolDownGlobal = mapToRealInt(this.value, 30, 60);
                    }
                }
        );

        commonY += commonHeight + commonMargin;

        this.addRenderableWidget(new Checkbox(commonX, commonY, commonWidth, commonHeight, Component.translatable("gui.stanleyparable.tts"), tts) {
            @Override
            public void onPress() {
                super.onPress();
                tts = this.selected();
                ClientConfig.TTS.set(tts);
            }
        });

        commonY += commonHeight + commonMargin;

        this.addRenderableWidget(new Checkbox(commonX, commonY, commonWidth, commonHeight, Component.translatable("gui.stanleyparable.send_to_chat"), sendToChat) {
            @Override
            public void onPress() {
                super.onPress();
                sendToChat = this.selected();
                ClientConfig.SEND_TO_CHAT.set(sendToChat);
            }
        });

        commonY += commonHeight + commonMargin;

        pingWidget = new PingWidget(commonX, commonY, commonWidth, commonHeight, Component.literal("Ping: " + ping)) {
            @Override
            public void onPress() {
                long time = System.currentTimeMillis();
                APICommunicator.sendRequestAsync("GET", "ping", null).whenComplete((response, throwable) -> {
                    check_connection(time, response, throwable);
                    this.setMessage(Component.literal("Ping: " + ping));
                });
            }
        };

        this.addRenderableWidget(pingWidget);

        commonY += commonHeight + commonMargin;

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.stanleyparable.token.title"), button -> {
            assert this.minecraft != null;
            this.minecraft.setScreen(new TokenScreen(this));
        }).pos(commonX, commonY).size(commonWidth, commonHeight).build());

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> {
            ClientConfig.COOLDOWN_INDIVIDUAL.set(coolDownIndividual);
            ClientConfig.COOLDOWN_GLOBAL.set(coolDownGlobal);
            ClientConfig.applyServerConfig();
            assert this.minecraft != null;
            this.minecraft.setScreen(this.parent);
        }).pos(commonX, this.height - 30).size(commonWidth, commonHeight).build());
    }

    private void check_connection(long time, JsonObject response, Throwable throwable) {
        if (throwable != null) {
            ping = "Offline";
            LOGGER.error("Error while sending request", throwable);
            return;
        }
        if (response.get("text").getAsString().equals("pong")) {
            ping = "Online " + (System.currentTimeMillis() - time) + "ms";
        } else {
            ping = "Bad response: " + response.get("text").getAsString();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, new Color(252, 56, 103).getRGB());
    }

    int mapToRealInt(double x, int out_min, int out_max) {
        return (int) (x * (out_max - out_min) + out_min);
    }

    double mapToSlideDouble(int x, int out_min, int out_max) {
        return (double) (x - out_min) / (out_max - out_min);
    }
}
