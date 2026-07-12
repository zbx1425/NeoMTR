package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.BlockPSDTop;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.util.UtilitiesClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class RenderRouteBase<T extends BlockPSDTop.TileEntityRouteBase, S extends RenderRouteBase.RouteBaseRenderState> extends BlockEntityRendererMapper<T, S> implements IGui, IBlock {

	protected final float topPadding;
	protected final float bottomPadding;
	protected final float sidePadding;
	private final float z;
	private final boolean transparentWhite;
	private final Property<Integer> arrowDirectionProperty;

	public RenderRouteBase(BlockEntityRenderDispatcher dispatcher, float z, float topPadding, float bottomPadding, float sidePadding, boolean transparentWhite, Property<Integer> arrowDirectionProperty) {
		super(dispatcher);
		this.z = z / 16;
		this.topPadding = topPadding / 16;
		this.bottomPadding = bottomPadding / 16;
		this.sidePadding = sidePadding / 16;
		this.transparentWhite = transparentWhite;
		this.arrowDirectionProperty = arrowDirectionProperty;
	}

	@Override
	public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (!state.shouldRender) return;

		final StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations();
		storedMatrixTransformations.add(matricesNew -> {
			matricesNew.translate(0.5 + state.blockPos.getX(), state.blockPos.getY(), 0.5 + state.blockPos.getZ());
			UtilitiesClient.rotateYDegrees(matricesNew, -state.facing.toYRot());
		});

		renderAdditionalUnmodified(state, storedMatrixTransformations.copy(), state.facing, state.lightCoords);

		if (state.platformId != 0) {
			storedMatrixTransformations.add(matricesNew -> {
				matricesNew.translate(0, 1, 0);
				UtilitiesClient.rotateZDegrees(matricesNew, 180);
				matricesNew.translate(-0.5, -state.additionalYOffset, z);
			});

			final int color = getShadingColor(state.facing, ARGB_WHITE);

			RenderType routeRenderType = state.routeRenderType;
			if ((routeRenderType == RenderType.ARROW || routeRenderType == RenderType.ROUTE) && state.sideExtended != EnumSide.SINGLE) {
				final float width = state.leftBlocks + state.rightBlocks + 1 - sidePadding * 2;
				final float height = 1 - topPadding - bottomPadding;

				final Identifier resourceLocation;
				if (routeRenderType == RenderType.ARROW) {
					resourceLocation = ClientData.DATA_CACHE.getDirectionArrow(state.platformId, (state.arrowDirection & 0b01) > 0, (state.arrowDirection & 0b10) > 0, HorizontalAlignment.CENTER, true, 0.25F, width / height, ARGB_WHITE, ARGB_BLACK, transparentWhite ? ARGB_WHITE : 0).resourceLocation;
				} else {
					resourceLocation = ClientData.DATA_CACHE.getRouteMap(state.platformId, false, state.arrowDirection == 2, width / height, transparentWhite).resourceLocation;
				}

				RenderTrains.scheduleRender(resourceLocation, false, RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
					storedMatrixTransformations.transform(matricesNew);
					IDrawing.drawTexture(matricesNew.last(), vertexConsumer, state.leftBlocks == 0 ? sidePadding : 0, topPadding, 0, 1 - (state.rightBlocks == 0 ? sidePadding : 0), 1 - bottomPadding, 0, (state.leftBlocks - (state.leftBlocks == 0 ? 0 : sidePadding)) / width, 0, (width - state.rightBlocks + (state.rightBlocks == 0 ? 0 : sidePadding)) / width, 1, state.facing.getOpposite(), color, state.lightCoords);
					matricesNew.popPose();
				});
			}

			renderAdditional(state, storedMatrixTransformations, state.platformId, state.leftBlocks, state.rightBlocks, state.facing.getOpposite(), color, state.lightCoords);
		}
	}

	@Override
	public void extractRenderState(T blockEntity, S state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		final BlockState blockState = blockEntity.getBlockState();
		final BlockPos pos = blockEntity.getBlockPos();
		final Level level = blockEntity.getLevel();
		if (level == null) {
			state.shouldRender = false;
			return;
		}

		state.facing = IBlock.getStatePropertySafe(blockState, HorizontalDirectionalBlock.FACING);
		state.sideExtended = IBlock.getStatePropertySafe(blockState, SIDE_EXTENDED);
		state.shouldRender = !RenderTrains.shouldNotRender(blockEntity.getBlockPos(), RenderTrains.maxTrainRenderDistance, null);

		if (!state.shouldRender) return;

		final long platformId = blockEntity.getPlatformId(ClientData.PLATFORMS, ClientData.DATA_CACHE);
		state.platformId = platformId;
		state.shouldRender = platformId != 0;

		if (platformId != 0) {
			state.leftBlocks = getTextureNumber(level, pos, state.facing, true);
			state.rightBlocks = getTextureNumber(level, pos, state.facing, false);
			state.routeRenderType = getRenderType(level, pos.relative(state.facing.getCounterClockWise(), state.leftBlocks), blockState);
			state.arrowDirection = IBlock.getStatePropertySafe(blockState, arrowDirectionProperty);
			state.additionalYOffset = -getAdditionalOffset(blockState);
		}
	}

	public static class RouteBaseRenderState extends BlockEntityRenderState {
		Direction facing;
		RenderType routeRenderType;
		EnumSide sideExtended;
		boolean shouldRender;
		long platformId;
		int leftBlocks;
		int rightBlocks;
		int arrowDirection;
		float additionalYOffset;
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	protected void renderAdditionalUnmodified(S state, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
	}

	protected float getAdditionalOffset(BlockState state) {
		return 0;
	}

	protected boolean isLeft(BlockState state) {
		return IBlock.getStatePropertySafe(state, SIDE_EXTENDED) == IBlock.EnumSide.LEFT;
	}

	protected boolean isRight(BlockState state) {
		return IBlock.getStatePropertySafe(state, SIDE_EXTENDED) == IBlock.EnumSide.RIGHT;
	}

	protected abstract RenderType getRenderType(BlockGetter world, BlockPos pos, BlockState state);

	protected abstract void renderAdditional(S state, StoredMatrixTransformations storedMatrixTransformations, long platformId, int leftBlocks, int rightBlocks, Direction facing, int color, int light);

	private int getTextureNumber(BlockGetter world, BlockPos pos, Direction facing, boolean searchLeft) {
		int number = 0;
		final Block thisBlock = world.getBlockState(pos).getBlock();

		while (true) {
			final BlockState state = world.getBlockState(pos.relative(searchLeft ? facing.getCounterClockWise() : facing.getClockWise(), number));

			if (state.getBlock() == thisBlock) {
				final boolean isLeft = isLeft(state);
				final boolean isRight = isRight(state);

				if (number == 0 || (searchLeft ? !isRight : !isLeft)) {
					number++;
					if (searchLeft ? isLeft : isRight) {
						break;
					}
				} else {
					break;
				}
			} else {
				break;
			}
		}

		return number - 1;
	}

	public static int getShadingColor(Direction facing, int grayscaleColorByte) {
		final int colorByte = Math.round((grayscaleColorByte & 0xFF) * (facing.getAxis() == Direction.Axis.X ? 0.75F : 1));
		return ARGB_BLACK | ((colorByte << 16) + (colorByte << 8) + colorByte);
	}

	protected enum RenderType {ARROW, ROUTE, NONE}
}
