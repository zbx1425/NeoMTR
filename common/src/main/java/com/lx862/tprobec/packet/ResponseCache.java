package com.lx862.tprobec.packet;

import java.util.HashMap;
import java.util.Map;

public class ResponseCache {
    private static final int TTL = 1000;
    private static final Map<RequestPath, TProbeDataResponse> cache = new HashMap<>();

    public static TProbeDataResponse get(long depotId, String path) {
        TProbeDataResponse existing = cache.get(new RequestPath(depotId, path));
        if(existing != null && !existing.expired()) return existing;
        return null;
    }

    public static void save(long depotId, String path, TProbeDataResponse tProbeDataResponse) {
        cache.put(new RequestPath(depotId, path), tProbeDataResponse);
    }

    record RequestPath(long depotId, String path) {}

    public record TProbeDataResponse(boolean dataExists, long responseTime, Object data) {
        boolean expired() {
            return System.currentTimeMillis() - responseTime > TTL;
        }
    }
}
