package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.BlockNode;
import mtr.block.IBlock;
import mtr.client.IDrawing;
import mtr.mappings.BlockEntityRendererMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderBoatNode extends BlockEntityRendererMapper<BlockNode.TileEntityBoatNode, RenderBoatNode.BoatNodeRenderState> {

	public RenderBoatNode(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void submit(BoatNodeRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (!state.shouldRender) return;

		poseStack.pushPose();
		submitNodeCollector.submitCustomGeometry(poseStack, MoreRenderLayers.getExterior(Identifier.parse("textures/block/oak_log.png")), (pose, vertexConsumer) -> {
			IDrawing.drawTexture(pose, vertexConsumer, 0.25F, 0, 0.25F, 0.25F, 0, 0.75F, 0.75F, 0, 0.75F, 0.75F, 0, 0.25F, 0.25F, 0.25F, 0.75F, 0.75F, Direction.EAST, -1, state.lightCoords);
			IDrawing.drawTexture(pose, vertexConsumer, 0.75F, 0, 0.25F, 0.75F, 0, 0.75F, 0.25F, 0, 0.75F, 0.25F, 0, 0.25F, 0.25F, 0.25F, 0.75F, 0.75F, Direction.DOWN, -1, state.lightCoords);
		});
		poseStack.popPose();
	}

	@Override
	public void extractRenderState(BlockNode.TileEntityBoatNode blockEntity, BoatNodeRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		final BlockState blockState = blockEntity.getBlockState();
		if (blockState.getBlock() instanceof BlockNode.BlockBoatNode && !IBlock.getStatePropertySafe(blockState, BlockNode.IS_CONNECTED)) {
			state.shouldRender = false;
			return;
		}
		state.shouldRender = RenderTrains.isHoldingRailRelated(Minecraft.getInstance().player);
	}

	@Override
	public BoatNodeRenderState createRenderState() {
		return new BoatNodeRenderState();
	}

	public static class BoatNodeRenderState extends BlockEntityRenderState {
		boolean shouldRender;
	}
}
