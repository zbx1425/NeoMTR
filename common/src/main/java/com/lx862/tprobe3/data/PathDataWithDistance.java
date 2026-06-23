package com.lx862.tprobe3.data;

import mtr.path.PathData;
import net.minecraft.network.FriendlyByteBuf;

public record PathDataWithDistance(PathData pathData, double distance) {

    public PathDataWithDistance(FriendlyByteBuf packet) {
        double dist = packet.readDouble();
        PathData pathData1 = new PathData(packet);
        this(pathData1, dist);
    }

    public void write(FriendlyByteBuf packet) {
        packet.writeDouble(distance);
        this.pathData.writePacket(packet);
    }
}
