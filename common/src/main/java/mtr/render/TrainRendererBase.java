package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.MTRClient;
import mtr.client.Config;
import mtr.client.TrainClientRegistry;
import mtr.client.TrainProperties;
import mtr.client.VehiclePlayerMovementTracker;
import mtr.data.RailwayData;
import mtr.data.TrainClient;
import mtr.util.UtilitiesClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public abstract class TrainRendererBase {

	protected static Camera camera;
	protected static Level world;
	protected static float lastFrameDuration;
	protected static float lastFramePartialTick;
	protected static PoseStack matrices;
	protected static MultiBufferSource vertexConsumers;

	/* 26.1 Custom submission system */
	private static final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
	private static final CameraRenderState cameraRenderState = new CameraRenderState();
	protected static boolean isTranslucentBatch;

	private static EntityRenderDispatcher entityRenderDispatcher;
	private static LocalPlayer clientPlayer;

	public abstract TrainRendererBase createTrainInstance(TrainClient train);

	public abstract void renderCar(int carIndex, double x, double y, double z, float yaw, float pitch, float roll, boolean doorLeftOpen, boolean doorRightOpen);

	public abstract void renderConnection(Vec3 prevPos1, Vec3 prevPos2, Vec3 prevPos3, Vec3 prevPos4, Vec3 thisPos1, Vec3 thisPos2, Vec3 thisPos3, Vec3 thisPos4, double x, double y, double z, float yaw, float pitch, float roll);

	public abstract void renderBarrier(Vec3 prevPos1, Vec3 prevPos2, Vec3 prevPos3, Vec3 prevPos4, Vec3 thisPos1, Vec3 thisPos2, Vec3 thisPos3, Vec3 thisPos4, double x, double y, double z, float yaw, float pitch, float roll);

	public static void renderRidingPlayer(UUID playerId, Vec3 playerPositionOffset) {
		final BlockPos posAverage = applyAverageTransform(playerPositionOffset.x, playerPositionOffset.y, playerPositionOffset.z);
		if (posAverage == null) {
			return;
		}

		final Player renderPlayer = world.getPlayerByUUID(playerId);
		if (renderPlayer != null && (!playerId.equals(clientPlayer.getUUID()) || camera.isDetached())) {
			float speed = (float)Math.min(VehiclePlayerMovementTracker.getDeltaMovementLastTick(playerId).length() * 4f, 1.0F);
			// TODO: Walk animation
			//			if(VehiclePlayerMovementTracker.oneTickElapsed()) renderPlayer.walkAnimation.update(speed, 0.6F, renderPlayer.isBaby() ? 3.0F : 0.75F);

			renderPlayer.walkAnimation.stop();
			if(Minecraft.getInstance().isPaused()) {
			}

			AvatarRenderState playerRenderState = (AvatarRenderState)entityRenderDispatcher.extractEntity(renderPlayer, lastFramePartialTick);

			// HACK: The renderer is invoked in RenderLevelStageEvent, which is beyond the extraction phase (for now, too much work to move)
			// So we create our own submission system for entityRenderDispatcher, then immediately invoke it
			Minecraft.getInstance().gameRenderer.getMainCamera().extractRenderState(cameraRenderState, lastFramePartialTick);

			entityRenderDispatcher.submit(playerRenderState, cameraRenderState, playerPositionOffset.x, playerPositionOffset.y-RenderTrains.PLAYER_RENDER_OFFSET, playerPositionOffset.z, matrices, submitNodeStorage);

			ModelFeatureRenderer modelFeatureRenderer = new ModelFeatureRenderer();
			ModelPartFeatureRenderer modelPartFeatureRenderer = new ModelPartFeatureRenderer();
			ItemFeatureRenderer itemRenderer = new ItemFeatureRenderer();
			NameTagFeatureRenderer nameTagFeatureRenderer = new NameTagFeatureRenderer();
			submitNodeStorage.getSubmitsPerOrder().forEach((i, collection) -> {
				modelFeatureRenderer.renderSolid(collection, Minecraft.getInstance().renderBuffers().bufferSource(), Minecraft.getInstance().renderBuffers().outlineBufferSource(), Minecraft.getInstance().renderBuffers().crumblingBufferSource());
				modelFeatureRenderer.renderTranslucent(collection, Minecraft.getInstance().renderBuffers().bufferSource(), Minecraft.getInstance().renderBuffers().outlineBufferSource(), Minecraft.getInstance().renderBuffers().crumblingBufferSource());
				modelPartFeatureRenderer.renderSolid(collection, Minecraft.getInstance().renderBuffers().bufferSource(), Minecraft.getInstance().renderBuffers().outlineBufferSource(), Minecraft.getInstance().renderBuffers().crumblingBufferSource());
				modelPartFeatureRenderer.renderTranslucent(collection, Minecraft.getInstance().renderBuffers().bufferSource(), Minecraft.getInstance().renderBuffers().outlineBufferSource(), Minecraft.getInstance().renderBuffers().crumblingBufferSource());
				itemRenderer.renderSolid(collection, Minecraft.getInstance().renderBuffers().bufferSource(), Minecraft.getInstance().renderBuffers().outlineBufferSource());
				itemRenderer.renderTranslucent(collection, Minecraft.getInstance().renderBuffers().bufferSource(), Minecraft.getInstance().renderBuffers().outlineBufferSource());
				nameTagFeatureRenderer.renderTranslucent(collection, Minecraft.getInstance().renderBuffers().bufferSource(), Minecraft.getInstance().font);
			});
			submitNodeStorage.endFrame();
			submitNodeStorage.clear();
		}
		matrices.popPose();
	}

	public static void setupStaticInfo(PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
		final Minecraft client = Minecraft.getInstance();
		camera = client.gameRenderer.getMainCamera();
		entityRenderDispatcher = client.getEntityRenderDispatcher();
		world = client.level;
		clientPlayer = client.player;
		lastFrameDuration = MTRClient.getLastFrameDuration();
		lastFramePartialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		if(Minecraft.getInstance().isPaused()) lastFramePartialTick = 0;
		TrainRendererBase.matrices = matrices;
		TrainRendererBase.vertexConsumers = vertexConsumers;
	}

	public static void setBatch(boolean isTranslucentBatch) {
		TrainRendererBase.isTranslucentBatch = isTranslucentBatch;
	}

	public static BlockPos applyAverageTransform(double x, double y, double z) {
		final BlockPos posAverage = RailwayData.newBlockPos(x, y, z);
		if (RenderTrains.shouldNotRender(posAverage, UtilitiesClient.getRenderDistance() * (Config.trainRenderDistanceRatio() + 1), null)) {
			return null;
		}
		matrices.pushPose();
		return posAverage;
	}

	public static void applyTransform(TrainClient train, double x, double y, double z, float yaw, float pitch, float roll, boolean isBbModel) {
		final TrainProperties trainProperties = TrainClientRegistry.getTrainProperties(train.trainId);
		final boolean hasPitch = pitch < 0 ? train.transportMode.hasPitchAscending : train.transportMode.hasPitchDescending;
		matrices.translate(x, y, z);
		matrices.translate(0, trainProperties.railSurfaceOffset, 0);
		UtilitiesClient.rotateY(matrices, (float) Math.PI + yaw);
		UtilitiesClient.rotateX(matrices, (hasPitch ? pitch : 0));
		UtilitiesClient.rotateZ(matrices, roll);
		matrices.translate(0, -trainProperties.railSurfaceOffset, 0);
		if (isBbModel) UtilitiesClient.rotateX(matrices, (float) Math.PI);
	}
}
