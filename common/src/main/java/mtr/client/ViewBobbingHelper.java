package mtr.client;

import net.minecraft.world.phys.Vec3;

public class ViewBobbingHelper {
    private static Vec3 deltaMovementTick = new Vec3(0, 0, 0);

    public static void addMovement(Vec3 movement) {
        deltaMovementTick = deltaMovementTick.add(movement);
    }

    public static void tick() {
        deltaMovementTick = new Vec3(0, 0, 0);
    }

    public static float getBobbingFactor() {
        return Math.min(0.1F, (float) deltaMovementTick.horizontalDistance());
    }

    public static float getBobDistance() {
        return (float) deltaMovementTick.length() * 0.5f;
    }
}
