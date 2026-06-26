package cn.zbx1425.mtrsteamloco.game;

import cn.zbx1425.mtrsteamloco.network.PacketVirtualDrive;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import mtr.MTR;
import mtr.client.ClientData;
import mtr.data.RailwayDataCoolDownModule;
import mtr.data.TrainClient;
import mtr.mappings.Text;
import mtr.path.PathData;
import mtr.render.RenderTrains;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Random;

public class TrainVirtualDrive extends TrainClient {

    public TrainVirtualDrive(TrainClient trainClient) {
        super(Util.make(() -> {
            FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
            trainClient.writePacket(packet);
            int prevWriterIndex = packet.writerIndex();
            packet.writerIndex(0);
            packet.writeLong(new Random().nextLong()); // Get a new ID
            packet.writerIndex(prevWriterIndex);
            packet.resetReaderIndex();
            return packet;
        }));
        for (PathData path : this.path) {
            vdMaxSpeed = Math.max(vdMaxSpeed, path.rail.railType.maxBlocksPerTick);
        }
        vdMaxSpeed += 20 / 20f / 3.6f;
    }

    public int vdNotch = 0;
    public int vdReverser = 1;
    public float vdMaxSpeed;

    public int nextPlatformIndex;
    public double nextPlatformRailProgress;
    public float atpYellowSpeed;
    public float atpRedSpeed;
    public float atpTargetSpeed;
    public double atpTargetDistance;
    public boolean atpEmergencyBrake;
    public boolean atpCutout = false;

    private int doorOpenedAtPlatformIndex = 0;

    public LongArrayList railAheadLookup = new LongArrayList();
    private final DelayedValue actualNotch = new DelayedValue(0.5);

    public int powerNotches = 5;
    public int brakeNotches = 7;
    public float yellowSpeedBrakeRatio = 0.7f;
    public float redSpeedBrakeRatio = 0.9f;

    public float motorPowerPerCar = 240000; // 240kW when averaged per car
    public float weightPerCar = 27000; // 27t when averaged per car

    public double PROG_TOLERANCE = 5;

    @Override
    public void simulateTrain(Level world, float ticksElapsed, SpeedCallback speedCallback, AnnouncementCallback announcementCallback, AnnouncementCallback lightRailAnnouncementCallback) {
        isCurrentlyManual = true;
        manualNotch = 114514; // Magic number to bypass base class manual driving logic
        nextStoppingIndex = path.size() - 1;
        super.simulateTrain(world, ticksElapsed, speedCallback, announcementCallback, lightRailAnnouncementCallback);
        if (!isOnRoute || ClientData.getShiftHoldingTicks() >= RailwayDataCoolDownModule.SHIFT_ACTIVATE_TICKS) {
            stopDriving();
            return;
        }

        // Repeat
        if (isRepeat()) {
            int headIndex = getIndex(railProgress + PROG_TOLERANCE, false);
            if (headIndex >= repeatIndex2 && distances.size() > repeatIndex1) {
                if (path.get(repeatIndex2).isOppositeRail(path.get(repeatIndex1))) {
                    if (speed <= 0) {
                        railProgress = distances.get(repeatIndex1 - 1) + trainCars * spacing;
                        nextPlatformIndex = 0;
                        reversed = !reversed;
                        Minecraft.getInstance().player.sendSystemMessage(Text.translatable("gui.mtrsteamloco.drive.change_end"));
                    }
                } else {
                    railProgress = distances.get(repeatIndex1) + (railProgress - distances.get(repeatIndex2));
                    nextPlatformIndex = 0;
                }
            }
        }
        // Turn back
        if (speed <= 0) {
            int tailIndex = getIndex(railProgress - spacing * trainCars + PROG_TOLERANCE, false);
            if (path.size() > tailIndex + 1 && Math.abs(railProgress - distances.get(tailIndex)) < PROG_TOLERANCE
                && path.get(tailIndex).isOppositeRail(path.get(tailIndex + 1))) {
                railProgress = distances.get(tailIndex) + trainCars * spacing + 0.1; // 0.1 to avoid red speed at 0
                reversed = !reversed;
                Minecraft.getInstance().player.sendSystemMessage(Text.translatable("gui.mtrsteamloco.drive.change_end"));
            }
        }

        float accelDueToFriction = (-0.2f / 400 / 3.6f);
        float sinPitch = (float)(keyPointsPositions[0].y - keyPointsPositions[keyPointsPositions.length - 1].y)
                / (spacing * trainCars - ((spacing - 1) / 2f - getBogiePosition()) * 2f) * (reversed ? 1 : -1);
        float accelDueToPitch = (sinPitch * 9.8f / 400);
        float passiveAccel = accelDueToFriction + accelDueToPitch;
        float commandNotch;
        if (atpEmergencyBrake) {
            commandNotch = -2;
            if (speed <= 0 && vdNotch < 0) atpEmergencyBrake = false;
        } else {
            commandNotch = getPercentNotch();
        }
        float actualNotch = this.actualNotch.setAndGet(commandNotch, ticksElapsed);
        if (actualNotch < -1) {
            speed = Mth.clamp(speed - 1.1f * (accelerationConstant / yellowSpeedBrakeRatio) * ticksElapsed + passiveAccel * ticksElapsed, 0, vdMaxSpeed);
        } else if (actualNotch < 0) {
            speed = Mth.clamp(speed + actualNotch * (accelerationConstant / yellowSpeedBrakeRatio) * ticksElapsed + passiveAccel * ticksElapsed, 0, vdMaxSpeed);
        } else if (actualNotch > 0) {
            float maxPowerAccel = motorPowerPerCar / (weightPerCar * (speed * 20));
            float motorOutputAccel;
            if (accelerationConstant * 400 > maxPowerAccel) {
                // Constant power
                motorOutputAccel = maxPowerAccel * actualNotch;
            } else {
                // Constant force
                motorOutputAccel = (accelerationConstant * 400) * actualNotch;
            }
            speed = Mth.clamp(speed + (motorOutputAccel / 400) * ticksElapsed + passiveAccel * ticksElapsed, 0, vdMaxSpeed);
        } else {
            speed = Mth.clamp(speed + passiveAccel * ticksElapsed, 0, vdMaxSpeed);
        }
        if (doorValue > 0 || doorTarget) {
            speed = 0;
        }

        // Update next platform
        if (distances.get(nextPlatformIndex) < railProgress - 10) {
            for (int i = nextPlatformIndex; i < path.size(); i++) {
                if (path.get(i).dwellTime > 0) {
                    if (distances.get(i) >= railProgress - 10) {
                        nextPlatformIndex = i;
                        nextPlatformRailProgress = distances.get(i);
                        break;
                    }
                }
                if (i == path.size() - 1) {
                    nextPlatformIndex = i;
                    nextPlatformRailProgress = distances.get(i);
                }
            }
        }

        // Update rail segments ahead
        railAheadLookup.clear();
        atpYellowSpeed = vdMaxSpeed;
        atpRedSpeed = vdMaxSpeed;
        atpTargetSpeed = vdMaxSpeed;
        float targetPatternSpeed = vdMaxSpeed;
        float effectiveLimit = vdMaxSpeed;
        atpTargetDistance = distances.getLast();
        double lookAheadDistance = Math.max(300, Math.pow(speed, 2) / (2 * accelerationConstant));
        for (int i = getIndex(railProgress - spacing * trainCars, true);
            i < path.size() && distances.get(i) < railProgress + lookAheadDistance; i++) {
            PathData pathSeg = path.get(i);
            railAheadLookup.add(pathSeg.startingPos.asLong());
            if (i > 0 && distances.get(i - 1) < railProgress && distances.get(i) > railProgress - spacing * trainCars) {
                // Persisting speed limit
                atpYellowSpeed = Math.min(atpYellowSpeed, pathSeg.rail.railType.maxBlocksPerTick);
                atpRedSpeed = Math.min(atpRedSpeed, pathSeg.rail.railType.maxBlocksPerTick + 5 / 3.6f / 20);
                effectiveLimit = Math.min(effectiveLimit, pathSeg.rail.railType.maxBlocksPerTick);
            }
            if (pathSeg.dwellTime > 0 && i != doorOpenedAtPlatformIndex) {
                // Stop point pattern
                if (distances.get(i) > railProgress) {
                    float patternSpeed = (float)Math.sqrt(2 * accelerationConstant * (distances.get(i) - railProgress));
                    if (patternSpeed < atpYellowSpeed) {
                        atpYellowSpeed = patternSpeed;
                    }
                    if (patternSpeed < targetPatternSpeed) {
                        targetPatternSpeed = patternSpeed;
                        atpTargetSpeed = 0;
                        atpTargetDistance = distances.get(i);
                    }
                } else if (distances.get(i) > railProgress - 10) {
                    atpYellowSpeed = 0;
                    targetPatternSpeed = 0;
                    atpTargetSpeed = 0;
                    atpTargetDistance = distances.get(i);
                }
                if (Math.abs(distances.get(i) - railProgress) < 5 && (doorTarget || doorValue > 0)) {
                    doorOpenedAtPlatformIndex = i;
                }
                // Turn back rail
                if (i + 1 < path.size() && pathSeg.isOppositeRail(path.get(i + 1))) {
                    atpRedSpeed = Math.min(atpRedSpeed,
                            (float)Math.sqrt(2 * accelerationConstant / yellowSpeedBrakeRatio * redSpeedBrakeRatio * (distances.get(i) - railProgress)));
                }
            } else {
                if (i > 0 && distances.get(i - 1) > railProgress) {
                    // Decelerate to start of speed limit
                    float yellowPatternSpeed = (float) Math.sqrt(2 * accelerationConstant * (distances.get(i - 1) - railProgress)
                                    + Math.pow(pathSeg.rail.railType.maxBlocksPerTick, 2));
                    if (yellowPatternSpeed < atpYellowSpeed) {
                        atpYellowSpeed = yellowPatternSpeed;
                    }
                    if (pathSeg.rail.railType.maxBlocksPerTick < effectiveLimit && yellowPatternSpeed < targetPatternSpeed) {
                        targetPatternSpeed = yellowPatternSpeed;
                        atpTargetSpeed = pathSeg.rail.railType.maxBlocksPerTick;
                        atpTargetDistance = distances.get(i - 1);
                    }
                    atpRedSpeed = Math.min(atpRedSpeed,
                            (float)Math.sqrt(2 * accelerationConstant / yellowSpeedBrakeRatio * redSpeedBrakeRatio * (distances.get(i - 1) - railProgress)
                                    + Math.pow(pathSeg.rail.railType.maxBlocksPerTick + 5 / 3.6f / 20, 2)));
                }
            }
        }
        if (atpTargetDistance == distances.getLast()) {
            atpTargetSpeed = 0;
        }
        if (doorValue > 0 || doorTarget) {
            atpYellowSpeed = 0;
            atpRedSpeed = 0;
            atpTargetSpeed = -1;
        }
        if (speed > atpRedSpeed && !atpCutout) {
            atpEmergencyBrake = true;
        }
    }

    public static TrainVirtualDrive activeTrain;

    /** @return Whether the player is riding a train and the virtual driving is started
     */
    public static boolean startDrivingRidingTrain() {
        if (activeTrain != null) return false;
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        for (TrainClient train : ClientData.TRAINS) {
            if (train.isPlayerRiding(player)) {
                stopDriving();
                activeTrain = new TrainVirtualDrive(train);
                activeTrain.railProgress = train.getRailProgress();
                activeTrain.speed = train.getSpeed();
                activeTrain.vehicleRidingClient.startRiding(
                        player.getUUID(),
                        train.vehicleRidingClient.getPercentageX(player.getUUID()),
                        train.vehicleRidingClient.getPercentageZ(player.getUUID())
                );
                PacketVirtualDrive.sendVirtualDriveC2S(true);
                train.vehicleRidingClient.stopRiding(player.getUUID());
                RenderTrains.scheduleBeforeNextSimulation(() -> {
                    MTR.LOGGER.info("ADD");
                    ClientData.TRAINS.add(activeTrain);
                });
                return true;
            }
        }
        return false;
    }

    public static void stopDriving() {
        if (activeTrain != null) {
            PacketVirtualDrive.sendVirtualDriveC2S(false);
            activeTrain.isRemoved = true;

            // This can be called in simulateTrain, which is on the same thread as Render Thread, we must explicitly defer it to the next simulation tick.
            RenderTrains.scheduleBeforeNextSimulation(() -> {
                ClientData.TRAINS.remove(activeTrain);
                activeTrain = null;
            });
        }
    }

    public float getPercentNotch() {
        if (vdNotch < 0) {
            return (float) vdNotch / brakeNotches;
        } else {
            return (float) vdNotch / powerNotches;
        }
    }

    @Override
    public int getTotalDwellTicks() {
        return 114514;
    }

    @Override
    public boolean toggleDoors() {
        if (speed == 0) {
            if (doorTarget) {
                if (doorValue >= 1) {
                    doorTarget = false;
                }
            } else {
                doorTarget = true;
            }
            return true;
        } else {
            doorTarget = false;
            return false;
        }
    }
}
