package mtr.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public abstract class BlockStationNameTallBase extends BlockStationNameBase implements IBlock {

	public static final BooleanProperty METAL = BooleanProperty.create("metal");

	public BlockStationNameTallBase(BlockBehaviour.Properties properties) {
		super(properties.requiresCorrectToolForDrops().strength(2).noOcclusion());
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult blockHitResult) {
		return IBlock.checkHoldingBrush(world, player, () -> {
			final boolean isWhite = IBlock.getStatePropertySafe(state, COLOR) == 0;
			final int newColorProperty = isWhite ? 2 : 0;
			final boolean newMetalProperty = isWhite == IBlock.getStatePropertySafe(state, METAL);

			updateProperties(world, pos, newMetalProperty, newColorProperty);
			switch (IBlock.getStatePropertySafe(state, THIRD)) {
				case LOWER:
					updateProperties(world, pos.above(), newMetalProperty, newColorProperty);
					updateProperties(world, pos.above(2), newMetalProperty, newColorProperty);
					break;
				case MIDDLE:
					updateProperties(world, pos.below(), newMetalProperty, newColorProperty);
					updateProperties(world, pos.above(), newMetalProperty, newColorProperty);
					break;
				case UPPER:
					updateProperties(world, pos.below(), newMetalProperty, newColorProperty);
					updateProperties(world, pos.below(2), newMetalProperty, newColorProperty);
					break;
			}
		});
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos posFrom, BlockState newState, RandomSource random) {
		if ((direction == Direction.UP && IBlock.getStatePropertySafe(state, THIRD) != EnumThird.UPPER || direction == Direction.DOWN && IBlock.getStatePropertySafe(state, THIRD) != EnumThird.LOWER) && !newState.is(this)) {
			return Blocks.AIR.defaultBlockState();
		} else {
			return state;
		}
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		switch (IBlock.getStatePropertySafe(state, THIRD)) {
			case MIDDLE:
				IBlock.onBreakCreative(world, player, pos.below());
				break;
			case UPPER:
				IBlock.onBreakCreative(world, player, pos.below(2));
				break;
		}
		return super.playerWillDestroy(world, pos, state, player);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
		if (!world.isClientSide()) {
			final Direction facing = IBlock.getStatePropertySafe(state, FACING);
			world.setBlock(pos.above(), defaultBlockState().setValue(FACING, facing).setValue(METAL, true).setValue(THIRD, EnumThird.MIDDLE), 3);
			world.setBlock(pos.above(2), defaultBlockState().setValue(FACING, facing).setValue(METAL, true).setValue(THIRD, EnumThird.UPPER), 3);
			world.updateNeighborsAt(pos, Blocks.AIR);
			state.updateNeighbourShapes(world, pos, 3);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(COLOR, FACING, METAL, THIRD);
	}

	protected static Tuple<Integer, Integer> getBounds(BlockState state) {
		final EnumThird third = IBlock.getStatePropertySafe(state, THIRD);
		final int start, end;
        end = switch (third) {
            case LOWER -> {
                start = 10;
                yield 16;
            }
            case UPPER -> {
                start = 0;
                yield 8;
            }
            default -> {
                start = 0;
                yield 16;
            }
        };
		return new Tuple<>(start, end);
	}

	private static void updateProperties(Level world, BlockPos pos, boolean metalProperty, int colorProperty) {
		world.setBlockAndUpdate(pos, world.getBlockState(pos).setValue(COLOR, colorProperty).setValue(METAL, metalProperty));
	}

	public static class TileEntityStationNameTallBase extends TileEntityStationNameBase {

		public TileEntityStationNameTallBase(BlockEntityType<?> type, BlockPos pos, BlockState state, float zOffset, boolean isDoubleSided) {
			super(type, pos, state, 0.21875F, zOffset, isDoubleSided);
		}

		@Override
		public int getColor(BlockState state) {
            return switch (IBlock.getStatePropertySafe(state, BlockStationNameBase.COLOR)) {
                case 1 -> ARGB_LIGHT_GRAY;
                case 2 -> ARGB_BLACK;
                default -> ARGB_WHITE;
            };
		}
	}
}
