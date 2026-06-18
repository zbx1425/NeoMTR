package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.BlockClock;
import mtr.block.IBlock;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.UtilitiesClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderClock extends BlockEntityRendererMapper<BlockClock.TileEntityClock, RenderClock.ClockRenderState> implements IGui, IBlock {

	private static final Identifier CLOCK_HAND_TEXTURE = Identifier.parse("mtr:textures/block/white.png");

	public RenderClock(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void submit(ClockRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.3125, 0.5);
		if (state.rotated) {
			UtilitiesClient.rotateYDegrees(poseStack, 90);
		}

		final long time = state.time + 6000;
		final RenderType clockRenderType = MoreRenderLayers.getLight(CLOCK_HAND_TEXTURE, false);

		drawHand(poseStack, clockRenderType, submitNodeCollector, time * 360F / 12000, true);
		drawHand(poseStack, clockRenderType, submitNodeCollector, time * 360F / 1000, false);

		UtilitiesClient.rotateYDegrees(poseStack, 180);
		drawHand(poseStack, clockRenderType, submitNodeCollector, time * 360F / 12000, true);
		drawHand(poseStack, clockRenderType, submitNodeCollector, time * 360F / 1000, false);

		poseStack.popPose();
	}

	@Override
	public ClockRenderState createRenderState() {
		return new ClockRenderState();
	}

	@Override
	public void extractRenderState(BlockClock.TileEntityClock blockEntity, ClockRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.rotated = IBlock.getStatePropertySafe(blockEntity.getBlockState(), BlockClock.FACING);
		state.time = blockEntity.getLevel() == null ? 0 : blockEntity.getLevel().getOverworldClockTime();
	}

	private static void drawHand(PoseStack matrices, RenderType renderType, SubmitNodeCollector submitNodeCollector, float rotation, boolean isHourHand) {
		matrices.pushPose();
		UtilitiesClient.rotateZDegrees(matrices, -rotation);
		submitNodeCollector.submitCustomGeometry(matrices, renderType, (pose, vertexConsumer) -> {
			IDrawing.drawTexture(pose, vertexConsumer, -0.01F, isHourHand ? 0.15F : 0.24F, isHourHand ? 0.1F : 0.105F, 0.01F, -0.03F, isHourHand ? 0.1F : 0.105F, Direction.UP, ARGB_LIGHT_GRAY, MAX_LIGHT_INTERIOR);
		});
		matrices.popPose();
	}

	public static class ClockRenderState extends BlockEntityRenderState {
		boolean rotated;
		long time;
	}
}
