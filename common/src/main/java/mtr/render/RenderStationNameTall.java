package mtr.render;

import mtr.block.BlockStationNameTallBase;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderStationNameTall<T extends BlockStationNameTallBase.TileEntityStationNameTallBase> extends RenderStationNameBase<T, RenderStationNameTall.StationNameTallRenderState> {

	private static final float WIDTH = 0.6875F;
	private static final float HEIGHT = 1.5F;

	public RenderStationNameTall(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	protected void drawStationName(StationNameTallRenderState state, BlockPos pos, Direction facing, StoredMatrixTransformations storedMatrixTransformations, String stationName, int stationColor, int color, int light) {
		if (!state.shouldRender) return;

		RenderTrains.scheduleRender(ClientData.DATA_CACHE.getTallStationName(color, stationName, stationColor, WIDTH / HEIGHT).resourceLocation, false, RenderTrains.QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer) -> {
			storedMatrixTransformations.transform(matrices);
			IDrawing.drawTexture(matrices.last(), vertexConsumer, -WIDTH / 2, -HEIGHT / 2, WIDTH, HEIGHT, 0, 0, 1, 1, facing, ARGB_WHITE, light);
			matrices.popPose();
		});
	}

	@Override
	public StationNameTallRenderState createRenderState() {
		return new StationNameTallRenderState();
	}

	@Override
	public void extractRenderState(T blockEntity, StationNameTallRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.shouldRender = IBlock.getStatePropertySafe(blockEntity.getBlockState(), BlockStationNameTallBase.THIRD) == IBlock.EnumThird.MIDDLE;
	}

	public static class StationNameTallRenderState extends StationNameRenderState {
		boolean shouldRender;
	}
}
