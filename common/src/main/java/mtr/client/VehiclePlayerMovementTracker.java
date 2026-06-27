package mtr.client;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VehiclePlayerMovementTracker {
    private static Map<UUID, Vec3> deltaMovementTick = new HashMap<>();
    private static Map<UUID, Vec3> deltaMovementLastTick = new HashMap<>();
    private static boolean consumeTick = false;

    public static void addMovement(UUID uuid, Vec3 movement) {
        Vec3 existingDelta = deltaMovementTick.computeIfAbsent(uuid, k -> Vec3.ZERO);
        deltaMovementTick.put(uuid, existingDelta.add(movement));
    }

    public static void tick() {
        deltaMovementLastTick.putAll(deltaMovementTick);
        deltaMovementTick.clear();
        consumeTick = true;
    }

    public static float getBobbingFactor(UUID uuid) {
        return Math.min(0.1F, (float) getDeltaMovement(uuid).horizontalDistance());
    }

    public static float getBobDistance(UUID uuid) {
        return (float) getDeltaMovement(uuid).length() * 0.5f;
    }

    public static Vec3 getDeltaMovement(UUID uuid) {
        return deltaMovementTick.getOrDefault(uuid, Vec3.ZERO);
    }

    public static Vec3 getDeltaMovementLastTick(UUID uuid) {
        return deltaMovementLastTick.getOrDefault(uuid, Vec3.ZERO);
    }

    public static boolean oneTickElapsed() {
        boolean tick = consumeTick;
        consumeTick = false;
        return tick;
    }
}
