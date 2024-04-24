package com.mitsuaky.stanleyparable.server.player;

import com.mitsuaky.stanleyparable.common.network.PacketHandler;
import com.mitsuaky.stanleyparable.common.network.PacketSyncPlayerData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerData implements ICapabilitySerializable<CompoundTag> {
    private String vulgo;

    private final LazyOptional<PlayerData> capability;

    public PlayerData() {
        this.capability = LazyOptional.of(() -> this);
    }

    public static PlayerData get(Player player) {
        return player.getCapability(CapabilityManager.get(new CapabilityToken<PlayerData>() {
        })).orElseThrow(() -> new IllegalStateException("Could not get player data capability"));
    }

    public String getVulgo() {
        return vulgo;
    }

    public void setVulgo(String vulgo) {
        this.vulgo = vulgo;
        sync();
    }

    private void sync() {
        PacketHandler.sendToServer(new PacketSyncPlayerData(vulgo));
    }

    public void updateFromPacket(String vulgo) {
        this.vulgo = vulgo;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
        return CapabilityManager.get(new CapabilityToken<PlayerData>() {
        }).orEmpty(capability, this.capability);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("vulgo", vulgo);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag compoundTag) {
        vulgo = compoundTag.getString("vulgo");
    }
}
