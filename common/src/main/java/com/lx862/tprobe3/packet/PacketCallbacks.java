package com.lx862.tprobe3.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class PacketCallbacks {
    private static final Map<UUID, Consumer<ResponseCache.TProbeDataResponse>> callbacks = new HashMap<>();

    public static UUID register(Consumer<ResponseCache.TProbeDataResponse> callback) {
        UUID newUuid = UUID.randomUUID();
        callbacks.put(newUuid, callback);
        return newUuid;
    }

    public static void invoke(UUID uuid, ResponseCache.TProbeDataResponse dataResponse) {
        Consumer<ResponseCache.TProbeDataResponse> callback = callbacks.get(uuid);
        if(callback != null) {
            callback.accept(dataResponse);
            callbacks.remove(uuid);
        }
    }
}
