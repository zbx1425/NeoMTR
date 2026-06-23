package com.lx862.tprobe3.servlet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.tprobe3.data.DepotPathData;
import mtr.data.*;
import com.lx862.tprobe3.data.PathDataWithDistance;
import com.lx862.tprobe3.packet.PacketTProbeRequester;
import com.lx862.tprobe3.data.CompiledTrainData;
import mtr.path.PathData;
import mtr.servlet.IServletHandler;
import mtr.servlet.Webserver;
import net.minecraft.core.BlockPos;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepotServletHandler extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String pathInfo = request.getPathInfo();
        if(pathInfo == null) {
            handleDepotListing(request, response);
        } else {
            String[] segments = pathInfo.split("/");
            int segmentSize = segments.length-1;
            if(segmentSize == 0) {
                handleDepotListing(request, response);
            } else if(segmentSize == 2) {
                String depotId = segments[1];
                String operation = segments[2];
                switch(operation) {
                    case "path" -> handlePath(request, response, depotId);
                    case "vehicles" -> handleVehicles(request, response, depotId);
                    default -> handleSkillIssue(request, response);
                }
            } else {
                handleSkillIssue(request, response);
            }
        }
    }

    private void handlePath(HttpServletRequest request, HttpServletResponse response, String depotIdStr) {
        final AsyncContext asyncContext = request.isAsyncStarted() ? request.getAsyncContext() : request.startAsync();
        final long depotId = Long.parseLong(depotIdStr);

        Webserver.minecraftCallback.runOnMainThread(() -> {
            boolean[] depotFound = new boolean[]{false};
            final JsonObject data = new JsonObject();

            Webserver.minecraftCallback.getLevels().forEach(world -> {
                final RailwayData railwayData = RailwayData.getInstance(world);
                final DataCache dataCache = Webserver.minecraftCallback.getDataCache(railwayData);
                final Depot depot = dataCache.depotIdMap.get(depotId);
                if(depot != null) {
                    depotFound[0] = true;
                    data.addProperty("id", String.valueOf(depot.id));
                    data.addProperty("name", depot.name);

                    JsonArray routes = new JsonArray();
                    depot.routeIds.forEach(e -> {
                        Route route = dataCache.routeIdMap.get(e);
                        routes.add(serialize(route, dataCache));
                    });
                    data.add("routes", routes);

                    final JsonArray mainPath = new JsonArray();
                    PacketTProbeRequester.requestPathData(depotId, (callback) -> {
                        if(!callback.packetSuccess()) {
                            JsonObject respObject = TProbeHelper.getTProbeResponse(503, "Service Unavailable", null);
                            IServletHandler.sendResponse(response, asyncContext, respObject.toString());
                            return;
                        }

                        DepotPathData pathData = (DepotPathData)callback.data();
                        pathData.mainPath().forEach(p -> mainPath.add(serialize(p)));
                        data.add("path", mainPath);

                        JsonArray sidingArray = new JsonArray();
                        for(DepotPathData.SidingPathData sidingData : pathData.sidings()) {
                            Siding siding = dataCache.sidingIdMap.get(sidingData.sidingId());
                            JsonObject sidingObject = serialize(siding);

                            JsonArray pathSidingToMainRoute = new JsonArray();
                            sidingData.pathSidingToMainRoute().forEach(p -> pathSidingToMainRoute.add(serialize(p)));
                            JsonArray pathMainRouteToSiding = new JsonArray();
                            sidingData.pathMainRouteToSiding().forEach(p -> pathMainRouteToSiding.add(serialize(p)));

                            sidingObject.add("pathSidingToMainRoute", pathSidingToMainRoute);
                            sidingObject.add("pathMainRouteToSiding", pathMainRouteToSiding);
                            sidingArray.add(sidingObject);
                        }

                        data.add("sidings", sidingArray);
                        JsonObject respObject = TProbeHelper.getTProbeResponse(200, "OK", data);
                        IServletHandler.sendResponse(response, asyncContext, respObject.toString());
                    }, Webserver.minecraftCallback.getServer());
                }
            });

            if(!depotFound[0]) {
                handleSkillIssue(request, response);
            }
        });
    }

    private void handleVehicles(HttpServletRequest request, HttpServletResponse response, String depotIdStr) {
        final AsyncContext asyncContext = request.isAsyncStarted() ? request.getAsyncContext() : request.startAsync();
        final long depotId = Long.parseLong(depotIdStr);

        Webserver.minecraftCallback.runOnMainThread(() -> {
            boolean[] found = new boolean[]{false};
            final JsonObject data = new JsonObject();

            Webserver.minecraftCallback.getLevels().forEach(world -> {
                final RailwayData railwayData = RailwayData.getInstance(world);
                final DataCache dataCache = Webserver.minecraftCallback.getDataCache(railwayData);
                final Depot depot = dataCache.depotIdMap.get(depotId);
                if(depot != null) {
                    found[0] = true;
                    data.addProperty("id", String.valueOf(depot.id));
                    data.addProperty("name", depot.name);

                    Map<Long, JsonObject> sidingsData = new HashMap<>();

                    dataCache.sidingIdMap.values().forEach(siding -> {
                        if(dataCache.sidingIdToDepot.get(siding.id).id == depotId) {
                            JsonObject sidingObject = serialize(siding);
                            sidingObject.add("vehicles", new JsonArray());
                            sidingsData.put(siding.id, sidingObject);
                        }
                    });

                    PacketTProbeRequester.requestVehicles(new ArrayList<>(sidingsData.keySet()), (callback) -> {
                        if(!callback.packetSuccess()) {
                            JsonObject respObject = TProbeHelper.getTProbeResponse(503, "Service Unavailable", null);
                            IServletHandler.sendResponse(response, asyncContext, respObject.toString());
                            return;
                        }
                        // "Trust me bro"
                        List<CompiledTrainData> pathData = (List<CompiledTrainData>)callback.data();
                        pathData.forEach(vehicle -> {
                            JsonObject vehicleObject = new JsonObject();
                            vehicleObject.addProperty("id", String.valueOf(vehicle.id()));
                            vehicleObject.addProperty("transportMode", vehicle.transportMode().toString());
                            vehicleObject.addProperty("name", "");
                            vehicleObject.addProperty("color", 0);
                            vehicleObject.addProperty("speed", (vehicle.speed() * 20) / 1000);
                            vehicleObject.addProperty("railProgress", vehicle.railProgress());
                            vehicleObject.addProperty("nextStoppingIndexAto", vehicle.nextStopIndex());
                            vehicleObject.addProperty("nextStoppingIndexManual", vehicle.nextStopIndex());
                            vehicleObject.addProperty("reversed", vehicle.reversed());
                            vehicleObject.addProperty("elapsedDwellTime", (vehicle.elapsedDwellTime() / 20) * 1000);
                            vehicleObject.addProperty("departureIndex", vehicle.departureIndex());
                            vehicleObject.addProperty("sidingDepartureTime", -1);

                            sidingsData.get(vehicle.sidingId()).getAsJsonArray("vehicles").add(vehicleObject);
                        });

                        JsonArray sidingArray = new JsonArray();
                        sidingsData.values().forEach(e -> sidingArray.add(e));

                        data.add("sidings", sidingArray);
                        JsonObject respObject = TProbeHelper.getTProbeResponse(200, "OK", data);
                        IServletHandler.sendResponse(response, asyncContext, respObject.toString());
                    }, Webserver.minecraftCallback.getServer());
                }
            });

            if(!found[0]) {
                handleSkillIssue(request, response);
            }
        });
    }

    private static JsonObject serialize(PathDataWithDistance p) {
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
        pathDataObject.addProperty("shape", "QUADRATIC");
        pathDataObject.addProperty("verticalRadius", 0.0);
        pathDataObject.addProperty("speedLimit", p.pathData().rail.railType.speedLimit);
        return pathDataObject;
    }

    private static JsonObject serialize(BlockPos pos) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("x", pos.getX());
        jsonObject.addProperty("y", pos.getY());
        jsonObject.addProperty("z", pos.getZ());
        return jsonObject;
    }

    private static JsonObject serialize(Route route, DataCache dataCache) {
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

    private static JsonObject serialize(NameColorDataBase nameColorDataBase) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", String.valueOf(nameColorDataBase.id));
        jsonObject.addProperty("name", nameColorDataBase.name);
        jsonObject.addProperty("color", nameColorDataBase.color);
        jsonObject.addProperty("transportMode", nameColorDataBase.transportMode.toString());
        return jsonObject;
    }

    private void handleSkillIssue(HttpServletRequest request, HttpServletResponse response) {
        final AsyncContext asyncContext = request.isAsyncStarted() ? request.getAsyncContext() : request.startAsync();

        Webserver.minecraftCallback.runOnMainThread(() -> {
            final JsonObject data = new JsonObject();
            data.addProperty("code", 404);

            JsonObject respObject = TProbeHelper.getTProbeResponse(200, "OK", data);
            IServletHandler.sendResponse(response, asyncContext, respObject.toString());
        });
    }

    private void handleDepotListing(HttpServletRequest request, HttpServletResponse response) {
        final AsyncContext asyncContext = request.isAsyncStarted() ? request.getAsyncContext() : request.startAsync();

        Webserver.minecraftCallback.runOnMainThread(() -> {
            final JsonObject data = new JsonObject();
            final JsonArray depots = new JsonArray();

            Webserver.minecraftCallback.getLevels().forEach(world -> {
                final RailwayData railwayData = RailwayData.getInstance(world);
                final DataCache dataCache = Webserver.minecraftCallback.getDataCache(railwayData);

                if(dataCache != null) {
                    dataCache.depotIdMap.values().forEach(dp -> {
                        JsonObject depotObject = new JsonObject();
                        depotObject.addProperty("id", String.valueOf(dp.id));
                        depotObject.addProperty("name", String.valueOf(dp.name));

                        JsonArray depotRoutes = new JsonArray();

                        dp.routeIds.forEach(rtId -> {
                            Route route = dataCache.routeIdMap.get(rtId);
                            JsonObject routeObject = new JsonObject();
                            routeObject.addProperty("id", String.valueOf(route.id));
                            routeObject.addProperty("name", route.name);
                            depotRoutes.add(routeObject);
                        });
                        depotObject.add("routes", depotRoutes);
                        depots.add(depotObject);
                    });
                }
            });

            data.add("depots", depots);

            JsonObject respObject = TProbeHelper.getTProbeResponse(200, "OK", data);
            IServletHandler.sendResponse(response, asyncContext, respObject.toString());
        });
    }
}
