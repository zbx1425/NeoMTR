package mtr.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockEscalatorSide extends BlockEscalatorBase {

	public BlockEscalatorSide(Properties properties) {
		super(properties);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos posFrom, BlockState newState, RandomSource random) {
		if (direction == Direction.DOWN && !(world.getBlockState(pos.below()).getBlock() instanceof BlockEscalatorStep)) {
			return Blocks.AIR.defaultBlockState();
		} else {
			return super.updateShape(state, world, ticks, pos, direction, posFrom, newState, random);
		}
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.join(getShape(state, world, pos, context), super.getCollisionShape(state, world, pos, context), BooleanOp.AND);
	}

	@Override
	public VoxelShape getVisualShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
		return Shapes.empty();
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		BlockPos offsetPos = pos.below();
		if (IBlock.getStatePropertySafe(state, SIDE) == EnumSide.RIGHT) {
			offsetPos = offsetPos.relative(IBlock.getSideDirection(state));
		}
		IBlock.onBreakCreative(world, player, offsetPos);
		return super.playerWillDestroy(world, pos, state, player);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext collisionContext) {
		final EnumEscalatorOrientation orientation = getOrientation(world, pos, state);
		final boolean isBottom = orientation == EnumEscalatorOrientation.LANDING_BOTTOM;
		final boolean isTop = orientation == EnumEscalatorOrientation.LANDING_TOP;
		final boolean isRight = IBlock.getStatePropertySafe(state, SIDE) == EnumSide.RIGHT;
		return IBlock.getVoxelShapeByDirection(isRight ? 12 : 0, 0, isTop ? 8 : 0, isRight ? 16 : 4, 16, isBottom ? 8 : 16, IBlock.getStatePropertySafe(state, FACING));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ORIENTATION, SIDE);
	}
}
