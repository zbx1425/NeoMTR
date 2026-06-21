package mtr.item;

import mtr.CreativeModeTabs;
import mtr.block.BlockFreeNode;
import mtr.block.BlockNode;
import mtr.data.RailAngle;
import mtr.data.RailCalculator;
import mtr.data.RailwayData;
import mtr.data.TransportMode;
import mtr.mappings.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class ItemNodeModifierBase extends ItemBlockClickingBase {

	public final boolean forNonContinuousMovementNode;
	public final boolean forContinuousMovementNode;
	public final boolean forAirplaneNode;
	protected final boolean isConnector;

	public static final String TAG_POS = "pos";
	private static final String TAG_TRANSPORT_MODE = "transport_mode";

	private static final ThreadLocal<Map<BlockPos, Float>> FREE_NODE_PENDING_RAW_ANGLES = ThreadLocal.withInitial(HashMap::new);

	public ItemNodeModifierBase(Item.Properties properties, boolean forNonContinuousMovementNode, boolean forContinuousMovementNode, boolean forAirplaneNode, boolean isConnector) {
		super(properties, CreativeModeTabs.CORE, propModifier -> propModifier.stacksTo(1));
		this.forNonContinuousMovementNode = forNonContinuousMovementNode;
		this.forContinuousMovementNode = forContinuousMovementNode;
		this.forAirplaneNode = forAirplaneNode;
		this.isConnector = isConnector;
	}

	public static void applyQueuedFreeNodeAngles(Level world) {
		for (final Map.Entry<BlockPos, Float> entry : FREE_NODE_PENDING_RAW_ANGLES.get().entrySet()) {
			final BlockEntity blockEntity = world.getBlockEntity(entry.getKey());
			if (blockEntity instanceof BlockFreeNode.TileEntityFreeNode tileEntityFreeNode) {
                tileEntityFreeNode.setAngleAndMode(entry.getValue(), tileEntityFreeNode.getTransportMode());
			}
		}
		FREE_NODE_PENDING_RAW_ANGLES.get().clear();
	}

	public static void clearQueuedFreeNodeAngles() {
		FREE_NODE_PENDING_RAW_ANGLES.get().clear();
	}

	private static void queueFreeNodeRawAngle(BlockPos pos, float rawDegrees) {
		FREE_NODE_PENDING_RAW_ANGLES.get().put(pos, rawDegrees);
	}

	private static float getRawAngleDegrees(Level world, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof BlockFreeNode) {
			return BlockFreeNode.getRawAngleDegrees(world, pos);
		}
		return BlockNode.getAngle(state);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
		final CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		final CompoundTag compoundTag = customData.copyTag();
		final long posLong = compoundTag.getLongOr(TAG_POS, 0);
		if (posLong != 0) {
			tooltipAdder.accept(Text.translatable("tooltip.mtr.selected_block", BlockPos.of(posLong).toShortString()).setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
		}
	}

	@Override
	protected void onStartClick(UseOnContext context, CompoundTag compoundTag) {
		compoundTag.putString(TAG_TRANSPORT_MODE, BlockFreeNode.getEffectiveTransportMode(context.getLevel(), context.getClickedPos()).toString());
	}

	@Override
	protected void onEndClick(UseOnContext context, BlockPos posEnd, CompoundTag compoundTag) {
		final Level world = context.getLevel();
		final RailwayData railwayData = RailwayData.getInstance(world);
		final BlockPos posStart = context.getClickedPos();
		final BlockState stateStart = world.getBlockState(posStart);
		final Block blockStart = stateStart.getBlock();
		final BlockState stateEnd = world.getBlockState(posEnd);

		final TransportMode transportModeStart = BlockFreeNode.getEffectiveTransportMode(world, posStart);
		final TransportMode transportModeEnd = BlockFreeNode.getEffectiveTransportMode(world, posEnd);

		try {
			if (railwayData != null && stateEnd.getBlock() instanceof BlockNode && blockStart instanceof BlockNode
					&& transportModeStart.toString().equals(compoundTag.getStringOr(TAG_TRANSPORT_MODE, "TRAIN"))
					&& transportModeStart == transportModeEnd) {
				final Player player = context.getPlayer();

				if (isConnector) {
				if (!posStart.equals(posEnd)) {
					clearQueuedFreeNodeAngles();

					final float angleDifference = (float) Math.toDegrees(Math.atan2(posEnd.getZ() - posStart.getZ(), posEnd.getX() - posStart.getX()));
					float rawStart = getRawAngleDegrees(world, posStart, stateStart);
					float rawEnd = getRawAngleDegrees(world, posEnd, stateEnd);
					final boolean startUndetermined = blockStart instanceof BlockFreeNode && Float.isNaN(rawStart);
					final boolean endUndetermined = stateEnd.getBlock() instanceof BlockFreeNode && Float.isNaN(rawEnd);

					if (startUndetermined && endUndetermined) {
						rawStart = angleDifference;
						rawEnd = angleDifference;
						queueFreeNodeRawAngle(posStart, rawStart);
						queueFreeNodeRawAngle(posEnd, rawEnd);
					} else if (startUndetermined) {
						final Double deg = RailCalculator.calculateMaxRadiusAngle(
								posEnd.getX(), posEnd.getZ(), posStart.getX(), posStart.getZ(),
								Math.toRadians(rawEnd));
						if (deg == null) {
							if (player != null) {
								player.sendOverlayMessage(Text.translatable("gui.mtr.invalid_orientation"));
							}
							return;
						}
						rawStart = deg.floatValue();
						queueFreeNodeRawAngle(posStart, rawStart);
					} else if (endUndetermined) {
						final Double deg = RailCalculator.calculateMaxRadiusAngle(
								posStart.getX(), posStart.getZ(), posEnd.getX(), posEnd.getZ(),
								Math.toRadians(rawStart));
						if (deg == null) {
							if (player != null) {
								player.sendOverlayMessage(Text.translatable("gui.mtr.invalid_orientation"));
							}
							return;
						}
						rawEnd = deg.floatValue();
						queueFreeNodeRawAngle(posEnd, rawEnd);
					}

					RailAngle railAngleStart = RailAngle.fromExactAngle(rawStart);
					if (!RailAngle.similarFacing(angleDifference, rawStart)) {
						railAngleStart = railAngleStart.getOpposite();
					}
					RailAngle railAngleEnd = RailAngle.fromExactAngle(rawEnd);
					if (RailAngle.similarFacing(angleDifference, rawEnd)) {
						railAngleEnd = railAngleEnd.getOpposite();
					}

					onConnect(world, context.getItemInHand(), transportModeStart, stateStart, stateEnd, posStart, posEnd, railAngleStart, railAngleEnd, player, railwayData);
				}
				} else {
					onRemove(world, posStart, posEnd, player, railwayData);
				}
			}
		} finally {
			clearQueuedFreeNodeAngles();
			compoundTag.remove(TAG_TRANSPORT_MODE);
		}
	}

	@Override
	protected boolean clickCondition(UseOnContext context) {
		final Level world = context.getLevel();
		final Block blockStart = world.getBlockState(context.getClickedPos()).getBlock();
		if (blockStart instanceof BlockNode) {
			final TransportMode mode = BlockFreeNode.getEffectiveTransportMode(world, context.getClickedPos());
			if (mode == TransportMode.AIRPLANE) {
				return forAirplaneNode;
			} else {
				return mode.continuousMovement ? forContinuousMovementNode : forNonContinuousMovementNode;
			}
		} else {
			return false;
		}
	}

	protected abstract void onConnect(Level world, ItemStack stack, TransportMode transportMode, BlockState stateStart, BlockState stateEnd, BlockPos posStart, BlockPos posEnd, RailAngle facingStart, RailAngle facingEnd, Player player, RailwayData railwayData);

	protected abstract void onRemove(Level world, BlockPos posStart, BlockPos posEnd, Player player, RailwayData railwayData);
}
