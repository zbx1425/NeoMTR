package mtr.mappings;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public abstract class BlockEntityRendererMapper<T extends BlockEntityMapper, S extends BlockEntityRenderState> implements BlockEntityRenderer<T, S> {

	public BlockEntityRendererMapper(BlockEntityRenderDispatcher dispatcher) {
	}
}
