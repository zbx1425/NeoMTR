package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import mtr.block.BlockFreeNode;
import mtr.block.BlockNode;
import mtr.block.IBlock;
import mtr.client.IDrawing;
import mtr.mappings.BlockEntityRendererMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RenderFreeNode extends BlockEntityRendererMapper<BlockFreeNode.TileEntityFreeNode> {

	public RenderFreeNode(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(BlockFreeNode.TileEntityFreeNode entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
		final Level world = entity.getLevel();
		if (world == null) {
			return;
		}

		final BlockState state = world.getBlockState(entity.getBlockPos());
		if (!(state.getBlock() instanceof BlockFreeNode)) {
			return;
		}

		final Player player = Minecraft.getInstance().player;
		if (player == null || !RenderTrains.isHoldingRailRelated(player)) {
			return;
		}

		matrices.pushPose();
		final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(MoreRenderLayers.getExterior(Identifier.parse("textures/block/oak_log.png")));

		final float angle = entity.getAngleDegrees();
		if (Float.isNaN(angle)) {
			// Undetermined: thin vertical pole
			IDrawing.drawTexture(matrices, vertexConsumer, 0.45F, 0.25F, 0.45F, 0.55F, 0.25F, 0.55F, 0.45F, 1, 0.45F, 0.55F, 1, 0.55F, 0.45F, 0.45F, 0.55F, 0.55F, Direction.UP, -1, light);
		} else {
			matrices.translate(0.5, 0.125, 0.5);
			matrices.mulPose(Axis.YP.rotationDegrees(-angle));
			matrices.translate(-0.5, -0.125, -0.5);
			final float y0 = IBlock.getStatePropertySafe(state, BlockNode.IS_CONNECTED) ? 0.05F : 0.15F;
			IDrawing.drawTexture(matrices, vertexConsumer, 0.2F, y0, 0.45F, 0.8F, y0 + 0.1F, 0.55F, 0.2F, y0 + 0.1F, 0.55F, 0.8F, y0, 0.45F, 0.2F, 0.45F, 0.8F, 0.55F, Direction.UP, -1, light);
		}

		matrices.popPose();
	}
}
