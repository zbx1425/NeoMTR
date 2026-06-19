package cn.zbx1425.mtrsteamloco.block;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.network.PacketScreen;
import cn.zbx1425.mtrsteamloco.render.scripting.eyecandy.EyeCandyScriptContext;
import cn.zbx1425.sowcer.math.Vector3f;
import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.BlockEntityClientSerializableMapper;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class BlockEyeCandy extends BlockDirectionalMapper implements EntityBlockMapper {

    public BlockEyeCandy(BlockBehaviour.Properties properties) {
        super(properties.strength(2).noCollision());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (player.getMainHandItem().is(mtr.Items.BRUSH.get())) {
            if (!level.isClientSide()) {
                PacketScreen.sendScreenBlockS2C((ServerPlayer) player, "eye_candy", pos);
            }
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public BlockEntityMapper createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BlockEntityEyeCandy(blockPos, blockState);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState blockState) {
        return RenderShape.INVISIBLE;
    }

    public static class BlockEntityEyeCandy extends BlockEntityClientSerializableMapper {

        public String prefabId = null;

        public float translateX = 0, translateY = 0, translateZ = 0;
        public float rotateX = 0, rotateY = 0, rotateZ = 0;

        public boolean fullLight = false;

        public EyeCandyScriptContext scriptContext = new EyeCandyScriptContext(this);

        public BlockEntityEyeCandy(BlockPos pos, BlockState state) {
            super(Main.BLOCK_ENTITY_TYPE_EYE_CANDY.get(), pos, state);
        }

        @Override
        public void readCompoundTag(ValueInput compoundTag) {
            prefabId = compoundTag.getStringOr("prefabId", "");
            if (StringUtils.isEmpty(prefabId)) prefabId = null;
            fullLight = compoundTag.getBooleanOr("fullLight", false);

            translateX = compoundTag.getFloatOr("translateX", 0);
            translateY = compoundTag.getFloatOr("translateY", 0);
            translateZ = compoundTag.getFloatOr("translateZ", 0);
            rotateX = compoundTag.getFloatOr("rotateX", 0);
            rotateY = compoundTag.getFloatOr("rotateY", 0);
            rotateZ = compoundTag.getFloatOr("rotateZ", 0);
        }

        @Override
        public void writeCompoundTag(ValueOutput compoundTag) {
            compoundTag.putString("prefabId", prefabId == null ? "" : prefabId);
            compoundTag.putBoolean("fullLight", fullLight);
            
            compoundTag.putFloat("translateX", translateX);
            compoundTag.putFloat("translateY", translateY);
            compoundTag.putFloat("translateZ", translateZ);
            compoundTag.putFloat("rotateX", rotateX);
            compoundTag.putFloat("rotateY", rotateY);
            compoundTag.putFloat("rotateZ", rotateZ);
        }

        public BlockPos getWorldPos() {
            return this.worldPosition;
        }

        public Vector3f getWorldPosVector3f() {
            return new Vector3f(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ());
        }
    }
}
