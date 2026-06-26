package com.lx862.tprobe3.data;

import cn.zbx1425.mtrsteamloco.game.TrainVirtualDrive;
import mtr.data.TrainClient;
import mtr.data.TransportMode;
import net.minecraft.network.FriendlyByteBuf;

public record CompiledTrainData(long sidingId, long id, TransportMode transportMode, double speed, double railProgress, double elapsedDwellTime, int nextStopIndex, boolean reversed, int departureIndex, boolean isVirtDrive) {
    public CompiledTrainData(FriendlyByteBuf packet) {
        long sidingId = packet.readLong();
        long id = packet.readLong();
        TransportMode transportMode = TransportMode.valueOf(packet.readUtf());
        double speed = packet.readDouble();
        double railProgress = packet.readDouble();
        double elapsedDwellTime = packet.readDouble();
        int nextStopIndex = packet.readInt();
        boolean reversed = packet.readBoolean();
        int departureIndex = packet.readInt();
        this(sidingId, id, transportMode, speed, railProgress, elapsedDwellTime, nextStopIndex, reversed, departureIndex, false);
    }

    public static CompiledTrainData fromTrainClient(TrainVirtualDrive trainClient) {
        long sidingId = trainClient.sidingId;
        long id = trainClient.id;
        TransportMode transportMode = trainClient.transportMode;
        double speed = trainClient.getSpeed();
        double railProgress = trainClient.getRailProgress();
        double elapsedDwellTime = trainClient.getElapsedDwellTicks();
        int nextStopIndex = trainClient.nextPlatformIndex; // TrainVirtualDrive have nextStoppingIndex as last path, so use nextPlatformIndex instead.
        boolean reversed = trainClient.isReversed();
        int departureIndex = 0;
        return new CompiledTrainData(sidingId, id, transportMode, speed, railProgress, elapsedDwellTime, nextStopIndex, reversed, departureIndex, trainClient instanceof TrainVirtualDrive);
    }

    public void write(FriendlyByteBuf packet) {
        packet.writeLong(sidingId);
        packet.writeLong(id);
        packet.writeUtf(transportMode.toString());
        packet.writeDouble(speed);
        packet.writeDouble(railProgress);
        packet.writeDouble(elapsedDwellTime);
        packet.writeInt(nextStopIndex);
        packet.writeBoolean(reversed);
        packet.writeInt(departureIndex);
    }
}
