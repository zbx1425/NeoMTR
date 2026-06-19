package mtr.block;

import mtr.BlockEntityTypes;
import mtr.Items;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class BlockLiftDoorOdd extends BlockPSDAPGDoorBase implements ITripleBlock {

	public BlockLiftDoorOdd(Properties properties) {
		super(properties);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos posFrom, BlockState newState, RandomSource random) {
		return ITripleBlock.updateShape(state, direction, newState.is(this), () -> super.updateShape(state, world, ticks, pos, direction, posFrom, newState, random));
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		ITripleBlock.playerWillDestroy(world, pos, state, player, IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER);
		return super.playerWillDestroy(world, pos, state, player);
	}

	@Override
	public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityLiftDoorOdd(pos, state);
	}

	@Override
	public Item asItem() {
		return Items.LIFT_DOOR_ODD_1.get();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(END, FACING, HALF, ODD, SIDE, UNLOCKED);
	}

	public static class TileEntityLiftDoorOdd extends TileEntityPSDAPGDoorBase {

		public TileEntityLiftDoorOdd(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.LIFT_DOOR_ODD_1_TILE_ENTITY.get(), pos, state);
		}
	}
}
