package com.mitsuaky.stanleyparable.screen;

import com.mitsuaky.stanleyparable.ClientConfig;
import com.mitsuaky.stanleyparable.WebSocketClient;
import com.mitsuaky.stanleyparable.screen.widget.PingWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final int commonWidth = 250;
    private final int commonHeight = 20;
    private int coolDownIndividual = ClientConfig.COOLDOWN_INDIVIDUAL.get();
    private int coolDownGlobal = ClientConfig.COOLDOWN_GLOBAL.get();
    private int narratorVolume = ClientConfig.NARRATOR_VOLUME.get();
    private boolean sendToChat = ClientConfig.SEND_TO_CHAT.get();
    private boolean tts = ClientConfig.TTS.get();

    private String ping = "Offline";

    private PingWidget pingWidget;
    private EditBox akaWidget;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("gui.stanleyparable.config.title"));
        this.parent = parent;
        WebSocketClient.getInstance().setOnPong(time -> {
            long interval = System.currentTimeMillis() - time;
            ping = "Online " + interval + "ms";
            if (pingWidget != null) {
                pingWidget.setMessage(Component.literal("Ping: " + ping));
            }
            return null;
        });
    }

    @Override
    protected void init() {
        super.init();

        try {
            WebSocketClient.getInstance().sendPing().exceptionally(throwable -> {
                ping = "Offline";
                return null;
            });
        } catch (Exception ex) {
            ping = "Offline";
        }


        int commonX = (this.width / 2) - (commonWidth / 2);
        int commonMargin = 5;
        int commonY = 30;
        pingWidget = new PingWidget(commonX, commonY, commonWidth, commonHeight, Component.literal("Ping: " + ping)) {
            @Override
            public void onPress() {
                try {
                    WebSocketClient.getInstance().sendPing().exceptionally(throwable -> {
                        ping = "Offline";
                        this.setMessage(Component.literal("Ping: " + ping));
                        return null;
                    });
                } catch (Exception ex) {
                    ping = "Offline";
                    this.setMessage(Component.literal("Ping: " + ping));
                }
            }
        };

        this.addRenderableWidget(pingWidget);

        commonY += commonHeight + commonMargin;

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
                        mapToSlideDouble(coolDownGlobal, 30, 600)
                ) {
                    @Override
                    protected void updateMessage() {
                        this.setMessage(Component.nullToEmpty(Component.translatable("gui.stanleyparable.cooldown_global").getString() + coolDownGlobal));
                    }

                    @Override
                    protected void applyValue() {
                        coolDownGlobal = mapToRealInt(this.value, 30, 600);
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
                        Component.nullToEmpty(Component.translatable("gui.stanleyparable.narrator_volume").getString() + narratorVolume + "%"),
                        mapToSlideDouble(narratorVolume, 1, 130)
                ) {
                    @Override
                    protected void updateMessage() {
                        this.setMessage(Component.nullToEmpty(Component.translatable("gui.stanleyparable.narrator_volume").getString() + narratorVolume + "%"));
                    }

                    @Override
                    protected void applyValue() {
                        narratorVolume = mapToRealInt(this.value, 1, 130);
                    }
                }
        );

        commonY += commonHeight + commonMargin;

        this.addRenderableWidget(new Checkbox(commonX, commonY, commonWidth / 2 - commonMargin / 2, commonHeight, Component.translatable("gui.stanleyparable.tts"), tts) {
            @Override
            public void onPress() {
                super.onPress();
                tts = this.selected();
                ClientConfig.TTS.set(tts);
            }
        });

        this.addRenderableWidget(new Checkbox(commonX + commonWidth / 2 + commonMargin / 2, commonY, commonWidth / 2 - commonMargin / 2, commonHeight, Component.translatable("gui.stanleyparable.send_to_chat"), sendToChat) {
            @Override
            public void onPress() {
                super.onPress();
                sendToChat = this.selected();
                ClientConfig.SEND_TO_CHAT.set(sendToChat);
            }
        });

        commonY += commonHeight + commonMargin;


        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.stanleyparable.token.title"), button -> {
            ClientConfig.COOLDOWN_INDIVIDUAL.set(coolDownIndividual);
            ClientConfig.COOLDOWN_GLOBAL.set(coolDownGlobal);
            ClientConfig.NARRATOR_VOLUME.set(narratorVolume);
            ClientConfig.AKA.set(akaWidget.getValue());
            assert this.minecraft != null;
            this.minecraft.setScreen(new TokenScreen(this));
        }).pos(commonX, commonY).size(commonWidth, commonHeight).build());

        commonY += commonHeight + commonMargin;

        akaWidget = new EditBox(this.font, commonX, commonY, commonWidth, commonHeight, Component.translatable("gui.stanleyparable.aka"));
        akaWidget.setValue(ClientConfig.AKA.get());
        akaWidget.hint = Component.translatable("gui.stanleyparable.aka");
        this.addRenderableWidget(akaWidget);

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> {
            ClientConfig.COOLDOWN_INDIVIDUAL.set(coolDownIndividual);
            ClientConfig.COOLDOWN_GLOBAL.set(coolDownGlobal);
            ClientConfig.NARRATOR_VOLUME.set(narratorVolume);
            ClientConfig.AKA.set(akaWidget.getValue());
            ClientConfig.applyServerConfig();
            WebSocketClient.getInstance().setOnPong(null);
            assert this.minecraft != null;
            this.minecraft.setScreen(this.parent);
        }).pos(commonX, this.height - 30).size(commonWidth, commonHeight).build());
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
