package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.MTRClient;
import mtr.client.Config;
import mtr.client.TrainClientRegistry;
import mtr.client.TrainProperties;
import mtr.data.RailwayData;
import mtr.data.TrainClient;
import mtr.mappings.Utilities;
import mtr.mappings.UtilitiesClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public abstract class TrainRendererBase {

	protected static Camera camera;
	protected static Level world;
	protected static float lastFrameDuration;
	protected static PoseStack matrices;
	protected static MultiBufferSource vertexConsumers;

	protected static boolean isTranslucentBatch;

	private static EntityRenderDispatcher entityRenderDispatcher;
	private static LocalPlayer player;

	public abstract TrainRendererBase createTrainInstance(TrainClient train);

	public abstract void renderCar(int carIndex, double x, double y, double z, float yaw, float pitch, float roll, boolean doorLeftOpen, boolean doorRightOpen);

	public abstract void renderConnection(Vec3 prevPos1, Vec3 prevPos2, Vec3 prevPos3, Vec3 prevPos4, Vec3 thisPos1, Vec3 thisPos2, Vec3 thisPos3, Vec3 thisPos4, double x, double y, double z, float yaw, float pitch, float roll);

	public abstract void renderBarrier(Vec3 prevPos1, Vec3 prevPos2, Vec3 prevPos3, Vec3 prevPos4, Vec3 thisPos1, Vec3 thisPos2, Vec3 thisPos3, Vec3 thisPos4, double x, double y, double z, float yaw, float pitch, float roll);

	public static void renderRidingPlayer(UUID playerId, Vec3 playerPositionOffset) {
		final BlockPos posAverage = applyAverageTransform(playerPositionOffset.x, playerPositionOffset.y, playerPositionOffset.z);
		if (posAverage == null) {
			return;
		}
		matrices.translate(0, RenderTrains.PLAYER_RENDER_OFFSET, 0);
		final Player renderPlayer = world.getPlayerByUUID(playerId);
		if (renderPlayer != null && (!playerId.equals(player.getUUID()) || camera.isDetached())) {
			// TODO: Render the player
			EntityRenderState playerRenderState = entityRenderDispatcher.extractEntity(renderPlayer, 0);
			// Maybe this can stop the player from appearing moving and cape from flapping
//			playerRenderState.walkDistO = renderPlayer.walkDist;
//			playerRenderState.xCloak = renderPlayer.xCloakO = renderPlayer.xo;
//			playerRenderState.yCloak = renderPlayer.yCloakO = renderPlayer.yo;
//			playerRenderState.zCloak = renderPlayer.zCloakO = renderPlayer.zo;

			renderPlayer.walkAnimation.setSpeed(0);

//			SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
//
//			CameraRenderState cameraRenderState = new CameraRenderState();
//			Minecraft.getInstance().gameRenderer.getMainCamera().extractRenderState(cameraRenderState, 0);
//
//			entityRenderDispatcher.submit(playerRenderState, cameraRenderState, playerPositionOffset.x, playerPositionOffset.y, playerPositionOffset.z, matrices, submitNodeStorage);
//
//			ModelFeatureRenderer modelFeatureRenderer = new ModelFeatureRenderer();
//			submitNodeStorage.getSubmitsPerOrder().forEach((order, submitNodeCollection) -> {
//				modelFeatureRenderer.renderSolid(submitNodeCollection);
//				submitNodeCollection.getModelPartSubmits()
//			});


		}
		matrices.popPose();
	}

	public static void setupStaticInfo(PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
		final Minecraft client = Minecraft.getInstance();
		camera = client.gameRenderer.getMainCamera();
		entityRenderDispatcher = client.getEntityRenderDispatcher();
		world = client.level;
		player = client.player;
		lastFrameDuration = MTRClient.getLastFrameDuration();
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
