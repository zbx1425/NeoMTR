package mtr.storage;

import com.mojang.serialization.Codec;
import mtr.data.RailwayData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class NBTRailwaySavedData extends SavedData {
    private static final Codec<NBTRailwaySavedData> CODEC = CompoundTag.CODEC.xmap(NBTRailwaySavedData::new, NBTRailwaySavedData::getCompoundTag);
    public static final SavedDataType<NBTRailwaySavedData> TYPE = new SavedDataType<>(
            Identifier.parse(RailwayData.NAME),
            NBTRailwaySavedData::new,
            CODEC,
            null
    );

    private final CompoundTag compoundTag;

    public NBTRailwaySavedData() {
        this.compoundTag = new CompoundTag();
    }

    public NBTRailwaySavedData(CompoundTag compoundTag) {
        this.compoundTag = compoundTag;
    }

    public CompoundTag getCompoundTag() {
        return this.compoundTag;
    }
}
