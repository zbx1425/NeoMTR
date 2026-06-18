package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.BlockNode;
import mtr.block.BlockSignalLightBase;
import mtr.block.BlockSignalSemaphoreBase;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.data.IGui;
import mtr.data.Rail;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.UtilitiesClient;
import mtr.path.PathData;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class RenderSignalBase<T extends BlockEntityMapper, S extends RenderSignalBase.SignalBaseRenderState> extends BlockEntityRendererMapper<T, S> implements IBlock, IGui {

	protected final boolean isSingleSided;
	protected final int aspects;

	public RenderSignalBase(BlockEntityRenderDispatcher dispatcher, boolean isSingleSided, int aspects) {
		super(dispatcher);
		this.isSingleSided = isSingleSided;
		this.aspects = aspects;
	}

	@Override
	public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if(state.shouldRender) {
			poseStack.pushPose();
			poseStack.translate(0.5, 0, 0.5);

			for (int i = 0; i < (isSingleSided ? 1 : 2); i++) {
				final Direction newFacing = (i == 1 ? state.facing.getOpposite() : state.facing);
				int aspect = i == 0 ? state.occupiedAspect : state.occupiedAspectOpposite;

				if (aspect >= 0) {
					poseStack.pushPose();
					UtilitiesClient.rotateYDegrees(poseStack, -newFacing.toYRot());
					drawSignal(state, poseStack, submitNodeCollector, MoreRenderLayers.getLight(Identifier.parse("mtr:textures/block/white.png"), false), newFacing, state.occupiedAspect, i == 1);
					poseStack.popPose();
				}
			}

			poseStack.popPose();
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

		if (!(blockState.getBlock() instanceof BlockSignalLightBase || blockState.getBlock() instanceof BlockSignalSemaphoreBase)) {
			state.shouldRender = false;
			return;
		}
		final Direction facing = IBlock.getStatePropertySafe(blockState, HorizontalDirectionalBlock.FACING);
		if (RenderTrains.shouldNotRender(pos, RenderTrains.maxTrainRenderDistance, null)) {
			state.shouldRender = false;
			return;
		}
		final BlockPos startPos = getNodePos(level, pos, facing);
		if (startPos == null) {
			state.shouldRender = false;
			return;
		}
		state.shouldRender = true;
		state.facing = facing;

		for (int i = 0; i < (isSingleSided ? 1 : 2); i++) {
			final Direction newFacing = (i == 1 ? facing.getOpposite() : facing);
			final int occupiedAspect = getOccupiedAspect(startPos, newFacing.toYRot() + 90);

			if(i == 0) {
				state.occupiedAspect = occupiedAspect;
			} else {
				state.occupiedAspectOpposite = occupiedAspect;
			}
		}
	}

	protected abstract void drawSignal(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, RenderType renderType, Direction facing, int occupiedAspect, boolean isBackSide);

	private int getOccupiedAspect(BlockPos startPos, float facing) {
		Map<BlockPos, Float> nodesToScan = new HashMap<>();
		nodesToScan.put(startPos, facing);
		int occupiedAspect = -1;

		for (int j = 1; j < aspects; j++) {
			final Map<BlockPos, Float> newNodesToScan = new HashMap<>();

			for (final Map.Entry<BlockPos, Float> checkNode : nodesToScan.entrySet()) {
				final Map<BlockPos, Rail> railMap = ClientData.RAILS.get(checkNode.getKey());

				if (railMap != null) {
					for (final BlockPos endPos : railMap.keySet()) {
						final Rail rail = railMap.get(endPos);
						if (rail.facingStart.similarFacing(checkNode.getValue())) {
							if (ClientData.SIGNAL_BLOCKS.isOccupied(PathData.getRailProduct(checkNode.getKey(), endPos))) {
								return j;
							} else {
								final Boolean isOccupied = ClientData.OCCUPIED_RAILS.get(PathData.getRailProduct(checkNode.getKey(), endPos));
								if (isOccupied != null && isOccupied) {
									return j;
								}
							}

							newNodesToScan.put(endPos, rail.facingEnd.getOpposite().angleDegrees);
							occupiedAspect = 0;
						}
					}
				}
			}

			nodesToScan = newNodesToScan;
		}

		return occupiedAspect;
	}

	private static BlockPos getNodePos(BlockGetter world, BlockPos pos, Direction facing) {
		final int[] checkDistance = {0, 1, -1, 2, -2, 3, -3, 4, -4};
		for (final int z : checkDistance) {
			for (final int x : checkDistance) {
				for (int y = -5; y <= 0; y++) {
					final BlockPos checkPos = pos.above(y).relative(facing.getClockWise(), x).relative(facing, z);
					final BlockState checkState = world.getBlockState(checkPos);
					if (checkState.getBlock() instanceof BlockNode) {
						return checkPos;
					}
				}
			}
		}
		return null;
	}

	public static class SignalBaseRenderState extends BlockEntityRenderState {
		boolean shouldRender;
		Direction facing;
		int occupiedAspect;
		int occupiedAspectOpposite;
	}
}
