package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import mtr.MTRClient;
import mtr.block.BlockLiftPanelBase;
import mtr.block.BlockLiftTrackFloor;
import mtr.block.IBlock;
import mtr.block.ITripleBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.Lift;
import mtr.item.ItemLiftButtonsLinkModifier;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.Utilities;
import mtr.mappings.UtilitiesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static mtr.data.IGui.*;

public class RenderLiftPanel<T extends BlockLiftPanelBase.TileEntityLiftPanel1Base> extends BlockEntityRendererMapper<T, RenderLiftPanel.LiftPanelRenderState> {

	private final boolean isOdd;
	private final boolean isFlat;

	private static final Identifier ARROW_TEXTURE = Identifier.parse("mtr:textures/block/lift_arrow.png");
	private static final float ARROW_SPEED = 0.04F;
	private static final int SLIDE_TIME = 5;
	private static final int SLIDE_INTERVAL = 50;
	private static final float PANEL_WIDTH = 1.125F;

	public RenderLiftPanel(BlockEntityRenderDispatcher dispatcher, boolean isOdd, boolean isFlat) {
		super(dispatcher);
		this.isOdd = isOdd;
		this.isFlat = isFlat;
	}

	@Override
	public void submit(LiftPanelRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if(state.shouldRender) {
			final Font textRenderer = Minecraft.getInstance().font;

            poseStack.pushPose();
			poseStack.translate(0.5, 0, 0.5);
			RenderLiftButtons.renderLiftObjectLink(poseStack, submitNodeCollector, state.level, state.blockPos, state.linkedPosition, state.facing, state.holdingLinker);

			if (state.lift != null) {
				final String[] text = ClientData.DATA_CACHE.requestLiftFloorText(state.lift.getCurrentFloorBlockPos());
				UtilitiesClient.rotateYDegrees(poseStack, -state.facing.toYRot());
				UtilitiesClient.rotateZDegrees(poseStack, 180);
				poseStack.translate(isOdd ? 0 : 0.5, 0, 0);

				// Floor Number
				poseStack.pushPose();
				poseStack.translate(0, 0, (isFlat ? 0.4375F : 0.25F) - SMALL_OFFSET * 2);
				final MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
				IDrawing.drawStringWithFont(poseStack, textRenderer, immediate, ClientData.DATA_CACHE.requestLiftFloorText(state.linkedPosition)[0], HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 0, -0.47F, 0.1875F, 0.1875F, 1, ARGB_BLACK, false, MAX_LIGHT_GLOWING, null);
				immediate.endBatch();
				poseStack.popPose();

				renderLiftDisplay(poseStack, submitNodeCollector, isFlat ? 0.4375F : 0.25F, text[0], text[1], state.lift.getLiftDirection());
			}
			poseStack.popPose();
		}
	}

	@Override
	public LiftPanelRenderState createRenderState() {
		return new LiftPanelRenderState();
	}

	@Override
	public void extractRenderState(T blockEntity, LiftPanelRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
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
		if (!isOdd && IBlock.getStatePropertySafe(blockState, IBlock.SIDE) == IBlock.EnumSide.RIGHT || isOdd && !IBlock.getStatePropertySafe(blockEntity.getBlockState(), ITripleBlock.ODD)) {
			state.shouldRender = false;
			return;
		}
		state.holdingLinker = Utilities.isHolding(Minecraft.getInstance().player, item -> item instanceof ItemLiftButtonsLinkModifier || Block.byItem(item) instanceof BlockLiftPanelBase);
		state.level = level; // TODO: Temp Expose
		state.facing = IBlock.getStatePropertySafe(blockState, HorizontalDirectionalBlock.FACING);
		state.linkedPosition = blockEntity.getTrackPosition(level);

		Lift lift = null;
		for (final Lift checkLift : ClientData.LIFTS) {
			if (checkLift.hasFloor(state.linkedPosition)) {
				lift = checkLift;
				break;
			}
		}
		state.lift = lift;
		state.shouldRender = state.linkedPosition != null && state.level.getBlockEntity(state.linkedPosition) != null;
	}

	private void renderLiftDisplay(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, float zOffset, String floorNumber, String floorDisplay, Lift.LiftDirection liftDirection) {
		poseStack.pushPose();
		poseStack.translate(0, 0, zOffset - SMALL_OFFSET * 2);

		final boolean noFloorNumber = floorNumber.isEmpty();
		final boolean noFloorDisplay = floorDisplay.isEmpty();
		final int lineCount = (noFloorNumber ? 0 : floorNumber.split("\\|").length) + (noFloorDisplay ? 0 : floorDisplay.split("\\|").length);
		final float lineHeight = 1F / lineCount;
		final double gameTick = MTRClient.getGameTick();
		final boolean goingUp = liftDirection == Lift.LiftDirection.UP;
		final float arrowSize = PANEL_WIDTH / 6;
		final float y = -arrowSize - 0.125F;

		// Arrow
		if (liftDirection != Lift.LiftDirection.NONE) {
			final float uv = (float)((gameTick * ARROW_SPEED) % 1);
			final int color = goingUp ? 0xFF00FF00 : 0xFFFF0000;
			submitNodeCollector.submitCustomGeometry(poseStack, MoreRenderLayers.getLight(ARROW_TEXTURE, false), (pose, vertexConsumer) -> {
				IDrawing.drawTexture(pose, vertexConsumer, -PANEL_WIDTH / 2 - arrowSize, y, arrowSize, arrowSize, 0, (goingUp ? 0 : 1) + uv, 1, (goingUp ? 1 : 0) + uv, Direction.UP, color, MAX_LIGHT_GLOWING);
				IDrawing.drawTexture(pose, vertexConsumer, PANEL_WIDTH / 2, y, arrowSize, arrowSize, 0, (goingUp ? 0 : 1) + uv, 1, (goingUp ? 1 : 0) + uv, Direction.UP, color, MAX_LIGHT_GLOWING);
			});
		}

		// Floor Display
		if (!noFloorNumber || !noFloorDisplay) {
			float uvOffset = 0;
			if (lineCount > 1) {
				uvOffset = (float) Math.floor((gameTick % (SLIDE_INTERVAL * lineCount)) / SLIDE_INTERVAL) * lineHeight;
				if ((gameTick % SLIDE_INTERVAL) > SLIDE_INTERVAL - SLIDE_TIME) {
					uvOffset += lineHeight * ((gameTick % SLIDE_INTERVAL) - SLIDE_INTERVAL + SLIDE_TIME) / SLIDE_TIME;
				}
			}
			final float uv = (goingUp ? -1 : 1) * uvOffset;
			final String text = String.format("%s%s%s", floorNumber, noFloorNumber || noFloorDisplay ? "" : "|", floorDisplay);

			submitNodeCollector.submitCustomGeometry(poseStack, MoreRenderLayers.getLight(ClientData.DATA_CACHE.getLiftPanelDisplay(text, 0xFFAA00).resourceLocation, false), (pose, vertexConsumer) -> {
				IDrawing.drawTexture(pose, vertexConsumer, -PANEL_WIDTH / 2, y, PANEL_WIDTH, arrowSize, 0, uv, 1, lineHeight + uv, Direction.UP, ARGB_WHITE, MAX_LIGHT_GLOWING);
			});
		}

		poseStack.popPose();
	}

	public static class LiftPanelRenderState extends BlockEntityRenderState {
		Level level;
		Direction facing;
		BlockPos linkedPosition;
		Lift lift;
		boolean holdingLinker;
		boolean shouldRender;
	}
}
