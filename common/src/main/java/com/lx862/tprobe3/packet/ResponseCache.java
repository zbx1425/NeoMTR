package com.lx862.tprobe3.packet;

import java.util.HashMap;
import java.util.Map;

public class ResponseCache {
    private static final int TTL = 1000;
    private static final Map<RequestPath, TProbeDataResponse> cache = new HashMap<>();

    public static TProbeDataResponse get(long depotId, String path) {
        TProbeDataResponse existing = cache.get(new RequestPath(path, String.valueOf(depotId)));
        if(existing != null && !existing.expired()) return existing;
        return null;
    }

    public static void save(String path, String requestKey, TProbeDataResponse tProbeDataResponse) {
        cache.put(new RequestPath(path, requestKey), tProbeDataResponse);
    }

    record RequestPath(String path, String requestKey) {}

    public record TProbeDataResponse(boolean dataExists, long responseTime, Object data) {
        boolean expired() {
            return System.currentTimeMillis() - responseTime > TTL;
        }
    }
}
