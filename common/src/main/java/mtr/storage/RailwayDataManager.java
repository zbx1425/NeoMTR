package mtr.storage;

import mtr.data.RailwayData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class RailwayDataManager {
    private static final Map<Identifier, RailwayData> railwayData = new HashMap<>();

    public static RailwayData getInstance(ServerLevel level) {
        NBTRailwaySavedData nbtRailwaySavedData = level.getDataStorage().computeIfAbsent(NBTRailwaySavedData.TYPE);
        Identifier dimensionId = dimensionId(level);
        return railwayData.computeIfAbsent(dimensionId, dimId -> {
            RailwayData newRailwayData = new RailwayData(level);
            newRailwayData.load(nbtRailwaySavedData.getCompoundTag());
            return newRailwayData;
        });
    }

    public static void save(ServerLevel level, boolean fullSave) {
        RailwayData instance = getInstance(level);
        NBTRailwaySavedData nbtRailwaySavedData = level.getDataStorage().computeIfAbsent(NBTRailwaySavedData.TYPE);
        instance.save(fullSave);
        instance.save(nbtRailwaySavedData.getCompoundTag());
        nbtRailwaySavedData.setDirty();
    }

    private static Identifier dimensionId(Level level) {
        return level.dimension().identifier();
    }
}
