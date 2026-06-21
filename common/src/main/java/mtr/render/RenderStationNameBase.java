package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.BlockStationNameBase;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.data.RailwayData;
import mtr.data.Station;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.Text;
import mtr.util.UtilitiesClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class RenderStationNameBase<T extends BlockStationNameBase.TileEntityStationNameBase, S extends RenderStationNameBase.StationNameRenderState> extends BlockEntityRendererMapper<T, S> implements IGui, IDrawing {

	public RenderStationNameBase(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		final StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations();
		storedMatrixTransformations.add(matricesNew -> {
			matricesNew.translate(0.5 + state.blockPos.getX(), 0.5 + state.yOffset + state.blockPos.getY(), 0.5 + state.blockPos.getZ());
			UtilitiesClient.rotateYDegrees(matricesNew, -state.facing.toYRot());
			UtilitiesClient.rotateZDegrees(matricesNew, 180);
		});


		for (int i = 0; i < (state.isDoubleSided ? 2 : 1); i++) {
			final StoredMatrixTransformations storedMatrixTransformations2 = storedMatrixTransformations.copy();
			final boolean shouldFlip = i == 1;
			storedMatrixTransformations2.add(matricesNew -> {
				if (shouldFlip) {
					UtilitiesClient.rotateYDegrees(matricesNew, 180);
				}
				matricesNew.translate(0, 0, 0.5 - state.zOffset - SMALL_OFFSET);
			});
			drawStationName(state, state.blockPos, state.facing, storedMatrixTransformations2, state.stationName, state.stationColor, state.textColor, state.lightCoords);
		}
	}

	@Override
	public void extractRenderState(T blockEntity, S state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		final BlockState blockState = blockEntity.getBlockState();
		final BlockPos pos = blockEntity.getBlockPos();

		final Station station = RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, pos);
		state.facing = IBlock.getStatePropertySafe(blockState, BlockStationNameBase.FACING);
		state.stationName = station == null ? Text.translatable("gui.mtr.untitled").getString() : station.name;
		state.stationColor = station == null ? 0 : ARGB_BLACK + station.color;
		state.textColor = RenderRouteBase.getShadingColor(state.facing, blockEntity.getColor(blockState));
		state.isDoubleSided = blockEntity.isDoubleSided;
		state.yOffset = blockEntity.yOffset;
		state.zOffset = blockEntity.zOffset;
	}

	protected abstract void drawStationName(S state, BlockPos pos, Direction facing, StoredMatrixTransformations storedMatrixTransformations, String stationName, int stationColor, int color, int light);

	public static class StationNameRenderState extends BlockEntityRenderState {
		Direction facing;
		String stationName;
		int stationColor;
		int textColor;
		boolean isDoubleSided;
		float yOffset;
		float zOffset;
	}
}
