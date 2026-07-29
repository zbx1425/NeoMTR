package com.lx862.tprobe3.servlet;

import cn.zbx1425.mtrsteamloco.game.TrainVirtualDrive;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.tprobe3.data.DepotPathData;
import mtr.client.ClientData;
import mtr.data.*;
import com.lx862.tprobe3.packet.PacketTProbeRequester;
import com.lx862.tprobe3.data.CompiledTrainData;
import mtr.servlet.IServletHandler;
import mtr.servlet.Webserver;

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
                        routes.add(JsonDataSerializer.serialize(route, dataCache));
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
                        pathData.mainPath().forEach(p -> mainPath.add(JsonDataSerializer.serialize(p)));
                        data.add("path", mainPath);

                        JsonArray sidingArray = new JsonArray();
                        for(DepotPathData.SidingPathData sidingData : pathData.sidings()) {
                            Siding siding = dataCache.sidingIdMap.get(sidingData.sidingId());
                            JsonObject sidingObject = JsonDataSerializer.serialize(siding);

                            JsonArray pathSidingToMainRoute = new JsonArray();
                            sidingData.pathSidingToMainRoute().forEach(p -> pathSidingToMainRoute.add(JsonDataSerializer.serialize(p)));
                            JsonArray pathMainRouteToSiding = new JsonArray();
                            sidingData.pathMainRouteToSiding().forEach(p -> pathMainRouteToSiding.add(JsonDataSerializer.serialize(p)));

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
                        Depot belongingDepot = dataCache.sidingIdToDepot.get(siding.id);
                        if(belongingDepot != null && belongingDepot.id == depotId) {
                            JsonObject sidingObject = JsonDataSerializer.serialize(siding);
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

                        List<CompiledTrainData> trainData = (List<CompiledTrainData>)callback.data();
                        trainData.forEach(vehicle -> {
                            sidingsData.get(vehicle.sidingId()).getAsJsonArray("vehicles").add(JsonDataSerializer.serialize(vehicle));
                        });

                        Webserver.minecraftCallback.getExtraTrains().forEach(extraTrain -> {
                            JsonObject jsonObject = new JsonObject();
                            jsonObject.add("vehicles", new JsonArray());
                            sidingsData.getOrDefault(extraTrain.sidingId(), jsonObject).getAsJsonArray("vehicles").add(JsonDataSerializer.serialize(extraTrain));
                        });

                        JsonArray sidingArray = new JsonArray();
                        sidingsData.values().forEach(sidingArray::add);

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
