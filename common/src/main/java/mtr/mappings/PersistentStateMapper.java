package mtr.mappings;

import mtr.storage.NBTRailwaySavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.function.Supplier;

public abstract class PersistentStateMapper extends SavedData {

	public PersistentStateMapper(String name) {
		super();
	}

	public abstract void load(CompoundTag compoundTag);

	protected static <T extends PersistentStateMapper> T getInstance(Level world, Supplier<T> supplier, String name) {
//		if (world instanceof ServerLevel serverLevel) {
//			NBTRailwaySavedData NBTRailwaySavedData = serverLevel.getDataStorage().computeIfAbsent(NBTRailwaySavedData.TYPE);
//
//			return ((ServerLevel) world).getDataStorage().computeIfAbsent(new Factory<>(supplier, (nbtCompound, provider) -> {
//				final T railwayData = supplier.get();
//				railwayData.load(nbtCompound);
//				return railwayData;
//			}, null), name);
//		} else {
			return null;
//		}
	}
}
