package cn.zbx1425.mtrsteamloco.gui;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.game.TrainVirtualDrive;
import com.mojang.blaze3d.vertex.*;
import mtr.KeyMappings;
import mtr.MTRClient;
import mtr.mappings.Text;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.joml.Random;

public class VirtualDriveOverlay {

    private static float keyCooldown = 0;
    private static KeyMapping lastKey = null;

    private static int lastTargetState;
    private static double atpBuzzerTriggerTime = 0;
    private static final SoundEvent ATP_BUZZER_SOUND = SoundEvent.createVariableRangeEvent(Main.id("drive.atp_buzzer"));

    private static float delayedTrainSpeed, delayedAtpYellowSpeed, delayedAtpRedSpeed;
    private static float speedUpdateCooldown = 0;
    private static final Random random = new Random();

    public static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaT) {
        if (TrainVirtualDrive.activeTrain == null) return;
        TrainVirtualDrive train = TrainVirtualDrive.activeTrain;

        final int PADDING = 24;
        final int GAUGE_SIZE = 96;
        Font font = Minecraft.getInstance().font;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(0, guiGraphics.guiHeight());

        final LocalPlayer player = Minecraft.getInstance().player;
        final int currentRidingCar = Mth.clamp(
                (int) Math.floor(train.vehicleRidingClient.getPercentageZ(player.getUUID())),
                0, train.trainCars - 1);
        if (currentRidingCar != (train.isReversed() ? train.trainCars - 1 : 0)) {
            guiGraphics.text(font, Text.translatable("gui.mtrsteamloco.drive.not_in_cab"), PADDING, -PADDING - 10, 0xFFFFA500);
            guiGraphics.pose().popMatrix();
            return;
        }

        anyKeyDown = false;
        if (isKeyDownWithRepeat(KeyMappings.TRAIN_BRAKE, deltaT)) {
            train.vdNotch = Math.max(train.vdNotch - 1, -train.brakeNotches);
        } else if (isKeyDownWithRepeat(KeyMappings.TRAIN_NEUTRAL, deltaT)) {
            train.vdNotch -= (int) Math.signum(train.vdNotch);
        } else if (isKeyDownWithRepeat(KeyMappings.TRAIN_ACCELERATE, deltaT)) {
            train.vdNotch = Math.min(train.vdNotch + 1, train.powerNotches);
        }
        if (!anyKeyDown) {
            lastKey = null;
            keyCooldown = 0;
        }

        double platformDistance = train.nextPlatformRailProgress - train.getRailProgress();
        if (KeyMappings.TRAIN_TOGGLE_DOORS.consumeClick()) {
            if (train.getDoorValue() > 0 || Math.abs(platformDistance) < 1) {
                train.toggleDoors();
            }
        }

        speedUpdateCooldown -= MTRClient.getLastFrameDuration();
        if (speedUpdateCooldown <= 0) {
            delayedTrainSpeed = train.getSpeed();
            delayedAtpYellowSpeed = train.atpYellowSpeed;
            delayedAtpRedSpeed = train.atpRedSpeed;
            speedUpdateCooldown = (random.nextInt(3) + 2);
        }

        // HMI painting
        Identifier hmiTex = Main.id("textures/gui/drive_hmi.png");

        // Gauge back
        blit(hmiTex, guiGraphics,
                PADDING + 1, -GAUGE_SIZE - PADDING + 1,
                GAUGE_SIZE / 4, GAUGE_SIZE,
                0.125f, 0.5f, 0.125f, 0.5f, 0x88222222);
        blit(hmiTex, guiGraphics,
                PADDING, -GAUGE_SIZE - PADDING,
                GAUGE_SIZE / 4, GAUGE_SIZE,
                0.125f, 0.5f, 0.125f, 0.5f, 0xFFFFFFFF);
        blit(hmiTex, guiGraphics,
                PADDING + GAUGE_SIZE / 4, -GAUGE_SIZE - PADDING,
                GAUGE_SIZE, GAUGE_SIZE,
                0f, 0f, 0.5f, 0.5f, 0xFF222222);
        blit(hmiTex, guiGraphics,
                PADDING + GAUGE_SIZE / 4, -GAUGE_SIZE - PADDING,
                GAUGE_SIZE, GAUGE_SIZE,
                0.5f, 0f, 0.5f, 0.5f, 0xffffffff);

        final int GAUGE_MAX_SPEED = 100;
        // Speed needle
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().rotateAbout(
                (float)Math.toRadians(-140 + Mth.clamp(Math.round(delayedTrainSpeed * 3.6f * 20 * 4) / 4f, 0, GAUGE_MAX_SPEED) / GAUGE_MAX_SPEED * 280),
                PADDING + GAUGE_SIZE / 4f + GAUGE_SIZE / 2f, -GAUGE_SIZE / 2f - PADDING
        );
        int needleXOff = PADDING + GAUGE_SIZE / 4 + (GAUGE_SIZE * 3 / 8);
        blit(hmiTex, guiGraphics,
                needleXOff, -GAUGE_SIZE - PADDING,
                GAUGE_SIZE / 4, GAUGE_SIZE,
                0f, 0.5f, 0.125f, 0.5f, 0xffffffff);
        guiGraphics.pose().popMatrix();
        if (!train.atpCutout) {
            // Yellow ATP Speed needle
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().rotateAbout(
                    (float)Math.toRadians(-140 + Mth.clamp(Math.round(train.atpYellowSpeed * 3.6f * 20 * 3) / 3f, 0, GAUGE_MAX_SPEED) / GAUGE_MAX_SPEED * 280),
                    PADDING + GAUGE_SIZE / 4f + GAUGE_SIZE / 2f, -GAUGE_SIZE / 2f - PADDING
            );
            blit(hmiTex, guiGraphics,
                    needleXOff, -GAUGE_SIZE - PADDING,
                    GAUGE_SIZE / 4, GAUGE_SIZE,
                    0.375f, 0.5f, 0.125f, 0.5f, 0xffffffff);
            guiGraphics.pose().popMatrix();
            // Red ATP Speed needle
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().rotateAbout(
                    (float)Math.toRadians(-140 + Mth.clamp(Math.round(train.atpRedSpeed * 3.6f * 20 * 3) / 3f, 0, GAUGE_MAX_SPEED) / GAUGE_MAX_SPEED * 280),
                    PADDING + GAUGE_SIZE / 4f + GAUGE_SIZE / 2f, -GAUGE_SIZE / 2f - PADDING
            );
            blit(hmiTex, guiGraphics,
                    needleXOff, -GAUGE_SIZE - PADDING,
                    GAUGE_SIZE / 4, GAUGE_SIZE,
                    0.25f, 0.5f, 0.125f, 0.5f, 0xffffffff);
            guiGraphics.pose().popMatrix();
        }

        // Info icons
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(PADDING + GAUGE_SIZE / 4f + GAUGE_SIZE + 3, -GAUGE_SIZE - PADDING);
        float infoIconScale = (GAUGE_SIZE / 4f) / 64;
        guiGraphics.pose().scale(infoIconScale, infoIconScale);
        // Stop accuracy
        if (platformDistance < train.spacing * train.trainCars + 10) {
            blit(hmiTex, guiGraphics, 2, 2, 64, 64, 0.5f, 0.5f, 0.125f, 0.125f, 0x88222222);
            if (Math.abs(platformDistance) < 1) {
               blit(hmiTex, guiGraphics, 0, 0, 64, 64, 0.5f, 0.5f, 0.125f, 0.125f, 0xffffffff);
           } else {
               blit(hmiTex, guiGraphics, 0, 0, 64, 64, 0.625f, 0.5f, 0.125f, 0.125f, 0xffffffff);
           }
        }
        // Emergency states
        if (train.atpEmergencyBrake) {
            blit(hmiTex, guiGraphics, 2, 64 + 2, 64, 64, 0.625f, 0.625f, 0.125f, 0.125f, 0x88222222);
            blit(hmiTex, guiGraphics, 0, 64, 64, 64, 0.625f, 0.625f, 0.125f, 0.125f, 0xffffffff);
        } else if (train.getDoorValue() > 0) {
            blit(hmiTex, guiGraphics, 2, 64 + 2, 64, 64, 0.5f, 0.625f, 0.125f, 0.125f, 0x88222222);
            blit(hmiTex, guiGraphics, 0, 64, 64, 64, 0.5f, 0.625f, 0.125f, 0.125f, 0xffffffff);
        }
        guiGraphics.pose().popMatrix();

        // Speed Text
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(PADDING + GAUGE_SIZE / 4f + GAUGE_SIZE / 2f, -GAUGE_SIZE / 2f - PADDING);
        float speedTextScale = (50 / 730f * GAUGE_SIZE) / font.lineHeight;
        guiGraphics.pose().scale(speedTextScale, speedTextScale);
        guiGraphics.pose().translate(0, 0.5f);
        int speedKph = (int)Math.ceil(train.getSpeed() * 20 * 3.6F);
        if (speedKph >= 100) {
            guiGraphics.centeredText(font, Integer.toString(speedKph), 0, -font.lineHeight / 2, 0xFFFFFFFF);
        } else if (speedKph >= 10) {
            guiGraphics.text(font, Integer.toString(speedKph % 10), 0, -font.lineHeight / 2, 0xFFFFFFFF);
            guiGraphics.text(font, Integer.toString(speedKph / 10), -font.width(Integer.toString(speedKph / 10)), -font.lineHeight / 2, 0xFFFFFFFF);
        } else {
            guiGraphics.text(font, Integer.toString(speedKph), 0, -font.lineHeight / 2, 0xFFFFFFFF);
        }
        guiGraphics.pose().popMatrix();

        if (train.atpTargetSpeed >= 0 && !train.atpCutout) {
            // Target speed text
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(PADDING + (32 / 256f) * GAUGE_SIZE,
                    -GAUGE_SIZE - PADDING + (44 / 256f) * GAUGE_SIZE);
            float targetSpeedTextScale = (13 / 256f * GAUGE_SIZE) / font.lineHeight;
            guiGraphics.pose().scale(targetSpeedTextScale, targetSpeedTextScale);
            int targetSpeedKph = Math.round(train.atpTargetSpeed * 20 * 3.6F);
            guiGraphics.text(font, Integer.toString(targetSpeedKph), -font.width(Integer.toString(targetSpeedKph)), 0, 0xFFFFFFFF);
            guiGraphics.pose().popMatrix();

            // Target speed bar
            double targetDistance = Mth.clamp(train.atpTargetDistance - train.getRailProgress(), 1, 750);
            float targetBarHeight = (float) (Math.log10(targetDistance) * 40 / 256f * GAUGE_SIZE);
            if (targetDistance > 1) {
                float x1 = PADDING + (36 / 256f) * GAUGE_SIZE + (8 / 350f) * GAUGE_SIZE;
                float x2 = x1 + (10 / 350f) * GAUGE_SIZE;
                float y2 = -GAUGE_SIZE - PADDING + (184 / 256f) * GAUGE_SIZE;
                float y1 = y2 - targetBarHeight;
                int targetColor = 0xFF008000;
                if (targetDistance < 150) {
                    if (targetSpeedKph == 0) {
                        targetColor = 0xFFFF0000;
                    } else if (targetSpeedKph < 60) {
                        targetColor = 0xFFFFA500;
                    }
                } else if (targetDistance < 300) {
                    if (targetSpeedKph < 25) {
                        targetColor = 0xFFFFA500;
                    }
                }
                fill(guiGraphics, x1 + 1,  y1 + 1,  x2 + 1,  y2 + 1, 0x88222222);
                fill(guiGraphics, x1, y1, x2, y2, targetColor);
            }
        }

        // Target status
        if (!train.atpCutout) {
            float x1 = PADDING;
            float x2 = x1 + (40 / 256f) * GAUGE_SIZE;
            float y1 = -GAUGE_SIZE - PADDING - (10 / 256f) * GAUGE_SIZE;
            float y2 = y1 + (40 / 256f) * GAUGE_SIZE;
            int targetState = train.atpEmergencyBrake ? 2
                    : (train.getSpeed() > train.atpYellowSpeed + (0.1f / 20 / 3.6f) ? 1 : 0);
            if (targetState > 0) {
                guiGraphics.fill((int) x1 + 1, (int) y1 + 1, (int) x2 + 1, (int) y2 + 1, 0x88222222);
                guiGraphics.fill((int) x1, (int) y1, (int) x2, (int) y2, targetState == 2 ? 0xFFFF0000 : 0xFFFFA500);
            }
            if (targetState != lastTargetState) {
                if (targetState > lastTargetState) {
                    if (MTRClient.getGameTick() - atpBuzzerTriggerTime > 20) {
                        atpBuzzerTriggerTime = MTRClient.getGameTick();
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ATP_BUZZER_SOUND, 1.0F));
                    }
                }
                lastTargetState = targetState;
            }
        }

        // Current notch
        String reverserText = train.vdReverser == 0 ? "N"
                : (train.vdReverser < 0 ? "R" : "F");
        int reverserColor = train.vdReverser == 0 ? 0xFF008844
                : (train.vdReverser < 0 ? 0xFFFFA500 : 0xFF888888);
        String notchText = train.vdNotch == 0 ? "N"
                : (train.vdNotch < 0
                    ? "B" + Math.round(-train.getPercentNotch() * 100)
                    : "P" + Math.round(train.getPercentNotch() * 100));
        int notchColor = train.vdNotch == 0 ? 0xFF888888
                : (train.vdNotch < 0 ? 0xFFFFA500 : 0xFF4287F5);
        guiGraphics.text(font, Component.literal(reverserText), PADDING, -PADDING - 10, reverserColor);
        guiGraphics.text(font, Component.literal(notchText), PADDING + 12, -PADDING - 10, notchColor);

        // Various other info
        int lineHeight = 10;
        int y = -PADDING - GAUGE_SIZE - 10 - lineHeight;
        int x = 20;
        // Stop accuracy
        if (Math.abs(platformDistance) < 10 && train.getSpeed() <= 0) {
            String distanceText = (platformDistance > -5 && platformDistance < 5)
                    ? Math.round(platformDistance * 100) + " cm"
                    : Math.round(platformDistance) + " m";
            guiGraphics.text(font, Text.translatable("gui.mtrsteamloco.drive.stop_position", distanceText),
                    x, y, Math.abs(platformDistance) < 1 ? 0xFF1CED85 : 0xFFFFA500);
            y -= lineHeight;
        }
        // ATP status
        if (train.atpEmergencyBrake) {
            guiGraphics.text(font, Component.translatable("gui.mtrsteamloco.drive.atp_eb"), x, y, 0xFFFF0000);
            y -= lineHeight;
        }

        guiGraphics.pose().popMatrix();
    }

    private static void blit(Identifier id, GuiGraphicsExtractor guiGraphics, int x1, int y1, int width, int height, float minU, float minV, float deltaU, float deltaV, int color) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, id, x1, y1, (int)(minU*512), (int)(minV*512), width, height, (int)(deltaU*512), (int)(deltaV*512), 512, 512, color);
    }

    private static void fill(GuiGraphicsExtractor guiGraphics, float minX, float minY, float maxX, float maxY, int color) {
        guiGraphics.fill((int)minX, (int)minY, (int)maxX, (int)maxY, color);
    }

    private static final float KEY_DELAY_BEFORE_REPEAT = 6f;
    private static final float KEY_REPEAT_INTERVAL = 3f;
    private static double lastProcessedTick;
    private static boolean anyKeyDown;

    private static boolean isKeyDownWithRepeat(KeyMapping key, DeltaTracker deltaT) {
        if (key.isDown()) {
            anyKeyDown = true;
            if (lastKey != key) {
                lastKey = key;
                keyCooldown = KEY_DELAY_BEFORE_REPEAT;
                return true;
            }
            if (MTRClient.getGameTick() != lastProcessedTick) {
                keyCooldown -= deltaT.getGameTimeDeltaPartialTick(false);
                lastProcessedTick = MTRClient.getGameTick();
            }
            if (keyCooldown <= 0) {
                keyCooldown = KEY_REPEAT_INTERVAL;
                return true;
            }
        }
        return false;
    }
}
