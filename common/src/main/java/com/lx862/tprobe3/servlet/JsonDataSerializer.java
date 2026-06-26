package com.lx862.tprobe3.servlet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.tprobe3.data.CompiledTrainData;
import com.lx862.tprobe3.data.PathDataWithDistance;
import mtr.data.*;
import net.minecraft.core.BlockPos;

public class JsonDataSerializer {
    public static JsonObject serialize(PathDataWithDistance p) {
        JsonObject pathDataObject = new JsonObject();
        boolean isStop = p.pathData().dwellTime > 0;
        if(isStop) {
            pathDataObject.addProperty("savedRailBaseId", String.valueOf(p.pathData().savedRailBaseId));
            pathDataObject.addProperty("dwellTime", (p.pathData().dwellTime / 2) * 1000);
        }

        int stopIndex = p.pathData().stopIndex - 1; // MTR 4 starts at -1
        if(stopIndex != -1 && !isStop) stopIndex++;

        pathDataObject.addProperty("stopIndex", stopIndex);
        pathDataObject.addProperty("startDistance", p.distance() - p.pathData().rail.getLength());
        pathDataObject.addProperty("endDistance", p.distance());
        pathDataObject.add("startPosition", serialize(p.pathData().startingPos));
        pathDataObject.add("endPosition", serialize(p.pathData().endingPos));
        pathDataObject.addProperty("startAngle", p.pathData().rail.facingStart.angleDegrees);
        pathDataObject.addProperty("endAngle", p.pathData().rail.facingEnd.angleDegrees);
        pathDataObject.addProperty("shape", p.verticalRadius() > 0 ? "TWO_RADII" : "QUADRATIC");
        pathDataObject.addProperty("verticalRadius", p.verticalRadius());
        pathDataObject.addProperty("speedLimit", p.pathData().rail.railType.speedLimit);
        return pathDataObject;
    }

    public static JsonObject serialize(CompiledTrainData vehicle) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", String.valueOf(vehicle.id()));
        jsonObject.addProperty("transportMode", vehicle.transportMode().toString());
        jsonObject.addProperty("name", "");
        jsonObject.addProperty("color", 0);
        jsonObject.addProperty("speed", (vehicle.speed() * 20) / 1000);
        jsonObject.addProperty("railProgress", vehicle.railProgress());
        jsonObject.addProperty("nextStoppingIndexAto", vehicle.nextStopIndex());
        jsonObject.addProperty("nextStoppingIndexManual", vehicle.nextStopIndex());
        jsonObject.addProperty("reversed", vehicle.reversed());
        jsonObject.addProperty("elapsedDwellTime", (vehicle.elapsedDwellTime() / 20) * 1000);
        jsonObject.addProperty("departureIndex", vehicle.departureIndex());
        jsonObject.addProperty("sidingDepartureTime", -1);
        jsonObject.addProperty("isVirtDrive", vehicle.isVirtDrive());
        return jsonObject;
    }

    public static JsonObject serialize(BlockPos pos) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("x", pos.getX());
        jsonObject.addProperty("y", pos.getY());
        jsonObject.addProperty("z", pos.getZ());
        return jsonObject;
    }

    public static JsonObject serialize(Route route, DataCache dataCache) {
        JsonObject jsonObject = serialize(route);
        jsonObject.addProperty("routeType", route.routeType.toString());
        jsonObject.addProperty("routeNumber", route.lightRailRouteNumber);
        jsonObject.addProperty("hidden", route.isHidden);
        jsonObject.addProperty("circularState", route.circularState.toString());

        JsonArray routePlatforms = new JsonArray();

        route.platformIds.forEach(routePlatform -> {
            Station station = dataCache.platformIdToStation.get(routePlatform.platformId);
            JsonObject routePlatformObj = new JsonObject();
            routePlatformObj.addProperty("platformId", String.valueOf(routePlatform.platformId));
            routePlatformObj.addProperty("customDestination", routePlatform.customDestination.get(MultipartName.Usage.GENERIC));

            if(station != null) {
                JsonObject stationObject = serialize(station);
                stationObject.addProperty("zone1", station.zone);
                stationObject.addProperty("zone2", 0);
                stationObject.addProperty("zone3", 0);
                stationObject.add("exits", new JsonArray());
                routePlatformObj.add("station", stationObject);
            }
            routePlatforms.add(routePlatformObj);
        });
        jsonObject.add("routePlatformData", routePlatforms);
        return jsonObject;
    }

    public static JsonObject serialize(NameColorDataBase nameColorDataBase) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", String.valueOf(nameColorDataBase.id));
        jsonObject.addProperty("name", nameColorDataBase.name);
        jsonObject.addProperty("color", nameColorDataBase.color);
        jsonObject.addProperty("transportMode", nameColorDataBase.transportMode.toString());
        return jsonObject;
    }
}
