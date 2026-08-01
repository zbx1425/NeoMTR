package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.MTRClient;
import mtr.block.*;
import mtr.data.IGui;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.ModelDataWrapper;
import mtr.mappings.ModelMapper;
import mtr.util.UtilitiesClient;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderPSDAPGDoor<T extends BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase> extends BlockEntityRendererMapper<T, RenderPSDAPGDoor.PSDAPGDoorRenderState> implements IGui, IBlock {

	private final int type;
	private static final ModelSingleCube MODEL_PSD = new ModelSingleCube(36, 18, 0, 0, 0, 16, 16, 2);
	private static final ModelSingleCube MODEL_PSD_END_LEFT_1 = new ModelSingleCube(20, 18, 0, 0, 0, 8, 16, 2);
	private static final ModelSingleCube MODEL_PSD_END_RIGHT_1 = new ModelSingleCube(20, 18, 8, 0, 0, 8, 16, 2);
	private static final ModelSingleCube MODEL_PSD_END_LEFT_2 = new ModelSingleCube(20, 18, 8, 0, 2, 8, 16, 2);
	private static final ModelSingleCube MODEL_PSD_END_RIGHT_2 = new ModelSingleCube(20, 18, 0, 0, 2, 8, 16, 2);
	private static final ModelSingleCube MODEL_PSD_LIGHT_LEFT = new ModelSingleCube(16, 16, 0, -1, 5, 1, 1, 1);
	private static final ModelSingleCube MODEL_PSD_LIGHT_RIGHT = new ModelSingleCube(16, 16, 15, -1, 5, 1, 1, 1);
	private static final ModelSingleCube MODEL_APG_TOP = new ModelSingleCube(34, 9, 0, 8, 1, 16, 8, 1);
	private static final ModelAPGDoorBottom MODEL_APG_BOTTOM = new ModelAPGDoorBottom();
	private static final ModelAPGDoorLight MODEL_APG_LIGHT = new ModelAPGDoorLight();
	private static final ModelSingleCube MODEL_APG_DOOR_LOCKED = new ModelSingleCube(6, 6, 5, 10, 1, 6, 6, 0);
	private static final ModelSingleCube MODEL_PSD_DOOR_LOCKED = new ModelSingleCube(6, 6, 5, 6, 1, 6, 6, 0);
	private static final ModelSingleCube MODEL_LIFT_LEFT = new ModelSingleCube(28, 18, 0, 0, 0, 12, 16, 2);
	private static final ModelSingleCube MODEL_LIFT_RIGHT = new ModelSingleCube(28, 18, 4, 0, 0, 12, 16, 2);

	public RenderPSDAPGDoor(BlockEntityRenderDispatcher dispatcher, int type) {
		super(dispatcher);
		this.type = type;
	}

	@Override
	public PSDAPGDoorRenderState createRenderState() {
		return new PSDAPGDoorRenderState();
	}

	@Override
	public void extractRenderState(T blockEntity, PSDAPGDoorRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		final BlockState blockState = blockEntity.getBlockState();
		final BlockPos pos = blockEntity.getBlockPos();
		final Level level = blockEntity.getLevel();

		state.facing = IBlock.getStatePropertySafe(blockState, BlockPSDAPGDoorBase.FACING);
		state.side = IBlock.getStatePropertySafe(blockState, BlockPSDAPGDoorBase.SIDE) == EnumSide.RIGHT;
		state.half = IBlock.getStatePropertySafe(blockState, BlockPSDAPGDoorBase.HALF) == DoubleBlockHalf.UPPER;
		state.end = IBlock.getStatePropertySafe(blockState, BlockPSDAPGDoorBase.END);
		state.unlocked = IBlock.getStatePropertySafe(blockState, BlockPSDAPGDoorBase.UNLOCKED);
		state.open = Math.min(blockEntity.getOpen(MTRClient.getLastFrameDuration()), type >= 3 ? 0.75F : 1);
		state.isOdd = IBlock.getStatePropertySafe(blockState, ITripleBlock.ODD);
		final Block nearbyBlock = level == null ? null : level.getBlockState(pos.relative(state.side ? state.facing.getClockWise() : state.facing.getCounterClockWise())).getBlock();
		state.nearbyBlockIsAPG = nearbyBlock instanceof BlockAPGGlass || nearbyBlock instanceof BlockAPGGlassEnd;
	}

	@Override
	public void submit(PSDAPGDoorRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		final StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations();
		storedMatrixTransformations.add(matricesNew -> {
			matricesNew.translate(0.5 + state.blockPos.getX(), state.blockPos.getY(), 0.5 + state.blockPos.getZ());
			UtilitiesClient.rotateYDegrees(matricesNew, -state.facing.toYRot());
			UtilitiesClient.rotateXDegrees(matricesNew, 180);
		});
		final StoredMatrixTransformations storedMatrixTransformationsLight = storedMatrixTransformations.copy();

		switch (type) {
			case 0:
			case 1:
				if (state.half) {
					RenderTrains.scheduleRender(Identifier.parse(String.format("mtr:textures/block/light_%s.png", state.open > 0 ? "on" : "off")), false, state.open > 0 ? RenderTrains.QueuedRenderLayer.LIGHT : RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
						storedMatrixTransformationsLight.transform(matricesNew);
						(state.side ? MODEL_PSD_LIGHT_RIGHT : MODEL_PSD_LIGHT_LEFT).renderToBuffer(matricesNew, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
						matricesNew.popPose();
					});
				}
				if (state.end) {
					RenderTrains.scheduleRender(Identifier.parse(String.format("mtr:textures/vehicle/psd_apg_door/psd_door_end_%s_%s_2_%s.png", state.half ? "top" : "bottom", state.side ? "right" : "left", type == 1 ? "2" : "1")), false, RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
						storedMatrixTransformationsLight.transform(matricesNew);
						matricesNew.translate(state.open / 2 * (state.side ? -1 : 1), 0, 0);
						(state.side ? MODEL_PSD_END_RIGHT_2 : MODEL_PSD_END_LEFT_2).renderToBuffer(matricesNew, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
						matricesNew.popPose();
					});
				}
				break;
			case 2:
				if (state.half && state.nearbyBlockIsAPG) {
					RenderTrains.scheduleRender(Identifier.parse(String.format("mtr:textures/block/apg_door_light_%s.png", state.open > 0 ? "on" : "off")), false, state.open > 0 ? RenderTrains.QueuedRenderLayer.LIGHT_TRANSLUCENT : RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
						storedMatrixTransformationsLight.transform(matricesNew);
						matricesNew.translate(state.side ? -0.515625 : 0.515625, 0, 0);
						matricesNew.scale(0.5F, 1, 1);
						MODEL_APG_LIGHT.renderToBuffer(matricesNew, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
						matricesNew.popPose();
					});
				}
				break;
		}

		storedMatrixTransformations.add(matricesNew -> matricesNew.translate(state.open * (state.side ? -1 : 1), 0, 0));

		switch (type) {
			case 0:
			case 1:
				if (state.end) {
					RenderTrains.scheduleRender(Identifier.parse(String.format("mtr:textures/vehicle/psd_apg_door/psd_door_end_%s_%s_1_%s.png", state.half ? "top" : "bottom", state.side ? "right" : "left", type == 1 ? "2" : "1")), false, RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
						storedMatrixTransformations.transform(matricesNew);
						(state.side ? MODEL_PSD_END_RIGHT_1 : MODEL_PSD_END_LEFT_1).renderToBuffer(matricesNew, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
						matricesNew.popPose();
					});
				} else {
					RenderTrains.scheduleRender(Identifier.parse(String.format("mtr:textures/vehicle/psd_apg_door/psd_door_%s_%s_%s.png", state.half ? "top" : "bottom", state.side ? "right" : "left", type == 1 ? "2" : "1")), false, RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
						storedMatrixTransformations.transform(matricesNew);
						MODEL_PSD.renderToBuffer(matricesNew, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
						matricesNew.popPose();
					});
				}
				if (state.half && !state.unlocked) {
					RenderTrains.scheduleRender(Identifier.parse("mtr:textures/block/sign/door_not_in_use.png"), false, RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
						storedMatrixTransformations.transform(matricesNew);
						if (state.end) {
							matricesNew.translate(state.side ? 0.25 : -0.25, 0, 0);
						}
						MODEL_PSD_DOOR_LOCKED.renderToBuffer(matricesNew, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
						matricesNew.popPose();
					});
				}
				break;
			case 2:
				RenderTrains.scheduleRender(Identifier.parse(String.format("mtr:textures/vehicle/psd_apg_door/apg_door_%s_%s.png", state.half ? "top" : "bottom", state.side ? "right" : "left")), false, RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
					storedMatrixTransformations.transform(matricesNew);
					(state.half ? MODEL_APG_TOP : MODEL_APG_BOTTOM).renderToBuffer(matricesNew, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
					matricesNew.popPose();
				});
				if (state.half && !state.unlocked) {
					RenderTrains.scheduleRender(Identifier.parse("mtr:textures/block/sign/door_not_in_use.png"), false, RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
						storedMatrixTransformations.transform(matricesNew);
						MODEL_APG_DOOR_LOCKED.renderToBuffer(matricesNew, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
						matricesNew.popPose();
					});
				}
				break;
			case 4:
				if (state.isOdd) {
					break;
				}
				storedMatrixTransformations.add(matricesNew -> matricesNew.translate(state.side ? 0.5 : -0.5, 0, 0));
			case 3:
				RenderTrains.scheduleRender(Identifier.parse(String.format("mtr:textures/vehicle/psd_apg_door/lift_door_%s_%s_1.png", state.half ? "top" : "bottom", state.side ? "right" : "left")), false, RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
					storedMatrixTransformations.transform(matricesNew);
					(state.side ? MODEL_LIFT_RIGHT : MODEL_LIFT_LEFT).renderToBuffer(matricesNew, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
					matricesNew.popPose();
				});
				if (state.half && !state.unlocked) {
					RenderTrains.scheduleRender(Identifier.parse("mtr:textures/block/sign/door_not_in_use.png"), false, RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
						storedMatrixTransformations.transform(matricesNew);
						matricesNew.translate(state.side ? 0.125 : -0.125, 0, 0);
						MODEL_PSD_DOOR_LOCKED.renderToBuffer(matricesNew, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
						matricesNew.popPose();
					});
				}
				break;
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	public static class PSDAPGDoorRenderState extends BlockEntityRenderState {
		Direction facing;
		boolean side;
		boolean half;
		boolean end;
		boolean unlocked;
		float open;
		boolean isOdd;
		boolean nearbyBlockIsAPG;
	}

	private static class ModelSingleCube extends EntityModel<EntityRenderState> {

		private ModelSingleCube(int textureWidth, int textureHeight, int x, int y, int z, int length, int height, int depth) {
			ModelDataWrapper modelDataWrapper = new ModelDataWrapper();
			ModelMapper cube = new ModelMapper(modelDataWrapper);
			cube.texOffs(0, 0).addBox(x - 8, y - 16, z - 8, length, height, depth, 0, false);
			modelDataWrapper.setModelPart(textureWidth, textureHeight);
			cube.setModelPart();
			super(modelDataWrapper.modelPart);
		}
	}

	private static class ModelAPGDoorBottom extends EntityModel<EntityRenderState> {
		private ModelAPGDoorBottom() {

            final int textureWidth = 34;
			final int textureHeight = 27;

			final ModelDataWrapper modelDataWrapper = new ModelDataWrapper();

			ModelMapper bone = new ModelMapper(modelDataWrapper);
			bone.texOffs(0, 0).addBox(-8, -16, -7, 16, 16, 1, 0, false);
			bone.texOffs(0, 17).addBox(-8, -6, -8, 16, 6, 1, 0, false);

			final ModelMapper cube_r1 = new ModelMapper(modelDataWrapper);
			cube_r1.setPos(0, -6, -8);
			bone.addChild(cube_r1);
			cube_r1.setRotationAngle(-0.7854F, 0, 0);
			cube_r1.texOffs(0, 24).addBox(-8, -2, 0, 16, 2, 1, 0, false);

			modelDataWrapper.setModelPart(textureWidth, textureHeight);
			bone.setModelPart();
			super(modelDataWrapper.modelPart);
		}
	}

	private static class ModelAPGDoorLight extends EntityModel<EntityRenderState> {

		private ModelAPGDoorLight() {
            final int textureWidth = 8;
			final int textureHeight = 8;

			final ModelDataWrapper modelDataWrapper = new ModelDataWrapper();

			ModelMapper bone = new ModelMapper(modelDataWrapper);
			bone.texOffs(0, 4).addBox(-0.5F, -9, -7, 1, 1, 3, 0.05F, false);

			final ModelMapper cube_r1 = new ModelMapper(modelDataWrapper);
			cube_r1.setPos(0, -9.05F, -4.95F);
			bone.addChild(cube_r1);
			cube_r1.setRotationAngle(0.3927F, 0, 0);
			cube_r1.texOffs(0, 0).addBox(-0.5F, 0.05F, -3.05F, 1, 1, 3, 0.05F, false);

			modelDataWrapper.setModelPart(textureWidth, textureHeight);
			bone.setModelPart();
            super(modelDataWrapper.modelPart);
		}
	}
}
