package com.mitsuaky.stanleyparable.server;

import com.mitsuaky.stanleyparable.StanleyParableMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;


public class WorldSavedData extends SavedData {
    private boolean adventureMode = false;

    @Override
    public @NotNull CompoundTag save(CompoundTag compoundTag) {
        compoundTag.putBoolean("adventureMode", adventureMode);
        return compoundTag;
    }

    public boolean isAdventureMode() {
        return adventureMode;
    }

    public void setAdventureMode(boolean adventureMode) {
        this.adventureMode = adventureMode;
        setDirty();
    }

    public static WorldSavedData load(CompoundTag compoundTag) {
        WorldSavedData data = new WorldSavedData();
        data.adventureMode = compoundTag.getBoolean("adventureMode");
        return data;
    }

    public static WorldSavedData get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldSavedData::new, WorldSavedData::load, DataFixTypes.SAVED_DATA_MAP_DATA), StanleyParableMod.MOD_ID + "_world_data");
    }
}
