package com.lx862.tprobec.packet;

import io.netty.buffer.Unpooled;
import mtr.RegistryClient;
import com.lx862.tprobec.data.PathDataWithDistance;
import com.lx862.tprobec.data.CompiledTrainData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class PacketTProbeClient {
    public static void requestVehicles(List<Long> sidingIds, Consumer<ResponseCache.TProbeDataResponse> responseCallback) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeUUID(PacketCallbacks.register(responseCallback));
        packet.writeInt(sidingIds.size());
        for(long id : sidingIds) {
            packet.writeLong(id);
        }
        RegistryClient.sendToServer(TProbePackets.PACKET_REQUEST_VEHICLES, packet);
    }

    public static void requestPathData(long depotId, Consumer<ResponseCache.TProbeDataResponse> responseCallback) {
        ResponseCache.TProbeDataResponse existingCache = ResponseCache.get(depotId, "path");
        if(existingCache != null) {
            responseCallback.accept(existingCache);
            return;
        }

        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeUUID(PacketCallbacks.register(responseCallback));
        packet.writeLong(depotId);
        RegistryClient.sendToServer(TProbePackets.PACKET_REQUEST_PATH, packet);
    }

    public static void receivePathData(Minecraft minecraftClient, FriendlyByteBuf packet) {
        UUID responseCallbackUuid = packet.readUUID();
        int pathDataSize = packet.readInt();
        List<PathDataWithDistance> pathData = new ArrayList<>();

        for(int i = 0; i < pathDataSize; i++) {
            pathData.add(new PathDataWithDistance(packet));
        }

        ResponseCache.TProbeDataResponse dataResponse = new ResponseCache.TProbeDataResponse(true, System.currentTimeMillis(), pathData);
        //ResponseCache.save(); // TODO
        PacketCallbacks.invoke(responseCallbackUuid, dataResponse);
    }

    public static void receiveVehicles(Minecraft minecraftClient, FriendlyByteBuf packet) {
        UUID responseCallbackUuid = packet.readUUID();
        int trainSize = packet.readInt();
        List<CompiledTrainData> trainData = new ArrayList<>();

        for(int i = 0; i < trainSize; i++) {
            trainData.add(new CompiledTrainData(packet));
        }
        PacketCallbacks.invoke(responseCallbackUuid, new ResponseCache.TProbeDataResponse(true, System.currentTimeMillis(), trainData));
    }
}
