package com.mitsuaky.stanleyparable.client.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class PingWidget extends AbstractButton {
    protected static final WidgetSprites BUTTON_SPRITES = new WidgetSprites(new ResourceLocation("widget/button"), new ResourceLocation("widget/button_disabled"), new ResourceLocation("widget/button_highlighted"));
    protected static final ResourceLocation RESTART_ICON = new ResourceLocation("stanleyparable", "widget/restart_icon");

    public PingWidget(int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        super(pX, pY, pWidth, pHeight, pMessage);
    }

    public void onClick(double pMouseX, double pMouseY) {
        this.onPress();
    }

    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        pGuiGraphics.blitSprite(BUTTON_SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), 20, this.getHeight());
        pGuiGraphics.blitSprite(RESTART_ICON, this.getX() + 2, this.getY() + 2, 16, 16);
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        pGuiGraphics.drawString(minecraft.font, this.getMessage(), this.getX() + 24, this.getY() + (this.height - 8) / 2, 14737632 | Mth.ceil(this.alpha * 255.0F) << 24);
    }

    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.active && this.visible) {
            if (CommonInputs.selected(pKeyCode)) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.onPress();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
        this.defaultButtonNarrationText(pNarrationElementOutput);
    }

    public void onPress() {
    }
}
