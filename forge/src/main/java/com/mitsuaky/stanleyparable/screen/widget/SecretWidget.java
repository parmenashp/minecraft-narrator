package com.mitsuaky.stanleyparable.screen.widget;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SecretWidget extends EditBox {
    private static final WidgetSprites SPRITES = new WidgetSprites(new ResourceLocation("widget/text_field"), new ResourceLocation("widget/text_field_highlighted"));
    Font font;

    public SecretWidget(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        super(pFont, pX, pY, pWidth, pHeight, pMessage);
        this.font = pFont;
        this.setFormatter((s, i) -> FormattedCharSequence.forward("●".repeat(s.length()), Style.EMPTY));
    }

    public void onClick(double pMouseX, double pMouseY) {
        int mousePosX = Mth.floor(pMouseX) - this.getX();
        if (this.bordered) {
            mousePosX -= 4;
        }

        String crop = font.plainSubstrByWidth("●".repeat(this.value.length()).substring(this.displayPos), this.width - 8);
        this.moveCursorTo(font.plainSubstrByWidth(crop, mousePosX).length() + this.displayPos, Screen.hasShiftDown());
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.isVisible()) {
            if (this.isBordered()) {
                ResourceLocation editBoxSprite = SPRITES.get(this.isActive(), this.isFocused());
                pGuiGraphics.blitSprite(editBoxSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
            }

            int color = this.isEditable ? this.textColor : this.textColorUneditable;
            int normalizePos = this.cursorPos - this.displayPos;
            String text = this.font.plainSubstrByWidth("●".repeat(this.value.length()).substring(this.displayPos), this.getInnerWidth());
            boolean onRange = normalizePos >= 0 && normalizePos <= text.length();
            boolean focus = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L && onRange;
            int pX = this.bordered ? this.getX() + 4 : this.getX();
            int pY = this.bordered ? this.getY() + (this.height - 8) / 2 : this.getY();
            int pXholder = pX;
            int clamp = Mth.clamp(this.highlightPos - this.displayPos, 0, text.length());
            if (!text.isEmpty()) {
                String displayText = onRange ? text.substring(0, normalizePos) : text;
                pXholder = pGuiGraphics.drawString(this.font, this.formatter.apply(displayText, this.displayPos), pX, pY, color);
            }

            boolean scroll = this.cursorPos < this.value.length() || this.value.length() >= this.getMaxLength();
            int pMinX = pXholder;
            if (!onRange) {
                pMinX = normalizePos > 0 ? pX + this.width : pX;
            } else if (scroll) {
                pMinX = pXholder - 1;
                --pXholder;
            }

            if (!text.isEmpty() && onRange && normalizePos < text.length()) {
                pGuiGraphics.drawString(this.font, this.formatter.apply(text.substring(normalizePos), this.cursorPos), pXholder, pY, color);
            }

            if (this.hint != null && text.isEmpty() && !this.isFocused()) {
                pGuiGraphics.drawString(this.font, this.hint, pXholder, pY, color);
            }

            if (!scroll && this.suggestion != null) {
                pGuiGraphics.drawString(this.font, this.suggestion, pMinX - 1, pY, -8355712);
            }

            int pMinY;
            int pMaxX;
            int pMaxY;
            if (focus) {
                if (scroll) {
                    RenderType renderType = RenderType.guiOverlay();
                    pMinY = pY - 1;
                    pMaxX = pMinX + 1;
                    pMaxY = pY + 1;
                    Objects.requireNonNull(this.font);
                    pGuiGraphics.fill(renderType, pMinX, pMinY, pMaxX, pMaxY + 9, -3092272);
                } else {
                    pGuiGraphics.drawString(this.font, "_", pMinX, pY, color);
                }
            }

            if (clamp != normalizePos) {
                int textWidth = pX + this.font.width(text.substring(0, clamp));
                pMinY = pY - 1;
                pMaxX = textWidth - 1;
                pMaxY = pY + 1;
                Objects.requireNonNull(this.font);
                this.renderHighlight(pGuiGraphics, pMinX, pMinY, pMaxX, pMaxY + 9);
            }
        }
    }

    @Override
    public void scrollTo(int pPosition) {
        if (this.font != null) {
            this.displayPos = Math.min(this.displayPos, this.value.length());
            int innerWidth = this.getInnerWidth();
            String string = this.font.plainSubstrByWidth("●".repeat(this.value.length()).substring(this.displayPos), innerWidth);
            int completeLength = string.length() + this.displayPos;
            if (pPosition == this.displayPos) {
                this.displayPos -= this.font.plainSubstrByWidth("●".repeat(this.value.length()), innerWidth, true).length();
            }

            if (pPosition > completeLength) {
                this.displayPos += pPosition - completeLength;
            } else if (pPosition <= this.displayPos) {
                this.displayPos -= this.displayPos - pPosition;
            }

            this.displayPos = Mth.clamp(this.displayPos, 0, this.value.length());
        }
    }
}
