package com.lx862.tprobe3.data;

import mtr.data.TransportMode;
import net.minecraft.network.FriendlyByteBuf;

public record CompiledTrainData(long sidingId, long id, TransportMode transportMode, double speed, double railProgress, double elapsedDwellTime, int nextStopIndex, boolean reversed) {
    public CompiledTrainData(FriendlyByteBuf packet) {
        long sidingId = packet.readLong();
        long id = packet.readLong();
        TransportMode transportMode = TransportMode.valueOf(packet.readUtf());
        double speed = packet.readDouble();
        double railProgress = packet.readDouble();
        double elapsedDwellTime = packet.readDouble();
        int nextStopIndex = packet.readInt();
        boolean reversed = packet.readBoolean();
        this(sidingId, id, transportMode, speed, railProgress, elapsedDwellTime, nextStopIndex, reversed);
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
    }
}
