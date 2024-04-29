package com.mitsuaky.stanleyparable.common.events;

public enum SystemEventType implements EventType {
    CUSTOM_TTS("custom_tts"),
    CUSTOM_PROMPT("custom_prompt"),
    SET_SYSTEM("set_system"),
    VOICE_COMPLETE("voice_complete"),
    VOICE_ACTIVATE("voice_activate"),
    CONFIG("config");

    private final String value;

    SystemEventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
