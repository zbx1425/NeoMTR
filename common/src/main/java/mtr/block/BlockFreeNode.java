package mtr.block;

import mtr.BlockEntityTypes;
import mtr.data.EnumHelper;
import mtr.data.TransportMode;
import mtr.mappings.BlockEntityClientSerializableMapper;
import mtr.mappings.EntityBlockMapper;
import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

public class BlockFreeNode extends BlockNode implements EntityBlockMapper {

	public BlockFreeNode(BlockBehaviour.Properties properties) {
		super(properties, TransportMode.TRAIN, false);
	}

	public static TransportMode getEffectiveTransportMode(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos) instanceof TileEntityFreeNode) {
			return ((TileEntityFreeNode) world.getBlockEntity(pos)).getTransportMode();
		}
		final Block block = world.getBlockState(pos).getBlock();
		return block instanceof BlockNode ? ((BlockNode) block).transportMode : TransportMode.TRAIN;
	}

	/**
	 * Raw placement angle in degrees, or NaN when orientation is undetermined.
	 */
	public static float getRawAngleDegrees(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos) instanceof TileEntityFreeNode) {
			return ((TileEntityFreeNode) world.getBlockEntity(pos)).getAngleDegrees();
		}
		return Float.NaN;
	}

	public static boolean isFreeNodeUndetermined(Level world, BlockPos pos) {
		return world.getBlockEntity(pos) instanceof TileEntityFreeNode && ((TileEntityFreeNode) world.getBlockEntity(pos)).isUndetermined();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(IS_CONNECTED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState();
	}

	@Override
	public mtr.mappings.BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityFreeNode(pos, state);
	}

	@Override
	protected TransportMode getTransportModeForRemoval(Level world, BlockPos pos) {
		return getEffectiveTransportMode(world, pos);
	}

	public static class TileEntityFreeNode extends BlockEntityClientSerializableMapper {

		private static final String KEY_ANGLE = "angle_degrees";
		private static final String KEY_TRANSPORT_MODE = "transport_mode";

		private float angleDegrees = Float.NaN;
		private TransportMode transportMode = TransportMode.TRAIN;

		public TileEntityFreeNode(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.FREE_NODE_TILE_ENTITY.get(), pos, state);
		}

		public boolean isUndetermined() {
			return Float.isNaN(angleDegrees);
		}

		public float getAngleDegrees() {
			return angleDegrees;
		}

		public TransportMode getTransportMode() {
			return transportMode;
		}

		public void setAngleAndMode(float angleDegrees, TransportMode transportMode) {
			this.angleDegrees = angleDegrees;
			this.transportMode = transportMode;
			setChanged();
			syncData();
		}

		public void setUndeterminedWithMode(TransportMode transportMode) {
			this.angleDegrees = Float.NaN;
			this.transportMode = transportMode;
			setChanged();
			syncData();
		}

		@Override
		public void readCompoundTag(ValueInput compoundTag) {
			angleDegrees = compoundTag.getFloatOr(KEY_ANGLE, Float.NaN);
			transportMode = EnumHelper.valueOf(TransportMode.TRAIN, compoundTag.getStringOr(KEY_TRANSPORT_MODE, ""));
		}

		@Override
		public void writeCompoundTag(ValueOutput compoundTag) {
			if (!Float.isNaN(angleDegrees)) {
				compoundTag.putFloat(KEY_ANGLE, angleDegrees);
			} else {
				compoundTag.discard(KEY_ANGLE);
			}
			compoundTag.putString(KEY_TRANSPORT_MODE, transportMode.toString());
		}
	}
}
