package com.lx862.tprobe3.data;

import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import mtr.path.PathData;
import net.minecraft.network.FriendlyByteBuf;

public record PathDataWithDistance(PathData pathData, double distance, float verticalRadius) {

    public PathDataWithDistance(PathData pathData, double distance) {
        this(pathData, distance, ((RailExtraSupplier)pathData.rail).getVerticalCurveRadius());
    }

    public PathDataWithDistance(FriendlyByteBuf packet) {
        double dist = packet.readDouble();
        float verticalRadius = packet.readFloat();
        PathData pathData1 = new PathData(packet);
        this(pathData1, dist, verticalRadius);
    }

    public void write(FriendlyByteBuf packet) {
        packet.writeDouble(distance);
        packet.writeFloat(verticalRadius);
        this.pathData.writePacket(packet);
    }
}
