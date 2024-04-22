package com.mitsuaky.stanleyparable.client.screen;

import com.mitsuaky.stanleyparable.client.ClientConfig;
import com.mitsuaky.stanleyparable.client.screen.widget.SecretWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TokenScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger(ConfigScreen.class);
    private final Screen parent;

    private SecretWidget openAIKeyEditBox;
    private EditBox openAIBaseURLEditBox;
    private EditBox openAIModelEditBox;

    private SecretWidget elevenLabsAPIKeyEditBox;
    private EditBox elevenLabsVoiceIdEditBox;

    private int elevenLabsBufferSize = ClientConfig.ELEVENLABS_BUFFER_SIZE.get();
    private int chatGPTBufferSize = ClientConfig.CHATGPT_BUFFER_SIZE.get();

    private Checkbox elevenLabsStreamingButton;
    private Checkbox openAiStreamingButton;

    private boolean elevenLabsStreaming = ClientConfig.ELEVENLABS_STREAMING.get();
    private boolean openAiStreaming = ClientConfig.OPENAI_STREAMING.get();

    private boolean more = false;
    private Button moreButton;

    public TokenScreen(Screen parent) {
        super(Component.translatable("gui.stanleyparable.token.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int commonWidth = 300;
        int commonHeight = 20;
        int commonX = (this.width / 2) - (commonWidth / 2);
        int commonTextMargin = 15;
        int commonMargin = 5;
        int commonY = 40;

        openAIKeyEditBox = new SecretWidget(font, commonX, commonY, commonWidth / 2 - commonMargin / 2, commonHeight, Component.translatable("gui.stanleyparable.openai_key"));
        openAIKeyEditBox.setMaxLength(32500);
        openAIKeyEditBox.setValue(ClientConfig.OPENAI_API_KEY.get());
        this.addRenderableWidget(openAIKeyEditBox);

        elevenLabsAPIKeyEditBox = new SecretWidget(font, commonX + commonWidth / 2 + commonMargin / 2, commonY, commonWidth / 2 - commonMargin / 2, commonHeight, Component.translatable("gui.stanleyparable.elevenlabs_api_key"));
        elevenLabsAPIKeyEditBox.setMaxLength(32500);
        elevenLabsAPIKeyEditBox.setValue(ClientConfig.ELEVENLABS_API_KEY.get());
        this.addRenderableWidget(elevenLabsAPIKeyEditBox);

        commonY += commonHeight + commonTextMargin;

        elevenLabsVoiceIdEditBox = new EditBox(font, commonX, commonY, commonWidth, commonHeight, Component.translatable("gui.stanleyparable.elevenlabs_voice_id"));
        elevenLabsVoiceIdEditBox.setMaxLength(32500);
        elevenLabsVoiceIdEditBox.setValue(ClientConfig.ELEVENLABS_VOICE_ID.get());
        this.addRenderableWidget(elevenLabsVoiceIdEditBox);

        commonY += commonHeight + commonMargin;

        this.addRenderableWidget(
                new AbstractSliderButton(
                        commonX,
                        commonY,
                        commonWidth,
                        commonHeight,
                        Component.nullToEmpty(Component.translatable("gui.stanleyparable.buffer_elevenlabs").getString() + elevenLabsBufferSize),
                        mapToSlideDouble(bufferToInt(elevenLabsBufferSize), 8, 12)
                ) {
                    @Override
                    protected void updateMessage() {
                        this.setMessage(Component.nullToEmpty(Component.translatable("gui.stanleyparable.buffer_elevenlabs").getString() + elevenLabsBufferSize));
                    }

                    @Override
                    protected void applyValue() {
                        elevenLabsBufferSize = intToBuffer(mapToRealInt(this.value, 8, 12));
                    }
                }
        );

        commonY += commonHeight + commonMargin;

        AbstractSliderButton chatGPTBufferSizeSlider = new AbstractSliderButton(
                commonX,
                commonY,
                commonWidth,
                commonHeight,
                Component.nullToEmpty(Component.translatable("gui.stanleyparable.buffer_chatgpt").getString() + chatGPTBufferSize),
                mapToSlideDouble(chatGPTBufferSize, 10, 500)
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.nullToEmpty(Component.translatable("gui.stanleyparable.buffer_chatgpt").getString() + chatGPTBufferSize));
            }

            @Override
            protected void applyValue() {
                chatGPTBufferSize = mapToRealInt(this.value, 10, 500);
            }
        };

        this.addRenderableWidget(chatGPTBufferSizeSlider);

        commonY += commonHeight + commonTextMargin;

        openAIBaseURLEditBox = new EditBox(font, commonX, commonY, commonWidth / 2 - commonMargin / 2, commonHeight, Component.translatable("gui.stanleyparable.openai_base_url"));
        openAIBaseURLEditBox.setMaxLength(32500);
        openAIBaseURLEditBox.setValue(ClientConfig.OPENAI_BASE_URL.get());

        openAIModelEditBox = new EditBox(font, commonX + commonWidth / 2 + commonMargin / 2, commonY, commonWidth / 2 - commonMargin / 2, commonHeight, Component.translatable("gui.stanleyparable.openai_model"));
        openAIModelEditBox.setMaxLength(32500);
        openAIModelEditBox.setValue(ClientConfig.OPENAI_MODEL.get());

        commonY += commonHeight + commonMargin;

        elevenLabsStreamingButton = new Checkbox(commonX, commonY, commonWidth / 2 - commonMargin / 2, commonHeight, Component.translatable("gui.stanleyparable.elevenlabs_streaming"), elevenLabsStreaming) {
            @Override
            public void onPress() {
                super.onPress();
                elevenLabsStreaming = this.selected();
            }
        };

        openAiStreamingButton = new Checkbox(commonX + commonWidth / 2 + commonMargin / 2, commonY, commonWidth / 2 - commonMargin / 2, commonHeight, Component.translatable("gui.stanleyparable.openai_streaming"), openAiStreaming) {
            @Override
            public void onPress() {
                super.onPress();
                openAiStreaming = this.selected();
            }
        };

        int moreButtonY = chatGPTBufferSizeSlider.getY() + commonHeight + commonMargin;
        int lessButtonY = openAiStreamingButton.getY() + commonHeight + commonMargin;
        moreButton = new Button.Builder(more ? Component.translatable("gui.stanleyparable.less") : Component.translatable("gui.stanleyparable.more"), (button) -> {
            more = !more;
            if (more) {
                moreButton.setMessage(Component.translatable("gui.stanleyparable.less"));
                moreButton.setY(lessButtonY);
                this.addWidget(openAIBaseURLEditBox);
                this.addWidget(openAIModelEditBox);
                this.addWidget(openAiStreamingButton);
                this.addWidget(elevenLabsStreamingButton);
                LOGGER.info("More options enabled");
            } else {
                moreButton.setMessage(Component.translatable("gui.stanleyparable.more"));
                moreButton.setY(moreButtonY);
                this.removeWidget(openAIBaseURLEditBox);
                this.removeWidget(openAIModelEditBox);
                this.removeWidget(openAiStreamingButton);
                this.removeWidget(elevenLabsStreamingButton);
                LOGGER.info("More options disabled");
            }
        }).pos(commonX, (more) ? lessButtonY : moreButtonY).size(commonWidth, commonHeight).build();
        this.addRenderableWidget(moreButton);

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), (button) -> {
            ClientConfig.OPENAI_API_KEY.set(openAIKeyEditBox.getValue());
            ClientConfig.OPENAI_BASE_URL.set(openAIBaseURLEditBox.getValue());
            ClientConfig.OPENAI_MODEL.set(openAIModelEditBox.getValue());
            ClientConfig.ELEVENLABS_API_KEY.set(elevenLabsAPIKeyEditBox.getValue());
            ClientConfig.ELEVENLABS_VOICE_ID.set(elevenLabsVoiceIdEditBox.getValue());
            ClientConfig.ELEVENLABS_BUFFER_SIZE.set(elevenLabsBufferSize);
            ClientConfig.CHATGPT_BUFFER_SIZE.set(chatGPTBufferSize);
            ClientConfig.ELEVENLABS_STREAMING.set(elevenLabsStreaming);
            ClientConfig.OPENAI_STREAMING.set(openAiStreaming);
            LOGGER.info("Tokens saved");
            assert this.minecraft != null;
            this.minecraft.setScreen(this.parent);
        }).pos(commonX, this.height - 30).size(commonWidth, commonHeight).build());
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.drawCenteredString(font, this.title, this.width / 2, 10, new Color(252, 56, 103).getRGB());
        pGuiGraphics.drawString(font, openAIKeyEditBox.getMessage(), openAIKeyEditBox.getX(), openAIKeyEditBox.getY() - 10, Color.WHITE.getRGB());
        pGuiGraphics.drawString(font, elevenLabsAPIKeyEditBox.getMessage(), elevenLabsAPIKeyEditBox.getX(), elevenLabsAPIKeyEditBox.getY() - 10, Color.WHITE.getRGB());
        pGuiGraphics.drawString(font, elevenLabsVoiceIdEditBox.getMessage(), elevenLabsVoiceIdEditBox.getX(), elevenLabsVoiceIdEditBox.getY() - 10, Color.WHITE.getRGB());
        if (more) {
            pGuiGraphics.drawString(font, openAIBaseURLEditBox.getMessage(), openAIBaseURLEditBox.getX(), openAIBaseURLEditBox.getY() - 10, Color.WHITE.getRGB());
            openAIBaseURLEditBox.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            pGuiGraphics.drawString(font, openAIModelEditBox.getMessage(), openAIModelEditBox.getX(), openAIModelEditBox.getY() - 10, Color.WHITE.getRGB());
            openAIModelEditBox.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            openAiStreamingButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            elevenLabsStreamingButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }
    }

    int mapToRealInt(double x, int out_min, int out_max) {
        return (int) (x * (out_max - out_min) + out_min);
    }

    double mapToSlideDouble(int x, int out_min, int out_max) {
        return (double) (x - out_min) / (out_max - out_min);
    }

    int intToBuffer(int x) {
        return (int) (4 * Math.pow(2, x));
    }

    int bufferToInt(int x) {
        return (int) (Math.log((double) x / 4) / Math.log(2));
    }
}
