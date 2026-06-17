package mtr.mixin;

import mtr.mappings.BlockEntityMapper;
import mtr.mappings.BlockEntityRendererMapper;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockEntityRendererMapper.class)
public abstract class BlockEntityRendererMapperMixin<T extends BlockEntityMapper, S extends BlockEntityRenderState> implements BlockEntityRenderer<T, S> {
    @Override
    public @NotNull AABB getRenderBoundingBox(@NotNull T blockEntity) {
        return AABB.INFINITE;
    }
}
