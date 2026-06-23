package com.lx862.tprobe3.packet;

import com.lx862.tprobe3.data.DepotPathData;
import io.netty.buffer.Unpooled;
import mtr.RegistryClient;
import com.lx862.tprobe3.data.PathDataWithDistance;
import com.lx862.tprobe3.data.CompiledTrainData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class PacketTProbeRequester {
    public static void requestVehicles(List<Long> sidingIds, Consumer<ResponseCache.TProbeDataResponse> responseCallback, MinecraftServer minecraftServer) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeUUID(PacketCallbacks.register(responseCallback));
        packet.writeInt(sidingIds.size());
        for(long id : sidingIds) {
            packet.writeLong(id);
        }
        sendToServer(TProbePackets.PACKET_REQUEST_VEHICLES, packet, minecraftServer);
    }

    public static void requestPathData(long depotId, Consumer<ResponseCache.TProbeDataResponse> responseCallback, MinecraftServer minecraftServer) {
        ResponseCache.TProbeDataResponse existingCache = ResponseCache.get(depotId, "path");
        if(existingCache != null) {
            responseCallback.accept(existingCache);
            return;
        }

        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeUUID(PacketCallbacks.register(responseCallback));
        packet.writeLong(depotId);
        sendToServer(TProbePackets.PACKET_REQUEST_PATH, packet, minecraftServer);
    }

    public static void receivePathData(FriendlyByteBuf packet) {
        boolean requestSuccessful = packet.readBoolean();
        UUID responseCallbackUuid = packet.readUUID();

        DepotPathData depotPathData = null;
        long depotId = -1;

        if(requestSuccessful) {
            depotId = packet.readLong();
            int sidingAmount = packet.readInt();
            depotPathData = new DepotPathData();

            for(int i = 0; i < sidingAmount; i++) {
                long sidingId = packet.readLong();
                int sdgToMainRouteSize = packet.readInt();
                final DepotPathData.SidingPathData sidingPathData = new DepotPathData.SidingPathData(sidingId);
                for(int j = 0; j < sdgToMainRouteSize; j++) {
                    sidingPathData.pathSidingToMainRoute().add(new PathDataWithDistance(packet));
                }
                int mainRouteToSdgSize = packet.readInt();
                for(int j = 0; j < mainRouteToSdgSize; j++) {
                    sidingPathData.pathMainRouteToSiding().add(new PathDataWithDistance(packet));
                }
                depotPathData.sidings().add(sidingPathData);
            }

            int mainPathSize = packet.readInt();
            for(int j = 0; j < mainPathSize; j++) {
                depotPathData.mainPath().add(new PathDataWithDistance(packet));
            }
        }

        ResponseCache.TProbeDataResponse dataResponse = new ResponseCache.TProbeDataResponse(requestSuccessful, System.currentTimeMillis(), depotPathData);
        ResponseCache.save("path", String.valueOf(depotId), dataResponse);
        PacketCallbacks.invoke(responseCallbackUuid, dataResponse);
    }

    public static void receiveVehicles(FriendlyByteBuf packet) {
        boolean requestSuccessful = packet.readBoolean();
        UUID responseCallbackUuid = packet.readUUID();
        List<CompiledTrainData> trainData = null;
        if(requestSuccessful) {
            int trainSize = packet.readInt();
            trainData = new ArrayList<>();

            for(int i = 0; i < trainSize; i++) {
                trainData.add(new CompiledTrainData(packet));
            }
        }


        ResponseCache.TProbeDataResponse tProbeDataResponse = new ResponseCache.TProbeDataResponse(requestSuccessful, System.currentTimeMillis(), trainData);
        PacketCallbacks.invoke(responseCallbackUuid, tProbeDataResponse);
    }

    private static void sendToServer(Identifier identifier, FriendlyByteBuf packet, MinecraftServer minecraftServer) {
        if(minecraftServer != null) { // Bound for server itself
            if(identifier.equals(TProbePackets.PACKET_REQUEST_PATH)) {
                PacketTProbeDataSender.handle(minecraftServer, identifier, null, packet, PacketTProbeDataSender::handlePathRequestC2S);
            } else if(identifier.equals(TProbePackets.PACKET_REQUEST_VEHICLES)) {
                PacketTProbeDataSender.handle(minecraftServer, identifier, null, packet, PacketTProbeDataSender::handleVehicleRequestC2S);
            } else {
                throw new IllegalStateException("TProbe3: Cannot send unknown packet " + identifier + "!");
            }
        } else {
            RegistryClient.sendToServer(identifier, packet);
        }
    }
}
