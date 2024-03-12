package com.mitsuaky.stanleyparable;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBinding {
    public static final String KEY_CATEGORY = "key.category.narrator";
    public static final String KEY_VOICE_ID = "key.voice_activate";

    public static final KeyMapping VOICE_KEY = new KeyMapping(
            KEY_VOICE_ID, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, KEY_CATEGORY
    );
}
