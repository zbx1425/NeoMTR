package mtr.block;

import mtr.BlockEntityTypes;
import mtr.data.RailwayData;
import mtr.mappings.BlockEntityClientSerializableMapper;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

public class BlockLiftTrackFloor extends BlockLiftTrack implements EntityBlockMapper {

	public BlockLiftTrackFloor(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult blockHitResult) {
		return IBlock.checkHoldingBrush(world, player, () -> {
			final BlockEntity entity = world.getBlockEntity(pos);
			if (entity instanceof TileEntityLiftTrackFloor) {
				((TileEntityLiftTrackFloor) entity).syncData();
				PacketTrainDataGuiServer.openLiftTrackFloorScreenS2C((ServerPlayer) player, pos);
			}
		});
	}

	@Override
	public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityLiftTrackFloor(pos, state);
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		if (!world.isClientSide()) {
			final RailwayData railwayData = RailwayData.getInstance(world);
			if (railwayData != null) {
				railwayData.removeLiftFloorTrack(pos);
				PacketTrainDataGuiServer.removeLiftFloorTrackS2C(world, pos);
			}
		}
		return super.playerWillDestroy(world, pos, state, player);
	}

	public static class TileEntityLiftTrackFloor extends BlockEntityClientSerializableMapper {

		private String floorNumber = "1";
		private String floorDescription = "";
		private boolean shouldDing;
		private boolean disableCarCall;

		private static final String KEY_FLOOR_NUMBER = "floor_number";
		private static final String KEY_FLOOR_DESCRIPTION = "floor_description";
		private static final String KEY_SHOULD_DING = "should_ding";
		private static final String KEY_DISABLE_CAR_CALL = "disable_car_call";

		public TileEntityLiftTrackFloor(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.LIFT_TRACK_FLOOR_1_TILE_ENTITY.get(), pos, state);
		}

		@Override
		public void readCompoundTag(ValueInput compoundTag) {
			floorNumber = compoundTag.getStringOr(KEY_FLOOR_NUMBER, "");
			floorDescription = compoundTag.getStringOr(KEY_FLOOR_DESCRIPTION, "");
			shouldDing = compoundTag.getBooleanOr(KEY_SHOULD_DING, false);
			disableCarCall = compoundTag.getBooleanOr(KEY_DISABLE_CAR_CALL, false);
		}

		@Override
		public void writeCompoundTag(ValueOutput compoundTag) {
			compoundTag.putString(KEY_FLOOR_NUMBER, floorNumber);
			compoundTag.putString(KEY_FLOOR_DESCRIPTION, floorDescription);
			compoundTag.putBoolean(KEY_SHOULD_DING, shouldDing);
			compoundTag.putBoolean(KEY_DISABLE_CAR_CALL, disableCarCall);
		}

		public void setData(String floorNumber, String floorDescription, boolean shouldDing, boolean disableCarCall) {
			this.floorNumber = floorNumber;
			this.floorDescription = floorDescription;
			this.shouldDing = shouldDing;
			this.disableCarCall = disableCarCall;
			setChanged();
			syncData();
		}

		public String getFloorNumber() {
			return floorNumber;
		}

		public String getFloorDescription() {
			return floorDescription;
		}

		public boolean getShouldDing() {
			return shouldDing;
		}

		public boolean getDisableCarCall() { return disableCarCall; }
	}
}
