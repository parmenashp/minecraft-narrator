package com.mitsuaky.stanleyparable.common.events;

public enum Event {
    ITEM_CRAFTED("item_crafted"),
    BLOCK_BROKEN("block_broken"),
    BLOCK_PLACED("block_placed"),
    PLAYER_DEATH("player_death"),
    ADVANCEMENT("advancement"),
    ITEM_PICKUP("item_pickup"),
    CHEST_CHANGE("chest_change"),
    ITEM_SMELTED("item_smelted"),
    MOB_KILLED("mob_killed"),
    DIMENSION_CHANGED("dimension_changed"),
    TIME_CHANGED("time_changed"),
    PLAYER_CHAT("player_chat"),
    PLAYER_ATE("player_ate"),
    RIDING("riding"),
    WAKE_UP("wake_up"),
    ITEM_FISHED("item_fished"),
    ITEM_REPAIR("item_repair"),
    ANIMAL_BREED("animal_breed"),
    ITEM_TOSS("item_toss"),
    SET_SYSTEM("set_system"),
    CUSTOM_PROMPT("custom_prompt"),
    JOIN_WORLD("join_world");

    private final String value;

    Event(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}