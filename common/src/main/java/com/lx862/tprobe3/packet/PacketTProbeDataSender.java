package com.lx862.tprobe3.packet;

import com.lx862.tprobe3.config.TProbe3KillSwitch;
import com.lx862.tprobe3.data.DepotPathData;
import io.netty.buffer.Unpooled;
import mtr.MTR;
import mtr.Registry;
import mtr.data.Depot;
import mtr.data.RailType;
import mtr.data.RailwayData;
import mtr.data.Siding;
import com.lx862.tprobe3.data.PathDataWithDistance;
import com.lx862.tprobe3.data.CompiledTrainData;
import mtr.path.PathData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PacketTProbeDataSender {

    public static void handle(MinecraftServer minecraftServer, Identifier identifier, ServerPlayer player, FriendlyByteBuf recvPacket, DataHandler dataHandler) {
        final UUID responseUuid = recvPacket.readUUID();
        if(TProbe3KillSwitch.activated()) {
            minecraftServer.execute(() -> {
                final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
                packet.writeBoolean(false);
                packet.writeUUID(responseUuid);
                sendToRequester(player, identifier, packet);
            });
            return;
        }

        try {
            dataHandler.handle(minecraftServer, identifier, player, recvPacket, responseUuid);
        } catch (Exception e) {
            MTR.LOGGER.error("Failed to handle TProbe Request Packet!", e);
        }
    }

    public static void handlePathRequestC2S(MinecraftServer minecraftServer, Identifier identifier, ServerPlayer player, FriendlyByteBuf recvPacket, UUID responseUuid) {
        final long depotId = recvPacket.readLong();
        minecraftServer.execute(() -> {
            final FriendlyByteBuf packet = newPacket(responseUuid);
            final RailwayData railwayData = RailwayData.getInstance(minecraftServer.overworld()); // TODO: Multi-DIM handling?
            final List<Siding> sidings = new ArrayList<>();
            railwayData.sidings.forEach(siding -> {
                Depot belongingDepot = railwayData.dataCache.sidingIdToDepot.get(siding.id);
                if(belongingDepot == null || belongingDepot.id != depotId) return;
                sidings.add(siding);
            });

            final List<PathDataWithDistance> mainPath = new ArrayList<>();
            packet.writeLong(depotId);
            packet.writeInt(sidings.size());

            for(int i = 0; i < sidings.size(); i++) {
                Siding siding = sidings.get(i);
                final List<PathData> fullSidingPath = siding.getPathData();
                final List<Double> sdDistances = siding.getDistances();
                final boolean shouldAppendMainPath = i == 0;

                int mainPathBegin = -1;
                int mainPathEnd = -1;

                final DepotPathData.SidingPathData sidingPath = new DepotPathData.SidingPathData(siding.id);
                final List<PathDataWithDistance> pathSidingToMainRoute = sidingPath.pathSidingToMainRoute();
                final List<PathDataWithDistance> pathMainRouteToSiding = sidingPath.pathMainRouteToSiding();

                // SDG To Main Route Pass
                for(int j = 0; j < fullSidingPath.size(); j++) {
                    PathData pathData = fullSidingPath.get(j);
                    double distance = sdDistances.get(j);

                    // Note: In MTR 4, the first platform is included in pathSdgToMainRoute.
                    pathSidingToMainRoute.add(new PathDataWithDistance(pathData, distance));
                    if(isStoppingPlatform(pathData)) {
                        mainPathBegin = j;
                        break;
                    }
                }

                // Main Route to SDG Pass
                for(int j = fullSidingPath.size()-1; j > 0; j--) {
                    PathData pathData = fullSidingPath.get(j);
                    double distance = sdDistances.get(j);
                    if(isStoppingPlatform(pathData)) {
                        mainPathEnd = j+1;
                        break;
                    }
                    pathMainRouteToSiding.add(new PathDataWithDistance(pathData, distance));
                }
                Collections.reverse(pathMainRouteToSiding);

                // Main Path pass
                if(shouldAppendMainPath && mainPathBegin != -1 && mainPathEnd != -1) {
                    double baseDistance = pathSidingToMainRoute.isEmpty() ? 0 : pathSidingToMainRoute.getLast().distance();
                    for(int j = mainPathBegin; j < mainPathEnd; j++) {
                        PathData pathData = fullSidingPath.get(j);
                        double distance = sdDistances.get(j) - baseDistance;
                        mainPath.add(new PathDataWithDistance(pathData, distance));
                    }
                }

                packet.writeLong(siding.id);
                packet.writeInt(pathSidingToMainRoute.size());
                pathSidingToMainRoute.forEach(p -> p.write(packet));
                packet.writeInt(pathMainRouteToSiding.size());

                double newReturnDistance = mainPath.getLast().distance();
                for(PathDataWithDistance p : pathMainRouteToSiding) {
                    newReturnDistance += p.distance();
                    PathDataWithDistance withNewDistance = new PathDataWithDistance(p.pathData(), newReturnDistance, p.verticalRadius());
                    withNewDistance.write(packet);
                }
            }

            packet.writeInt(mainPath.size());
            mainPath.forEach(p -> p.write(packet));

            sendToRequester(player, identifier, packet);
        });
    }

    private static boolean isStoppingPlatform(PathData pathData) {
        return pathData.dwellTime > 0 && pathData.rail.railType == RailType.PLATFORM;
    }

    public static void handleVehicleRequestC2S(MinecraftServer minecraftServer, Identifier identifier, ServerPlayer player, FriendlyByteBuf recvPacket, UUID responseUuid) {
        final int sidingIdSize = recvPacket.readInt();
        final List<Long> sidingIds = new ArrayList<>();

        for(int i = 0; i < sidingIdSize; i++) {
            sidingIds.add(recvPacket.readLong());
        }

        minecraftServer.execute(() -> {
            final List<CompiledTrainData> trainList = new ArrayList<>();
            final RailwayData railwayData = RailwayData.getInstance(minecraftServer.overworld()); // TODO: Multi-DIM handling?
            railwayData.sidings.forEach(siding -> {
                if(sidingIds.contains(siding.id)) {
                    siding.getTrains().forEach(train -> {
                        final int stopIndex = train.getNextStopIndex();
                        trainList.add(
                            new CompiledTrainData(train.sidingId, train.id, train.transportMode, train.getSpeed(), train.getRailProgress(), train.getElapsedDwellTicks(), stopIndex, train.isReversed(), train.getDepartureIndex(), false)
                        );
                    });
                }
            });

            final FriendlyByteBuf newPacket = newPacket(responseUuid);
            newPacket.writeInt(trainList.size());

            for(CompiledTrainData compiledTrainData : trainList) {
                compiledTrainData.write(newPacket);
            }

            sendToRequester(player, identifier, newPacket);
        });
    }

    private static void sendToRequester(ServerPlayer player, Identifier identifier, FriendlyByteBuf packet) {
        if(player == null) { // Bound for server itself
            if(identifier.equals(TProbePackets.PACKET_REQUEST_PATH)) {
                PacketTProbeRequester.receivePathData(packet);
            } else if(identifier.equals(TProbePackets.PACKET_REQUEST_VEHICLES)) {
                PacketTProbeRequester.receiveVehicles(packet);
            } else {
                throw new IllegalStateException("TProbe3: Cannot handle unknown packet " + identifier + "!");
            }
        } else {
            Registry.sendToPlayer(player, identifier, packet);
        }
    }

    private static FriendlyByteBuf newPacket(UUID responseUuid) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBoolean(true); // Request Success Flag
        packet.writeUUID(responseUuid);
        return packet;
    }

    @FunctionalInterface
    public interface DataHandler {
        void handle(MinecraftServer minecraftServer, Identifier identifier, ServerPlayer player, FriendlyByteBuf recvPacket, UUID responseUuid);
    }
}
