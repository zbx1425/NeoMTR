package com.lx862.tprobe3.packet;

import io.netty.buffer.Unpooled;
import mtr.Registry;
import mtr.data.Depot;
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
import java.util.List;
import java.util.UUID;

public class PacketTProbeDataSender {
    public static void handlePathRequestC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        final UUID responseUuid = packet.readUUID();
        final long depotId = packet.readLong();
        minecraftServer.execute(() -> {
            final RailwayData railwayData = RailwayData.getInstance(minecraftServer.overworld()); // TODO: Multi-DIM handling?
            final List<Siding> sidings = new ArrayList<>();
            railwayData.sidings.forEach(siding -> {
                Depot belongingDepot = railwayData.dataCache.sidingIdToDepot.get(siding.id);
                if(belongingDepot == null || belongingDepot.id != depotId) return;
                sidings.add(siding);
            });

            // TODO: Split it into MTR 4-alike format
            final List<PathData> mainPath = sidings.isEmpty() ? List.of() : sidings.getFirst().getPathData();
            final List<Double> distances = sidings.isEmpty() ? List.of() : sidings.getFirst().getDistances();

            final FriendlyByteBuf newPacket = new FriendlyByteBuf(Unpooled.buffer());
            newPacket.writeUUID(responseUuid);
            newPacket.writeLong(depotId);
            newPacket.writeInt(mainPath.size());

            for(int i = 0; i < mainPath.size(); i++) {
                PathData pathData = mainPath.get(i);
                double distance = distances.get(i);
                new PathDataWithDistance(pathData, distance).write(newPacket);
            }

            sendToPlayer(player, TProbePackets.PACKET_REQUEST_PATH, newPacket);
        });
    }

    public static void handleVehicleRequestC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        final UUID responseUuid = packet.readUUID();
        final int sidingIdSize = packet.readInt();
        final List<Long> sidingIds = new ArrayList<>();

        for(int i = 0; i < sidingIdSize; i++) {
            sidingIds.add(packet.readLong());
        }

        minecraftServer.execute(() -> {
            final List<CompiledTrainData> trainList = new ArrayList<>();
            final RailwayData railwayData = RailwayData.getInstance(minecraftServer.overworld()); // TODO: Multi-DIM handling?
            railwayData.sidings.forEach(siding -> {
                if(sidingIds.contains(siding.id)) {
                    siding.getTrains().forEach(train -> {
                        trainList.add(
                                new CompiledTrainData(train.sidingId, train.id, train.transportMode, train.getSpeed(), train.getRailProgress(), train.getElapsedDwellTicks(), train.getNextStopIndex(), train.isReversed())
                        );
                    });
                }
            });

            final FriendlyByteBuf newPacket = new FriendlyByteBuf(Unpooled.buffer());
            newPacket.writeUUID(responseUuid);
            newPacket.writeInt(trainList.size());

            for(CompiledTrainData compiledTrainData : trainList) {
                compiledTrainData.write(newPacket);
            }

            sendToPlayer(player, TProbePackets.PACKET_REQUEST_VEHICLES, newPacket);
        });
    }

    private static void sendToPlayer(ServerPlayer player, Identifier identifier, FriendlyByteBuf packet) {
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
}
