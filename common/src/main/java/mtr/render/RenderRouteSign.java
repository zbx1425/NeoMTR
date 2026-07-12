package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.BlockRouteSignBase;
import mtr.block.BlockStationNameBase;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.data.Platform;
import mtr.data.RailwayData;
import mtr.data.Station;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.util.UtilitiesClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class RenderRouteSign<T extends BlockRouteSignBase.TileEntityRouteSignBase> extends BlockEntityRendererMapper<T, RenderRouteSign.RouteSignRenderState> implements IBlock, IGui {

	private static final float SIDE = 2.5F / 16;
	private static final float BOTTOM = 10.5F / 16;
	private static final float MIDDLE = 13F / 16;
	private static final float TOP = 15.5F / 16;
	private static final float WIDTH = 1 - SIDE * 2;
	private static final float HEIGHT_BOTTOM = MIDDLE - BOTTOM + 1;
	private static final float HEIGHT_TOP = TOP - MIDDLE;
	private static final float TEXTURE_BREAK = MIDDLE / HEIGHT_BOTTOM;

	public RenderRouteSign(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void submit(RouteSignRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (!state.shouldRender) return;

		poseStack.pushPose();
		poseStack.translate(0.5, 0, 0.5);
		UtilitiesClient.rotateYDegrees(poseStack, -state.facing.toYRot());
		poseStack.translate(-0.5, 0, 0.4375 - SMALL_OFFSET * 2);

		submitNodeCollector.submitCustomGeometry(poseStack, MoreRenderLayers.getExterior(ClientData.DATA_CACHE.getDirectionArrow(state.platformId, (state.arrowDirection & 0b01) > 0, (state.arrowDirection & 0b10) > 0, HorizontalAlignment.CENTER, true, 0.2F, WIDTH / HEIGHT_TOP, ARGB_BLACK, ARGB_WHITE, 0).resourceLocation), (pose, vertexConsumer) -> {
			IDrawing.drawTexture(pose, vertexConsumer, 1 - SIDE, TOP + (state.isTop ? 0 : 1), 0, SIDE, MIDDLE + (state.isTop ? 0 : 1), 0, 0, 0, 1, 1, state.facing.getOpposite(), -1, state.lightCoords);
		});

		submitNodeCollector.submitCustomGeometry(poseStack, MoreRenderLayers.getExterior(ClientData.DATA_CACHE.getRouteMap(state.platformId, true, false, HEIGHT_BOTTOM / WIDTH, false).resourceLocation), (pose, vertexConsumer) -> {
			IDrawing.drawTexture(poseStack.last(), vertexConsumer, 1 - SIDE, MIDDLE + (state.isTop ? 0 : 1), 0, 1 - SIDE, state.isTop ? 0 : BOTTOM, 0, SIDE, state.isTop ? 0 : BOTTOM, 0, SIDE, MIDDLE + (state.isTop ? 0 : 1), 0, 0, 0, state.isTop ? TEXTURE_BREAK : 1, 1, state.facing.getOpposite(), -1, state.lightCoords);
		});

		poseStack.popPose();
	}

	@Override
	public RouteSignRenderState createRenderState() {
		return new RenderRouteSign.RouteSignRenderState();
	}

	@Override
	public void extractRenderState(T blockEntity, RouteSignRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		final BlockState blockState = blockEntity.getBlockState();
		final BlockPos pos = blockEntity.getBlockPos();
		final Direction facing = IBlock.getStatePropertySafe(blockState, BlockStationNameBase.FACING);
		if (RenderTrains.shouldNotRender(pos, RenderTrains.maxTrainRenderDistance, facing)) {
			state.shouldRender = false;
			return;
		}
		final Station station = RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, state.blockPos);
		if (station == null) {
			state.shouldRender = false;
			return;
		}

		final Map<Long, Platform> platformPositions = ClientData.DATA_CACHE.requestStationIdToPlatforms(station.id);
		if (platformPositions == null || platformPositions.isEmpty()) {
			state.shouldRender = false;
			return;
		}

		final Platform platform = platformPositions.get(blockEntity.getPlatformId());
		if (platform == null) {
			state.shouldRender = false;
			return;
		}
		state.shouldRender = true;
		state.facing = facing;
		state.isTop = IBlock.getStatePropertySafe(blockState, HALF) == DoubleBlockHalf.UPPER;
		state.arrowDirection = IBlock.getStatePropertySafe(blockState, BlockRouteSignBase.ARROW_DIRECTION);
		state.platformId = platform.id;
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	public static class RouteSignRenderState extends BlockEntityRenderState {
		boolean shouldRender;
		boolean isTop;
		int arrowDirection;
		long platformId;
		Direction facing;
	}
}
