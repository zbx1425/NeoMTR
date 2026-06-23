package com.lx862.tprobe3.data;

import java.util.ArrayList;
import java.util.List;

public record DepotPathData(List<PathDataWithDistance> mainPath, List<SidingPathData> sidings) {
    public record SidingPathData(long sidingId, List<PathDataWithDistance> pathSidingToMainRoute, List<PathDataWithDistance> pathMainRouteToSiding) {
        public SidingPathData(long sidingId) {
            this(sidingId, new ArrayList<>(), new ArrayList<>());
        }
    }

    public DepotPathData() {
        this(new ArrayList<>(), new ArrayList<>());
    }
}
