package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mtr.block.BlockFreeNode;
import mtr.block.BlockNode;
import mtr.block.IBlock;
import mtr.client.IDrawing;
import mtr.mappings.BlockEntityRendererMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFreeNode extends BlockEntityRendererMapper<BlockFreeNode.TileEntityFreeNode, RenderFreeNode.FreeNodeRenderState> {

	public RenderFreeNode(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void submit(FreeNodeRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if(state.shouldRender) {
			poseStack.pushPose();
			final float angle = state.angle;
			final RenderType renderType = MoreRenderLayers.getExterior(Identifier.parse("textures/block/oak_log.png"));

			if (Float.isNaN(angle)) {
				// Undetermined: thin vertical pole
				submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
					IDrawing.drawTexture(pose, vertexConsumer, 0.45F, 0.25F, 0.45F, 0.55F, 0.25F, 0.55F, 0.45F, 1, 0.45F, 0.55F, 1, 0.55F, 0.45F, 0.45F, 0.55F, 0.55F, Direction.UP, -1, state.lightCoords);
				});
			} else {
				poseStack.translate(0.5, 0.125, 0.5);
				poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
				poseStack.translate(-0.5, -0.125, -0.5);
				final float y0 = state.nodeConnected ? 0.05F : 0.15F;
				submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
					IDrawing.drawTexture(pose, vertexConsumer, 0.2F, y0, 0.45F, 0.8F, y0 + 0.1F, 0.55F, 0.2F, y0 + 0.1F, 0.55F, 0.8F, y0, 0.45F, 0.2F, 0.45F, 0.8F, 0.55F, Direction.UP, -1, state.lightCoords);
				});
			}

			poseStack.popPose();
		}
	}

	@Override
	public FreeNodeRenderState createRenderState() {
		return new FreeNodeRenderState();
	}

	@Override
	public void extractRenderState(BlockFreeNode.TileEntityFreeNode blockEntity, FreeNodeRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		final BlockState blockState = blockEntity.getBlockState();

		if (!(blockState.getBlock() instanceof BlockFreeNode)) {
			state.shouldRender = false;
			return;
		}

		if (!RenderTrains.isHoldingRailRelated(Minecraft.getInstance().player)) {
			state.shouldRender = false;
			return;
		}

		state.shouldRender = true;
		state.nodeConnected = IBlock.getStatePropertySafe(blockState, BlockNode.IS_CONNECTED);
		state.angle = blockEntity.getAngleDegrees();
	}

	public static class FreeNodeRenderState extends BlockEntityRenderState {
		float angle;
		boolean shouldRender;
		boolean nodeConnected;
	}
}
