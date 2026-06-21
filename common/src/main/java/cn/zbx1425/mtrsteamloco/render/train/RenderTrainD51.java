package cn.zbx1425.mtrsteamloco.render.train;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.render.RenderUtil;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.PoseStackUtil;
import cn.zbx1425.sowcerext.multipart.MultipartContainer;
import cn.zbx1425.sowcerext.multipart.MultipartUpdateProp;
import cn.zbx1425.sowcerext.multipart.animated.AnimatedLoader;
import cn.zbx1425.sowcer.math.Vector3f;
import mtr.MTRClient;
import mtr.data.TrainClient;
import mtr.render.TrainRendererBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;

public class RenderTrainD51 extends TrainRendererBase {

    protected static MultipartContainer modelD51;

    private final TrainClient train;
    private final MultipartUpdateProp updateProp = new MultipartUpdateProp();

    private final TrainRendererBase trailingCarRenderer;

    private static int renderingCarNum = 0;

    private float demoSpeed = -1;

    public static void initGLModel(ResourceManager resourceManager) {
        try {
            MainClient.atlasManager.load(resourceManager, Main.id("models/atlas/d51.json"));
            modelD51 = AnimatedLoader.loadModel(resourceManager, MainClient.modelManager, MainClient.atlasManager,
                    Main.id("models/d51/d51.animated"));
        } catch (IOException e) {
            modelD51 = null;
            Main.LOGGER.error("Failed loading model for D51:", e);
        }
    }

    public RenderTrainD51(TrainRendererBase trailingCarRenderer) {
        this.train = null;
        this.trailingCarRenderer = trailingCarRenderer;
    }

    public RenderTrainD51(TrainRendererBase trailingCarRenderer, float demoSpeed) {
        this.train = null;
        this.trailingCarRenderer = trailingCarRenderer;
        this.demoSpeed = demoSpeed;
    }

    private RenderTrainD51(TrainClient trainClient, RenderTrainD51 source) {
        this.train = trainClient;
        if (source.trailingCarRenderer == null) {
            this.trailingCarRenderer = null;
        } else {
            this.trailingCarRenderer = source.trailingCarRenderer.createTrainInstance(this.train);
        }
        this.demoSpeed = source.demoSpeed;
    }

    @Override
    public TrainRendererBase createTrainInstance(TrainClient trainClient) {
        return new RenderTrainD51(trainClient, this);
    }

    @Override
    public void renderCar(int carIndex, double x, double y, double z, float yaw, float pitch, float roll, boolean doorLeftOpen, boolean doorRightOpen) {
        if (RenderUtil.shouldSkipRenderTrain(train)) return;

        int carNum = !train.isReversed() ? carIndex : (train.trainCars - carIndex - 1);
        renderingCarNum = carNum;

        if (trailingCarRenderer != null && carNum != 0) {
            matrices.pushPose();
            int carNumToRender;
            if (trailingCarRenderer instanceof RenderTrainDK3) {
                if (carNum == 1) {
                    carNumToRender = (train.trainCars < 3) ? 1 : 0;
                } else if (carNum == 2) {
                    carNumToRender = train.trainCars - 1;
                    // matrices.translate(0, 0, train.isReversed() ? 1 : 0);
                } else {
                    matrices.popPose();
                    return;
                }
            } else {
                carNumToRender = (carNum == train.trainCars - 1) ? carNum: carNum - 1; // Make sure we always get a proper tail
            }

            int carIndexToRender = !train.isReversed() ? carNumToRender : (train.trainCars - carNumToRender - 1);
            trailingCarRenderer.renderCar(carIndexToRender, x, y, z, yaw, pitch, roll, doorLeftOpen, doorRightOpen);
            matrices.popPose();
            return;
        }

        if (isTranslucentBatch) {
            return;
        }

        final BlockPos posAverage = applyAverageTransform(x, y, z);
        if (posAverage == null) {
            return;
        }

        matrices.pushPose();
        applyTransform(train, x, y - 1, z, yaw, pitch, roll, false);

        if (train.isReversed()) {
            PoseStackUtil.rotY(matrices, (float) Math.PI);
        }

        final int light = LightCoordsUtil.pack(world.getBrightness(LightLayer.BLOCK, posAverage), world.getBrightness(LightLayer.SKY, posAverage));

        updateProp.update(train, carIndex, !train.isReversed());
        if (demoSpeed >= 0) {
            updateProp.speed = demoSpeed * 20F;
        }

        Matrix4f pose = new Matrix4f(matrices.last().pose());
        modelD51.updateAndEnqueueAll(MainClient.drawScheduler, updateProp, pose, light);

        if (ClientConfig.enableSmoke && (train.getIsOnRoute() || demoSpeed >= 0) && (int)MTRClient.getGameTick() % 4 == 0) {
            Vector3f smokeOrigin = new Vector3f(0, 2.7f, 8.4f);
            Vector3f carPos = new Vector3f((float)x, (float)y, (float)z);

            smokeOrigin.rotX(pitch);
            smokeOrigin.rotY((!train.isReversed() ? (float) Math.PI : 0) + yaw);
            smokeOrigin.add(carPos);
            world.addParticle(Main.PARTICLE_STEAM_SMOKE, smokeOrigin.x(), smokeOrigin.y(), smokeOrigin.z(), 0.0, 0.7f, 0.0);
        }

        matrices.popPose();
        matrices.popPose();
    }

    @Override
    public void renderConnection(Vec3 prevPos1, Vec3 prevPos2, Vec3 prevPos3, Vec3 prevPos4, Vec3 thisPos1, Vec3 thisPos2, Vec3 thisPos3, Vec3 thisPos4, double x, double y, double z, float yaw, float pitch, float roll) {
        if (trailingCarRenderer != null && renderingCarNum > 1) {
            trailingCarRenderer.renderConnection(prevPos1, prevPos2, prevPos3, prevPos4, thisPos1, thisPos2, thisPos3, thisPos4, x, y, z, yaw, pitch, roll);
        }
    }

    @Override
    public void renderBarrier(Vec3 prevPos1, Vec3 prevPos2, Vec3 prevPos3, Vec3 prevPos4, Vec3 thisPos1, Vec3 thisPos2, Vec3 thisPos3, Vec3 thisPos4, double x, double y, double z, float yaw, float pitch, float roll) {
        if (trailingCarRenderer != null && renderingCarNum > 1) {
            trailingCarRenderer.renderBarrier(prevPos1, prevPos2, prevPos3, prevPos4, thisPos1, thisPos2, thisPos3, thisPos4, x, y, z, yaw, pitch, roll);
        }
    }

}
