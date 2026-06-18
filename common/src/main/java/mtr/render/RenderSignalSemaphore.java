package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.BlockSignalSemaphoreBase;
import mtr.client.IDrawing;
import mtr.mappings.UtilitiesClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderSignalSemaphore<T extends BlockSignalSemaphoreBase.TileEntitySignalSemaphoreBase> extends RenderSignalBase<T, RenderSignalSemaphore.SignalSemaphoreRenderState> {

	private static final int ANGLE = 55;
	private static final int SPEED = 4;

	public RenderSignalSemaphore(BlockEntityRenderDispatcher dispatcher, boolean isSingleSided) {
		super(dispatcher, isSingleSided, 2);
	}

	@Override
	public SignalSemaphoreRenderState createRenderState() {
		return new SignalSemaphoreRenderState();
	}

	@Override
	public void extractRenderState(T blockEntity, SignalSemaphoreRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		for(int i = 0; i < 2; i++) {
			final boolean isBackSide = i == 1;
			final float aspect = isBackSide ? state.occupiedAspectOpposite : state.occupiedAspect;
			final float angle = isBackSide ? state.angle2 : state.angle1;
			final float newAngle;
			if (aspect > 0) {
				newAngle = Math.max(0, angle - SPEED * partialTicks);
			} else {
				newAngle = Math.min(ANGLE, angle + SPEED * partialTicks);
			}
			if (isBackSide) {
				state.angle2 = newAngle;
			} else {
				state.angle1 = newAngle;
			}
		}
	}

	@Override
	protected void drawSignal(SignalSemaphoreRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, RenderType renderType, Direction facing, int occupiedAspect, boolean isBackSide) {
		final float angle = isBackSide ? state.angle2 : state.angle1;
		submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
			IDrawing.drawTexture(poseStack.last(), vertexConsumer, -0.0625F, 0.296875F, -0.190625F, 0.0625F, 0.453125F, -0.190625F, facing.getOpposite(), angle < ANGLE / 2F ? 0xFFFF0000 : 0xFF00FF00, MAX_LIGHT_GLOWING);
		});
		poseStack.translate(0.1875, 0.375, 0);
		UtilitiesClient.rotateZDegrees(poseStack, -180 - angle);

		submitNodeCollector.submitCustomGeometry(poseStack, MoreRenderLayers.getExterior(Identifier.parse("mtr:textures/block/semaphore.png")), (pose, vertexConsumer) -> {
			IDrawing.drawTexture(pose, vertexConsumer, -0.705F, -0.5F, -0.19375F, 0.295F, 0.5F, -0.19375F, facing.getOpposite(), ARGB_WHITE, state.lightCoords);
			IDrawing.drawTexture(pose, vertexConsumer, 0.295F, -0.5F, -0.19375F, -0.705F, 0.5F, -0.19375F, 1, 0, 0, 1, facing.getOpposite(), ARGB_WHITE, state.lightCoords);
		});
	}

	public static class SignalSemaphoreRenderState extends SignalBaseRenderState {
		float angle1;
		float angle2;
	}
}
