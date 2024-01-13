package com.mitsuaky.stanleyparable.screen;

import com.mitsuaky.stanleyparable.ClientConfig;
import com.mitsuaky.stanleyparable.screen.widget.SecretWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

    private boolean more = false;
    private Button moreButton;

    public TokenScreen(Screen parent) {
        super(Component.translatable("gui.stanleyparable.token.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int commonWidth = 200;
        int commonHeight = 20;
        int commonX = (this.width / 2) - (commonWidth / 2);
        int commonMargin = 15;
        int commonY = 40;

        openAIKeyEditBox = new SecretWidget(font, commonX, commonY, commonWidth, commonHeight, Component.translatable("gui.stanleyparable.openai_key"));
        openAIKeyEditBox.setMaxLength(32500);
        openAIKeyEditBox.setValue(ClientConfig.OPENAI_API_KEY.get());
        this.addRenderableWidget(openAIKeyEditBox);

        commonY += commonHeight + commonMargin;


        elevenLabsAPIKeyEditBox = new SecretWidget(font, commonX, commonY, commonWidth, commonHeight, Component.translatable("gui.stanleyparable.elevenlabs_api_key"));
        elevenLabsAPIKeyEditBox.setMaxLength(32500);
        elevenLabsAPIKeyEditBox.setValue(ClientConfig.ELEVENLABS_API_KEY.get());
        this.addRenderableWidget(elevenLabsAPIKeyEditBox);

        commonY += commonHeight + commonMargin;

        elevenLabsVoiceIdEditBox = new EditBox(font, commonX, commonY, commonWidth, commonHeight, Component.translatable("gui.stanleyparable.elevenlabs_voice_id"));
        elevenLabsVoiceIdEditBox.setMaxLength(32500);
        elevenLabsVoiceIdEditBox.setValue(ClientConfig.ELEVENLABS_VOICE_ID.get());
        this.addRenderableWidget(elevenLabsVoiceIdEditBox);

        commonY += commonHeight + commonMargin;


        openAIBaseURLEditBox = new EditBox(font, commonX, commonY, commonWidth, commonHeight, Component.translatable("gui.stanleyparable.openai_base_url"));
        openAIBaseURLEditBox.setMaxLength(32500);
        openAIBaseURLEditBox.setValue(ClientConfig.OPENAI_BASE_URL.get());

        commonY += commonHeight + commonMargin;

        openAIModelEditBox = new EditBox(font, commonX, commonY, commonWidth, commonHeight, Component.translatable("gui.stanleyparable.openai_model"));
        openAIModelEditBox.setMaxLength(32500);
        openAIModelEditBox.setValue(ClientConfig.OPENAI_MODEL.get());

        int moreButtonY = elevenLabsVoiceIdEditBox.getY() + commonHeight + 5;
        int lessButtonY = openAIModelEditBox.getY() + commonHeight + 5;
        moreButton = new Button.Builder(more ? Component.translatable("gui.stanleyparable.less") : Component.translatable("gui.stanleyparable.more"), (button) -> {
            more = !more;
            this.addWidget(openAIBaseURLEditBox);
            this.addWidget(openAIModelEditBox);
            if (more) {
                moreButton.setMessage(Component.translatable("gui.stanleyparable.less"));
                moreButton.setY(lessButtonY);
                LOGGER.info("More options enabled");
            } else {
                moreButton.setMessage(Component.translatable("gui.stanleyparable.more"));
                moreButton.setY(moreButtonY);
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
        }
    }
}
