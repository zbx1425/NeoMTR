package mtr.render;

import mtr.block.BlockStationNameBase;
import mtr.block.BlockStationNameEntrance;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.IGui;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderStationNameTiled<T extends BlockStationNameBase.TileEntityStationNameBase> extends RenderStationNameBase<T, RenderStationNameTiled.StationNameTiledRenderState> {

	private final boolean showLogo;

	public RenderStationNameTiled(BlockEntityRenderDispatcher dispatcher, boolean showLogo) {
		super(dispatcher);
		this.showLogo = showLogo;
	}

	@Override
	protected void drawStationName(StationNameTiledRenderState state, BlockPos pos, Direction facing, StoredMatrixTransformations storedMatrixTransformations, String stationName, int stationColor, int color, int light) {
		final int totalLength = state.lengthLeft + state.lengthRight - 1;
		if (showLogo) {
			final int propagateProperty = state.propagateProperty;
			final float logoSize = propagateProperty % 2 == 0 ? 0.5F : 1;
			RenderTrains.scheduleRender(ClientData.DATA_CACHE.getStationNameEntrance(propagateProperty < 2 || propagateProperty >= 4 ? ARGB_WHITE : ARGB_BLACK, IGui.insertTranslation("gui.mtr.station_cjk", "gui.mtr.station", 1, stationName), totalLength / logoSize).resourceLocation, false, RenderTrains.QueuedRenderLayer.INTERIOR, (poseStack, vertexConsumer) -> {
				storedMatrixTransformations.transform(poseStack);
				IDrawing.drawTexture(poseStack.last(), vertexConsumer, -0.5F, -logoSize / 2, 1, logoSize, (float) (state.lengthLeft - 1) / totalLength, 0, (float) state.lengthLeft / totalLength, 1, facing, color, light);
				poseStack.popPose();
			});
		} else {
			RenderTrains.scheduleRender(ClientData.DATA_CACHE.getStationName(stationName, totalLength).resourceLocation, false, RenderTrains.QueuedRenderLayer.EXTERIOR, (poseStack, vertexConsumer) -> {
				storedMatrixTransformations.transform(poseStack);
				IDrawing.drawTexture(poseStack.last(), vertexConsumer, -0.5F, -0.5F, 1, 1, (float) (state.lengthLeft - 1) / totalLength, 0, (float) state.lengthLeft / totalLength, 1, facing, color, light);
				poseStack.popPose();
			});
		}
	}

	@Override
	public StationNameTiledRenderState createRenderState() {
		return new StationNameTiledRenderState();
	}

	@Override
	public void extractRenderState(T blockEntity, StationNameTiledRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		final BlockState blockState = blockEntity.getBlockState();
		final BlockPos pos = blockEntity.getBlockPos();
		final Level level = blockEntity.getLevel();
		state.lengthLeft = getLength(level, pos, false);
		state.lengthRight = getLength(level, pos, true);
		state.propagateProperty = level == null ? 0 : IBlock.getStatePropertySafe(blockState, BlockStationNameEntrance.STYLE);
	}

	private int getLength(BlockGetter world, BlockPos pos, boolean lookRight) {
		if (world == null) {
			return 1;
		}
		final Direction facing = IBlock.getStatePropertySafe(world, pos, BlockStationNameBase.FACING);
		final Block thisBlock = world.getBlockState(pos).getBlock();

		int length = 1;
		while (true) {
			final Block checkBlock = world.getBlockState(pos.relative(lookRight ? facing.getClockWise() : facing.getCounterClockWise(), length)).getBlock();
			if (checkBlock instanceof BlockStationNameBase && checkBlock == thisBlock) {
				length++;
			} else {
				break;
			}
		}

		return length;
	}

	public static class StationNameTiledRenderState extends StationNameRenderState {
		int lengthLeft;
		int lengthRight;
		int propagateProperty;
	}
}
