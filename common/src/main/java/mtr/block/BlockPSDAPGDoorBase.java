package mtr.block;

import mtr.MTRClient;
import mtr.data.IGui;
import mtr.mappings.BlockEntityClientSerializableMapper;
import mtr.mappings.EntityBlockMapper;
import mtr.mappings.Text;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BlockPSDAPGDoorBase extends BlockPSDAPGBase implements EntityBlockMapper {

	public static final int MAX_OPEN_VALUE = 32;

	public static final BooleanProperty END = BooleanProperty.create("end");
	public static final BooleanProperty UNLOCKED = BooleanProperty.create("unlocked");

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos posFrom, BlockState newState, RandomSource random) {
		if (IBlock.getSideDirection(state) == direction && !newState.is(this)) {
			return Blocks.AIR.defaultBlockState();
		} else {
			final BlockState superState = super.updateShape(state, world, ticks, pos, direction, posFrom, newState, random);
			if (superState.getBlock() == Blocks.AIR) {
				return superState;
			} else {
				final boolean end = world.getBlockState(pos.relative(IBlock.getSideDirection(state).getOpposite())).getBlock() instanceof BlockPSDAPGGlassEndBase;
				return superState.setValue(END, end);
			}
		}
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		BlockPos offsetPos = pos;
		if (IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER) {
			offsetPos = offsetPos.below();
		}
		if (IBlock.getStatePropertySafe(state, SIDE) == EnumSide.RIGHT) {
			offsetPos = offsetPos.relative(IBlock.getSideDirection(state));
		}
		IBlock.onBreakCreative(world, player, offsetPos);
		return super.playerWillDestroy(world, pos, state, player);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult blockHitResult) {
		return IBlock.checkHoldingBrush(world, player, () -> {
			final boolean unlocked = IBlock.getStatePropertySafe(state, UNLOCKED);
			for (int y = -1; y <= 1; y++) {
				final BlockState scanState = world.getBlockState(pos.above(y));
				if (state.is(scanState.getBlock())) {
					lockDoor(world, pos.above(y), scanState, !unlocked);
				}
			}
			player.sendOverlayMessage(!unlocked ? Text.translatable("gui.mtr.psd_apg_door_unlocked") : Text.translatable("gui.mtr.psd_apg_door_locked"));
		});
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext collisionContext) {
		if (!(world instanceof Level level && level.isClientSide())) return Shapes.empty();
		final BlockEntity entity = world.getBlockEntity(pos);
		return entity instanceof TileEntityPSDAPGDoorBase && ((TileEntityPSDAPGDoorBase) entity).isOpen() ? Shapes.empty() : super.getCollisionShape(state, world, pos, collisionContext);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(END, FACING, HALF, SIDE, UNLOCKED);
	}

	private static void lockDoor(Level world, BlockPos pos, BlockState state, boolean unlocked) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		final BlockPos leftPos = pos.relative(facing.getCounterClockWise());
		final BlockPos rightPos = pos.relative(facing.getClockWise());
		final BlockState leftState = world.getBlockState(leftPos);
		final BlockState rightState = world.getBlockState(rightPos);

		if (leftState.is(state.getBlock())) {
			final BlockState toggled = leftState.setValue(UNLOCKED, unlocked);
			world.setBlockAndUpdate(leftPos, toggled);
		}

		if (rightState.is(state.getBlock())) {
			final BlockState toggled = rightState.setValue(UNLOCKED, unlocked);
			world.setBlockAndUpdate(rightPos, toggled);
		}

		world.setBlockAndUpdate(pos, state.setValue(UNLOCKED, unlocked));
	}

	public static abstract class TileEntityPSDAPGDoorBase extends BlockEntityClientSerializableMapper implements IGui {

		private float open;
		private double openCooldownTick;

		public TileEntityPSDAPGDoorBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
			super(type, pos, state);
		}

		public AABB getRenderBoundingBox() {
			return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
		}

		public void setOpen(float open) {
			this.open = open;
			Client.setCooldown(this);
		}

		public float getOpen(float lastFrameDuration) {
			if (Client.isCooldownExpired(this)) {
				open = 0;
			}
			return open;
		}

		public boolean isOpen() {
			return open > 0;
		}

		private static class Client {

			public static void setCooldown(TileEntityPSDAPGDoorBase door) {
				door.openCooldownTick = MTRClient.getGameTick();
			}

			public static boolean isCooldownExpired(TileEntityPSDAPGDoorBase door) {
				return MTRClient.getGameTick() - door.openCooldownTick > 10;
			}
		}
	}
}