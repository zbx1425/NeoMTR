package mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.IDrawing;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;

public class RenderSignalLight2Aspect<T extends BlockEntityMapper> extends RenderSignalBase<T, RenderSignalBase.SignalBaseRenderState> {

	private final boolean redOnTop;
	private final int proceedColor;

	public RenderSignalLight2Aspect(BlockEntityRenderDispatcher dispatcher, boolean isSingleSided, boolean redOnTop, int proceedColor) {
		super(dispatcher, isSingleSided, 2);
		this.redOnTop = redOnTop;
		this.proceedColor = proceedColor;
	}

	@Override
	protected void drawSignal(SignalBaseRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, RenderType renderType, Direction facing, int occupiedAspect, boolean isBackSide) {
		final float y = occupiedAspect > 0 == redOnTop ? 0.4375F : 0.0625F;
		submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
			IDrawing.drawTexture(pose, vertexConsumer, -0.125F, y, -0.19375F, 0.125F, y + 0.25F, -0.19375F, facing.getOpposite(), occupiedAspect > 0 ? 0xFFFF0000 : proceedColor, MAX_LIGHT_GLOWING);
		});
	}

	@Override
	public SignalBaseRenderState createRenderState() {
		return new SignalBaseRenderState();
	}
}
