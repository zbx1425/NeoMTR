package mtr.render;

import cn.zbx1425.mtrsteamloco.render.rail.RailRenderDispatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mtr.Blocks;
import mtr.MTR;
import mtr.block.BlockFreeNode;
import mtr.block.BlockNode;
import mtr.block.IBlock;
import mtr.client.IDrawing;
import mtr.mappings.BlockEntityRendererMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RenderFreeNode extends BlockEntityRendererMapper<BlockFreeNode.TileEntityFreeNode, RenderFreeNode.FreeNodeRenderState> {

	public RenderFreeNode(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void submit(FreeNodeRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (!state.shouldRender) return;

		poseStack.pushPose();
		final float angle = state.angle;

		if (Float.isNaN(angle)) {
			poseStack.translate(0.5, 0.125, 0.5);
			poseStack.scale(0.125F, 1, 1);
			poseStack.translate(-0.5, -0.125, -0.5);
			submitNodeCollector.submitBlockModel(poseStack, RenderTypes.entitySolidZOffsetForward(TextureAtlas.LOCATION_BLOCKS),
				state.railBufferModel, new int[] {}, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
		} else {
			poseStack.translate(0.5, 0.125, 0.5);
			poseStack.mulPose(Axis.YP.rotationDegrees(-angle + 90));
			poseStack.translate(-0.5, -0.125, -0.5);
			submitNodeCollector.submitBlockModel(poseStack, RenderTypes.entitySolidZOffsetForward(TextureAtlas.LOCATION_BLOCKS),
				state.railBufferModel, new int[] {}, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
		}

		poseStack.popPose();
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

		state.nodeConnected = IBlock.getStatePropertySafe(blockState, BlockNode.IS_CONNECTED);
		if (state.nodeConnected) {
			state.shouldRender = false;
			return;
		}

		state.shouldRender = true;
		state.angle = blockEntity.getAngleDegrees();

		state.railBufferModel = new ArrayList<>();
		BlockStateModel railBufferModel = Minecraft.getInstance().getModelManager().getBlockStateModelSet()
			.get(Blocks.RAIL_NODE.get().defaultBlockState());
		railBufferModel.collectParts(RandomSource.createThreadLocalInstance(), state.railBufferModel);
	}

	public static class FreeNodeRenderState extends BlockEntityRenderState {
		float angle;
		boolean shouldRender;
		boolean nodeConnected;

		List<BlockStateModelPart> railBufferModel;
	}
}
