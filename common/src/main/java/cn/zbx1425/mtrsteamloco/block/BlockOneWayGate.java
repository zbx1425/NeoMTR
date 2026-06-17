package cn.zbx1425.mtrsteamloco.block;

import mtr.SoundEvents;
import mtr.block.IBlock;
import mtr.data.TicketSystem;
import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.Text;
import mtr.mappings.Utilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockOneWayGate extends BlockDirectionalMapper {

    public static final EnumProperty<TicketSystem.EnumTicketBarrierOpen> OPEN = EnumProperty.create("open", TicketSystem.EnumTicketBarrierOpen.class);

    public BlockOneWayGate() {
        super(Properties.of().requiresCorrectToolForDrops().strength(2).lightLevel(state -> 5).noOcclusion());
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!world.isClientSide() && entity instanceof Player player) {
            final Direction facing = IBlock.getStatePropertySafe(state, FACING);
            final Vec3 playerPosRotated = entity.position().subtract(pos.getX() + 0.5, 0, pos.getZ() + 0.5).yRot((float) Math.toRadians(facing.toYRot()));
            final float deltaFacing = Mth.wrapDegrees(entity.getYHeadRot() - facing.toYRot() + 180f);
            final TicketSystem.EnumTicketBarrierOpen open = IBlock.getStatePropertySafe(state, OPEN);

            if (playerPosRotated.z > 0) {
                if (open.isOpen()) {
                    world.setBlockAndUpdate(pos, state.setValue(OPEN, TicketSystem.EnumTicketBarrierOpen.CLOSED));
                }
                if (deltaFacing > -45 && deltaFacing < 45) {
                    player.sendOverlayMessage(Text.translatable("gui.mtrsteamloco.one_way_gate.wrong_way_pass"));
                }
            } else {
                if (!open.isOpen()) {
                    world.playSound(null, pos, SoundEvents.TICKET_BARRIER_CONCESSIONARY, SoundSource.BLOCKS, 1, 1);
                    world.setBlockAndUpdate(pos, state.setValue(OPEN, TicketSystem.EnumTicketBarrierOpen.OPEN_CONCESSIONARY));
                    if (!world.getBlockTicks().hasScheduledTick(pos, this)) {
                        Utilities.scheduleBlockTick(world, pos, this, 40);
                    }
                }
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos) {
        world.setBlockAndUpdate(pos, state.setValue(OPEN, TicketSystem.EnumTicketBarrierOpen.CLOSED));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection()).setValue(OPEN, TicketSystem.EnumTicketBarrierOpen.CLOSED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
        final Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return IBlock.getVoxelShapeByDirection(12, 0, 0, 16, 15, 16, facing);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        final Direction facing = IBlock.getStatePropertySafe(state, FACING);
        final TicketSystem.EnumTicketBarrierOpen open = IBlock.getStatePropertySafe(state, OPEN);
        final VoxelShape base = IBlock.getVoxelShapeByDirection(15, 0, 0, 16, 24, 16, facing);
        return open.isOpen() ? base : Shapes.or(IBlock.getVoxelShapeByDirection(0, 0, 7, 16, 24, 9, facing), base);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }
}
