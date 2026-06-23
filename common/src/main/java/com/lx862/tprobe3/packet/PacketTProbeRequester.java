package com.lx862.tprobe3.packet;

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
        UUID responseCallbackUuid = packet.readUUID();
        long depotId = packet.readLong();
        int pathDataSize = packet.readInt();
        List<PathDataWithDistance> pathData = new ArrayList<>();

        for(int i = 0; i < pathDataSize; i++) {
            pathData.add(new PathDataWithDistance(packet));
        }

        ResponseCache.TProbeDataResponse dataResponse = new ResponseCache.TProbeDataResponse(true, System.currentTimeMillis(), pathData);
        ResponseCache.save("path", String.valueOf(depotId), dataResponse);
        PacketCallbacks.invoke(responseCallbackUuid, dataResponse);
    }

    public static void receiveVehicles(FriendlyByteBuf packet) {
        UUID responseCallbackUuid = packet.readUUID();
        int trainSize = packet.readInt();
        List<CompiledTrainData> trainData = new ArrayList<>();

        for(int i = 0; i < trainSize; i++) {
            trainData.add(new CompiledTrainData(packet));
        }

        ResponseCache.TProbeDataResponse tProbeDataResponse = new ResponseCache.TProbeDataResponse(true, System.currentTimeMillis(), trainData);
        PacketCallbacks.invoke(responseCallbackUuid, tProbeDataResponse);
    }

    private static void sendToServer(Identifier identifier, FriendlyByteBuf packet, MinecraftServer minecraftServer) {
        if(minecraftServer != null) { // Bound for server itself
            if(identifier.equals(TProbePackets.PACKET_REQUEST_PATH)) {
                PacketTProbeDataSender.handlePathRequestC2S(minecraftServer, null, packet);
            } else if(identifier.equals(TProbePackets.PACKET_REQUEST_VEHICLES)) {
                PacketTProbeDataSender.handleVehicleRequestC2S(minecraftServer, null, packet);
            } else {
                throw new IllegalStateException("TProbe3: Cannot send unknown packet " + identifier + "!");
            }
        } else {
            RegistryClient.sendToServer(identifier, packet);
        }
    }
}
