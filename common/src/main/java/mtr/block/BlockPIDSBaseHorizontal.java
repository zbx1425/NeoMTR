package mtr.block;

import mtr.data.IPIDS;
import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.EntityBlockMapper;
import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public abstract class BlockPIDSBaseHorizontal extends BlockDirectionalMapper implements EntityBlockMapper, IPIDS {

	public BlockPIDSBaseHorizontal(BlockBehaviour.Properties properties) {
		super(properties.requiresCorrectToolForDrops().strength(2).lightLevel(state -> 5));
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult blockHitResult) {
		return IBlock.checkHoldingBrush(world, player, () -> {
			final BlockPos otherPos = pos.relative(IBlock.getStatePropertySafe(state, FACING));
			final BlockEntity entity1 = world.getBlockEntity(pos);
			final BlockEntity entity2 = world.getBlockEntity(otherPos);

			if (entity1 instanceof TileEntityBlockPIDSBaseHorizontal && entity2 instanceof TileEntityBlockPIDSBaseHorizontal) {
				((TileEntityBlockPIDSBaseHorizontal) entity1).syncData();
				((TileEntityBlockPIDSBaseHorizontal) entity2).syncData();
				PacketTrainDataGuiServer.openPIDSConfigScreenS2C((ServerPlayer) player, pos, otherPos, ((TileEntityBlockPIDSBaseHorizontal) entity1).getMaxArrivals(), ((TileEntityBlockPIDSBaseHorizontal) entity1).getLinesPerArrival());
			}
		});
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos posFrom, BlockState newState, RandomSource random) {
		if (IBlock.getStatePropertySafe(state, FACING) == direction && !newState.is(this)) {
			return Blocks.AIR.defaultBlockState();
		} else {
			return state;
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		final Direction direction = ctx.getHorizontalDirection().getOpposite();
		return IBlock.isReplaceable(ctx, direction, 2) ? defaultBlockState().setValue(FACING, direction) : null;
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		if (facing == Direction.SOUTH || facing == Direction.WEST) {
			IBlock.onBreakCreative(world, player, pos.relative(facing));
		}
		return super.playerWillDestroy(world, pos, state, player);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
		if (!world.isClientSide()) {
			final Direction direction = IBlock.getStatePropertySafe(state, FACING);
			world.setBlock(pos.relative(direction), defaultBlockState().setValue(FACING, direction.getOpposite()), 3);
			world.updateNeighborsAt(pos, Blocks.AIR);
			state.updateNeighbourShapes(world, pos, 3);
			final BlockEntity entity1 = world.getBlockEntity(pos);
			final BlockEntity entity2 = world.getBlockEntity(pos.relative(direction));
			if (entity1 instanceof TileEntityBlockPIDSBaseHorizontal && entity2 instanceof TileEntityBlockPIDSBaseHorizontal) {
				System.arraycopy(((TileEntityBlockPIDSBaseHorizontal) entity1).messages, 0, ((TileEntityBlockPIDSBaseHorizontal) entity2).messages, 0, Math.min(((TileEntityBlockPIDSBaseHorizontal) entity1).messages.length, ((TileEntityBlockPIDSBaseHorizontal) entity2).messages.length));
			}
		}
	}

// FIXME
//	@Override
//	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
		// TODO Make it work (1.21.1 checks BlockEntityType at given BlockPos)
//		final BlockEntity blockEntity = createBlockEntity(new BlockPos(0, 0, 0), null);
//		if (blockEntity instanceof TileEntityPIDS) {
//			tooltip.add(Text.translatable("tooltip.mtr.arrivals", ((TileEntityPIDS) blockEntity).getMaxArrivals()).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
//		}
//	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	public abstract static class TileEntityBlockPIDSBaseHorizontal extends TileEntityPIDS {

		public TileEntityBlockPIDSBaseHorizontal(BlockEntityType<?> type, BlockPos pos, BlockState state) {
			super(type, pos, state);
		}

		@Override
		public int getLinesPerArrival() {
			return 1;
		}
	}
}
