package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.BlockLiftButtons;
import mtr.block.BlockLiftTrackFloor;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.data.Lift;
import mtr.data.RailwayData;
import mtr.item.ItemLiftButtonsLinkModifier;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.util.Utilities;
import mtr.util.UtilitiesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class RenderLiftButtons extends BlockEntityRendererMapper<BlockLiftButtons.TileEntityLiftButtons, RenderLiftButtons.LiftButtonRenderState> implements IGui, IBlock {

	private static final int HOVER_COLOR = 0xFFFFAAAA;
	private static final Identifier BUTTON_TEXTURE = Identifier.parse("mtr:textures/block/lift_button.png");

	public RenderLiftButtons(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void submit(LiftButtonRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if(state.shouldRender) {
			poseStack.pushPose();
			poseStack.translate(0.5, 0, 0.5);

			final boolean[] buttonStates = {false, false, false, false};
			final Map<BlockPos, Tuple<String, Lift.LiftDirection>> liftDisplays = new HashMap<>();
			final List<BlockPos> liftPositions = new ArrayList<>();
			state.tracks.forEach(track -> {
				BlockPos trackPosition = track.pos();
				BlockLiftTrackFloor.TileEntityLiftTrackFloor floorBlockEntity = track.blockEntity();
				renderLiftObjectLink(poseStack, submitNodeCollector, state.level, state.blockPos, trackPosition, state.facing, state.holdingLinker);

				ClientData.LIFTS.forEach(lift -> {
					if (lift.hasFloor(trackPosition)) {
						lift.hasUpDownButtonForFloor(trackPosition.getY(), buttonStates);
						if (lift.liftInstructions.containsInstruction(trackPosition.getY(), true)) {
							buttonStates[2] = true;
						}
						if (lift.liftInstructions.containsInstruction(trackPosition.getY(), false)) {
							buttonStates[3] = true;
						}

						final BlockPos liftPos = RailwayData.newBlockPos(lift.getPositionX(), 0, lift.getPositionZ());
						liftPositions.add(liftPos);
						liftDisplays.put(liftPos, new Tuple<>(ClientData.DATA_CACHE.requestLiftFloorText(lift.getCurrentFloorBlockPos())[0], lift.getLiftDirection()));
					}
				});
			});

			liftPositions.sort(Comparator.comparingInt(checkPos -> state.facing.getStepX() * (checkPos.getZ() - state.blockPos.getZ()) - state.facing.getStepZ() * (checkPos.getX() - state.blockPos.getX())));

			final HitResult hitResult = Minecraft.getInstance().hitResult;
			final boolean lookingAtTopHalf;
			final boolean lookingAtBottomHalf;
			if (hitResult == null || !state.buttonUnlocked) {
				lookingAtTopHalf = false;
				lookingAtBottomHalf = false;
			} else {
				final Vec3 hitLocation = hitResult.getLocation();
				final double hitX = hitLocation.x - Math.floor(hitLocation.x);
				final double hitY = hitLocation.y - Math.floor(hitLocation.y);
				final double hitZ = hitLocation.z - Math.floor(hitLocation.z);
				final boolean inBlock = hitX > 0 && hitY > 0 && hitZ > 0 && RailwayData.newBlockPos(hitLocation).equals(state.blockPos);
				lookingAtTopHalf = inBlock && (!buttonStates[1] || hitY > 0.25 && hitY < 0.5);
				lookingAtBottomHalf = inBlock && (!buttonStates[0] || hitY < 0.25);
			}

			UtilitiesClient.rotateYDegrees(poseStack, -state.facing.toYRot());
			poseStack.translate(0, 0, 0.4375 - SMALL_OFFSET);

			if (buttonStates[0]) {
				final RenderType renderType = buttonStates[2] || lookingAtTopHalf ? MoreRenderLayers.getLight(BUTTON_TEXTURE, true) : MoreRenderLayers.getExterior(BUTTON_TEXTURE);
				submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
					IDrawing.drawTexture(pose, vertexConsumer, -1.5F / 16, (buttonStates[1] ? 4.5F : 2.5F) / 16, 3F / 16, 3F / 16, 0, 1, 1, 0, state.facing, buttonStates[2] ? RenderTrains.LIFT_LIGHT_COLOR : lookingAtTopHalf ? HOVER_COLOR : ARGB_GRAY, state.lightCoords);
				});
			}
			if (buttonStates[1]) {
				final RenderType renderType = buttonStates[3] || lookingAtBottomHalf ? MoreRenderLayers.getLight(BUTTON_TEXTURE, true) : MoreRenderLayers.getExterior(BUTTON_TEXTURE);
				submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
					IDrawing.drawTexture(pose, vertexConsumer, -1.5F / 16, (buttonStates[0] ? 0.5F : 2.5F) / 16, 3F / 16, 3F / 16, 0, 0, 1, 1, state.facing, buttonStates[3] ? RenderTrains.LIFT_LIGHT_COLOR : lookingAtBottomHalf ? HOVER_COLOR : ARGB_GRAY, state.lightCoords);
				});
			}

			final float maxWidth = Math.min(0.25F, 0.375F / liftPositions.size());
			UtilitiesClient.rotateZDegrees(poseStack, 180);
			poseStack.translate(maxWidth * (0.5 - liftPositions.size() / 2F), 0, 0);
			submitNodeCollector.submitCustomGeometry(poseStack, MoreRenderLayers.getExterior(Identifier.parse("mtr:textures/block/black.png")), (pose, vertexConsumer) -> {
				IDrawing.drawTexture(pose, vertexConsumer, -maxWidth / 2, -0.9375F, maxWidth * liftPositions.size(), 0.40625F, Direction.UP, state.lightCoords);
			});
			poseStack.translate(0, -0.875, -SMALL_OFFSET);

			liftPositions.forEach(liftPosition -> {
				final Tuple<String, Lift.LiftDirection> liftDisplay = liftDisplays.get(liftPosition);
				if (liftDisplay != null) {
					RenderTrains.renderLiftDisplay(poseStack, (renderType, callback) -> {
						submitNodeCollector.submitCustomGeometry(poseStack, renderType, callback::accept);
					}, state.blockPos, liftDisplay.getA(), liftDisplay.getB(), maxWidth, 0.3125F);
				}
				poseStack.translate(maxWidth, 0, 0);
			});

			poseStack.popPose();
		}
	}

	@Override
	public LiftButtonRenderState createRenderState() {
		return new LiftButtonRenderState();
	}

	@Override
	public void extractRenderState(BlockLiftButtons.TileEntityLiftButtons blockEntity, LiftButtonRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		final BlockState blockState = blockEntity.getBlockState();
		final BlockPos pos = blockEntity.getBlockPos();
		final Level level = blockEntity.getLevel();
		if (level == null) {
			state.shouldRender = false;
			return;
		}

		if (RenderTrains.shouldNotRender(pos, RenderTrains.maxTrainRenderDistance, null)) {
			state.shouldRender = false;
			return;
		}
		state.shouldRender = true;
		state.level = level;
		state.facing = IBlock.getStatePropertySafe(blockState, HorizontalDirectionalBlock.FACING);
		state.holdingLinker = Utilities.isHolding(Minecraft.getInstance().player, item -> item instanceof ItemLiftButtonsLinkModifier || Block.byItem(item) instanceof BlockLiftButtons);
		state.buttonUnlocked = IBlock.getStatePropertySafe(blockState, BlockLiftButtons.UNLOCKED);

		List<LiftButtonRenderState.Track> tracks = new ArrayList<>();
		blockEntity.forEachTrackPosition(level, (trackPosition, trackFloorTileEntity) -> {
			tracks.add(new LiftButtonRenderState.Track(trackPosition, trackFloorTileEntity));
		});
		state.tracks.clear();
		state.tracks.addAll(tracks);
	}

	public static void renderLiftObjectLink(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Level level, BlockPos pos, BlockPos trackPosition, Direction facing, boolean holdingLinker) {
		if (holdingLinker) {
			final Direction trackFacing = IBlock.getStatePropertySafe(level, trackPosition, HorizontalDirectionalBlock.FACING);
			IDrawing.drawLine((renderType, callback) -> submitNodeCollector.submitCustomGeometry(poseStack, renderType, callback::accept), trackPosition.getX() - pos.getX() + trackFacing.getStepX() / 2F, trackPosition.getY() - pos.getY() + 0.5F, trackPosition.getZ() - pos.getZ() + trackFacing.getStepZ() / 2F, facing.getStepX() / 2F, 0.25F, facing.getStepZ() / 2F, 0xFF, 0xFF, 0xFF);
		}
	}

	public static class LiftButtonRenderState extends BlockEntityRenderState {
		Level level;
		Direction facing;
		List<Track> tracks = new ArrayList<>();
		boolean shouldRender;
		boolean holdingLinker;
		boolean buttonUnlocked;

		public record Track(BlockPos pos, BlockLiftTrackFloor.TileEntityLiftTrackFloor blockEntity) {
		}
	}
}
