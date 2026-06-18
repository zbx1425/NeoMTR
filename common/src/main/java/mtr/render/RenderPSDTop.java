package mtr.render;

import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.BlockPSDAPGGlassEndBase;
import mtr.block.BlockPSDTop;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderPSDTop extends RenderRouteBase<BlockPSDTop.TileEntityPSDTop, RenderPSDTop.PSDTopRenderState> {

	private static final float END_FRONT_OFFSET = 1 / (Mth.SQRT_OF_TWO * 16);
	private static final float BOTTOM_DIAGONAL_OFFSET = ((float) Math.sqrt(3) - 1) / 32;
	private static final float ROOT_TWO_SCALED = Mth.SQRT_OF_TWO / 16;
	private static final float BOTTOM_END_DIAGONAL_OFFSET = END_FRONT_OFFSET - BOTTOM_DIAGONAL_OFFSET / Mth.SQRT_OF_TWO;
	private static final float COLOR_STRIP_START = 14.5F / 16;
	private static final float COLOR_STRIP_END = 15 / 16F;

	public RenderPSDTop(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher, 2 - SMALL_OFFSET_16, 7.5F, 1.5F, 0.125F, true, BlockPSDTop.ARROW_DIRECTION);
	}

	@Override
	protected RenderType getRenderType(BlockGetter world, BlockPos pos, BlockState state) {
		final BlockPSDTop.EnumPersistent persistent = IBlock.getStatePropertySafe(state, BlockPSDTop.PERSISTENT);
		if (persistent == BlockPSDTop.EnumPersistent.NONE) {
			final Block blockBelow = world.getBlockState(pos.below()).getBlock();
			if (blockBelow instanceof BlockPSDAPGDoorBase) {
				return RenderType.ARROW;
			} else if (!(blockBelow instanceof BlockPSDAPGGlassEndBase)) {
				return RenderType.ROUTE;
			} else {
				return RenderType.NONE;
			}
		} else {
			return persistent == BlockPSDTop.EnumPersistent.ARROW ? RenderType.ARROW : persistent == BlockPSDTop.EnumPersistent.ROUTE ? RenderType.ROUTE : RenderType.NONE;
		}
	}

	@Override
	protected void renderAdditionalUnmodified(PSDTopRenderState state, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
		final boolean airLeft = state.airLeft;
		final boolean airRight = state.airRight;
		final boolean persistent = state.persistent;
		if (!airLeft && !airRight || persistent) {
			return;
		}
		RenderTrains.scheduleRender(Identifier.parse("mtr:textures/block/psd_top.png"), false, RenderTrains.QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer) -> {
			storedMatrixTransformations.transform(matrices);
			if (airLeft) {
				// back
				IDrawing.drawTexture(matrices.last(), vertexConsumer, -0.125F, 0, 0.5F, 0.5F, 0, -0.125F, 0.5F, 1, -0.125F, -0.125F, 1, 0.5F, 0, 0, 1, 1, facing, -1, light);
				// front
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.5F - END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, -0.25F - END_FRONT_OFFSET, 0.0625F, 0.25F - END_FRONT_OFFSET, -0.25F - END_FRONT_OFFSET, 1, 0.25F - END_FRONT_OFFSET, 0.5F - END_FRONT_OFFSET, 1, -0.5F - END_FRONT_OFFSET, 0, 0, 1, 0.9375F, facing.getOpposite(), -1, light);
				// top curve
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.5F - BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, -0.5F - BOTTOM_END_DIAGONAL_OFFSET, -0.25F - BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, 0.25F - BOTTOM_END_DIAGONAL_OFFSET, -0.25F - END_FRONT_OFFSET, 0.0625F, 0.25F - END_FRONT_OFFSET, 0.5F - END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, 0, 0.9375F, 1, 0.96875F, facing.getOpposite(), -1, light);
				// bottom curve
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.5F, 0, -0.5F, -0.25F, 0, 0.25F, -0.25F - BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, 0.25F - BOTTOM_END_DIAGONAL_OFFSET, 0.5F - BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, -0.5F - BOTTOM_END_DIAGONAL_OFFSET, 0, 0.96875F, 1, 1, facing.getOpposite(), -1, light);
				// bottom
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.5F, SMALL_OFFSET, -0.125F, -0.125F, SMALL_OFFSET, 0.5F, -0.125F, SMALL_OFFSET, 0.125F, 0.5F, SMALL_OFFSET, -0.5F, 0.125F, 0.125F, 0.1875F, 0.1875F, facing, -1, light);
				// top
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.5F, 1 - SMALL_OFFSET, -0.5F, -0.125F, 1 - SMALL_OFFSET, 0.125F, -0.125F, 1 - SMALL_OFFSET, 0.5F, 0.5F, 1 - SMALL_OFFSET, -0.125F, 0.125F, 0.125F, 0.1875F, 0.1875F, Direction.UP, -1, light);
				// top front
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.5F - END_FRONT_OFFSET, 1 - SMALL_OFFSET, -0.5F - END_FRONT_OFFSET, -0.125F - ROOT_TWO_SCALED, 1 - SMALL_OFFSET, 0.125F, -0.125F, 1 - SMALL_OFFSET, 0.125F, 0.5F, 1 - SMALL_OFFSET, -0.5F, 0.125F, 0.125F, 0.1875F, 0.1875F, Direction.UP, -1, light);
				// left side diagonal
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.5F, 0.0625F, -0.5F, 0.5F - END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, 0.5F - END_FRONT_OFFSET, 1, -0.5F - END_FRONT_OFFSET, 0.5F, 1, -0.5F, 0.9375F, 0, 1, 0.9375F, facing, -1, light);
				// left side diagonal square
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.5F, 0, -0.5F, 0.5F - BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, -0.5F - BOTTOM_END_DIAGONAL_OFFSET, 0.5F - END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, 0.5F, 0.0625F, -0.5F, 0.9375F, 0.9375F, 1, 1, facing, -1, light);
			}
			if (airRight) {
				// back
				IDrawing.drawTexture(matrices.last(), vertexConsumer, -0.5F, 0, -0.125F, 0.125F, 0, 0.5F, 0.125F, 1, 0.5F, -0.5F, 1, -0.125F, 0, 0, 1, 1, facing, -1, light);
				// front
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.25F + END_FRONT_OFFSET, 0.0625F, 0.25F - END_FRONT_OFFSET, -0.5F + END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, -0.5F + END_FRONT_OFFSET, 1, -0.5F - END_FRONT_OFFSET, 0.25F + END_FRONT_OFFSET, 1, 0.25F - END_FRONT_OFFSET, 0, 0, 1, 0.9375F, facing.getOpposite(), -1, light);
				// top curve
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.25F + BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, 0.25F - BOTTOM_END_DIAGONAL_OFFSET, -0.5F + BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, -0.5F - BOTTOM_END_DIAGONAL_OFFSET, -0.5F + END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, 0.25F + END_FRONT_OFFSET, 0.0625F, 0.25F - END_FRONT_OFFSET, 0, 0.9375F, 1, 0.96875F, facing.getOpposite(), -1, light);
				// bottom curve
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.25F, 0, 0.25F, -0.5F, 0, -0.5F, -0.5F + BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, -0.5F - BOTTOM_END_DIAGONAL_OFFSET, 0.25F + BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, 0.25F - BOTTOM_END_DIAGONAL_OFFSET, 0, 0.96875F, 1, 1, facing.getOpposite(), -1, light);
				// bottom
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.125F, SMALL_OFFSET, 0.5F, -0.5F, SMALL_OFFSET, -0.125F, -0.5F, SMALL_OFFSET, -0.5F, 0.125F, SMALL_OFFSET, 0.125F, 0.125F, 0.125F, 0.1875F, 0.1875F, facing, -1, light);
				// top
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.125F, 1 - SMALL_OFFSET, 0.125F, -0.5F, 1 - SMALL_OFFSET, -0.5F, -0.5F, 1 - SMALL_OFFSET, -0.125F, 0.125F, 1 - SMALL_OFFSET, 0.5F, 0.125F, 0.125F, 0.1875F, 0.1875F, Direction.UP, -1, light);
				// top front
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.125F + ROOT_TWO_SCALED, 1 - SMALL_OFFSET, 0.125F, -0.5F + END_FRONT_OFFSET, 1 - SMALL_OFFSET, -0.5F - END_FRONT_OFFSET, -0.5F, 1 - SMALL_OFFSET, -0.5F, 0.125F, 1 - SMALL_OFFSET, 0.125F, 0.125F, 0.125F, 0.1875F, 0.1875F, Direction.UP, -1, light);
				// left side diagonal
				IDrawing.drawTexture(matrices.last(), vertexConsumer, -0.5F + END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, -0.5F, 0.0625F, -0.5F, -0.5F, 1, -0.5F, -0.5F + END_FRONT_OFFSET, 1, -0.5F - END_FRONT_OFFSET, 0, 0, 0.0625F, 0.9375F, facing, -1, light);
				// left side diagonal square
				IDrawing.drawTexture(matrices.last(), vertexConsumer, -0.5F + BOTTOM_END_DIAGONAL_OFFSET, BOTTOM_DIAGONAL_OFFSET, -0.5F - BOTTOM_END_DIAGONAL_OFFSET, -0.5F, 0, -0.5F, -0.5F, 0.0625F, -0.5F, -0.5F + END_FRONT_OFFSET, 0.0625F, -0.5F - END_FRONT_OFFSET, 0, 0.9375F, 0.0625F, 1, facing, -1, light);
			}
			matrices.popPose();
		});
	}

	@Override
	protected void renderAdditional(PSDTopRenderState state, StoredMatrixTransformations storedMatrixTransformations, long platformId, int leftBlocks, int rightBlocks, Direction facing, int color, int light) {
		final boolean isNotPersistent = !state.persistent;
		final boolean airLeft = isNotPersistent && state.airLeft;
		final boolean airRight = isNotPersistent && state.airRight;
		RenderTrains.scheduleRender(ClientData.DATA_CACHE.getColorStrip(platformId).resourceLocation, false, RenderTrains.QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer) -> {
			storedMatrixTransformations.transform(matrices);
			IDrawing.drawTexture(matrices.last(), vertexConsumer, airLeft ? 0.625F : 0, COLOR_STRIP_START, 0, airRight ? 0.375F : 1, COLOR_STRIP_END, 0, facing, color, light);
			if (airLeft) {
				IDrawing.drawTexture(matrices.last(), vertexConsumer, END_FRONT_OFFSET, COLOR_STRIP_START, -0.625F - END_FRONT_OFFSET, 0.75F + END_FRONT_OFFSET, COLOR_STRIP_END, 0.125F - END_FRONT_OFFSET, facing, -1, light);
			}
			if (airRight) {
				IDrawing.drawTexture(matrices.last(), vertexConsumer, 0.25F - END_FRONT_OFFSET, COLOR_STRIP_START, 0.125F - END_FRONT_OFFSET, 1 - END_FRONT_OFFSET, COLOR_STRIP_END, -0.625F - END_FRONT_OFFSET, facing, -1, light);
			}
			matrices.popPose();
		});
	}

	@Override
	protected float getAdditionalOffset(BlockState state) {
		return IBlock.getStatePropertySafe(state, BlockPSDTop.PERSISTENT) == BlockPSDTop.EnumPersistent.NONE ? 0 : BlockPSDTop.PERSISTENT_OFFSET_SMALL;
	}

	@Override
	public PSDTopRenderState createRenderState() {
		return new PSDTopRenderState();
	}

	@Override
	public void extractRenderState(BlockPSDTop.TileEntityPSDTop blockEntity, PSDTopRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		final BlockState blockState = blockEntity.getBlockState();

		state.airLeft = IBlock.getStatePropertySafe(blockState, BlockPSDTop.AIR_LEFT);
		state.airRight = IBlock.getStatePropertySafe(blockState, BlockPSDTop.AIR_RIGHT);
		state.persistent = IBlock.getStatePropertySafe(blockState, BlockPSDTop.PERSISTENT) != BlockPSDTop.EnumPersistent.NONE;
	}

	public static class PSDTopRenderState extends RouteBaseRenderState {
		boolean airLeft;
		boolean airRight;
		boolean persistent;
	}
}
