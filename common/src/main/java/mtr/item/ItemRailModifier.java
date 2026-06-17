package mtr.item;

import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import mtr.block.BlockNode;
import mtr.data.*;
import mtr.mappings.Text;
import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Consumer;

public class ItemRailModifier extends ItemNodeModifierBase {

	private final boolean isOneWay;
	private final RailType railType;

	public ItemRailModifier() {
		super(true, true, true, false);
		isOneWay = false;
		railType = null;
	}

	public ItemRailModifier(boolean forNonContinuousMovementNode, boolean forContinuousMovementNode, boolean forAirplaneNode, boolean isOneWay, RailType railType) {
		super(forNonContinuousMovementNode, forContinuousMovementNode, forAirplaneNode, true);
		this.isOneWay = isOneWay;
		this.railType = railType;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
		if (isConnector && railType != null && railType.canAccelerate) {
			tooltipAdder.accept(Text.translatable("tooltip.mtr.rail_speed_limit", railType.speedLimit).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
		}
		super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
	}

	@Override
	protected void onConnect(Level world, ItemStack stack, TransportMode transportMode, BlockState stateStart, BlockState stateEnd, BlockPos posStart, BlockPos posEnd, RailAngle facingStart, RailAngle facingEnd, Player player, RailwayData railwayData) {
		if (railType.hasSavedRail && (railwayData.hasSavedRail(posStart) || railwayData.hasSavedRail(posEnd))) {
			if (player != null) {
				player.sendOverlayMessage(Text.translatable("gui.mtr.platform_or_siding_exists"));
			}
		} else {
			final boolean isValidContinuousMovement;
			final RailType newRailType;
			if (transportMode.continuousMovement) {
				final Block blockStart = stateStart.getBlock();
				final Block blockEnd = stateEnd.getBlock();

				if (blockStart instanceof BlockNode.BlockContinuousMovementNode && blockEnd instanceof BlockNode.BlockContinuousMovementNode) {
					if (((BlockNode.BlockContinuousMovementNode) blockStart).isStation && ((BlockNode.BlockContinuousMovementNode) blockEnd).isStation) {
						isValidContinuousMovement = true;
						newRailType = railType.hasSavedRail ? railType : RailType.CABLE_CAR_STATION;
					} else {
						final int differenceX = posEnd.getX() - posStart.getX();
						final int differenceZ = posEnd.getZ() - posStart.getZ();
						isValidContinuousMovement = !railType.hasSavedRail && facingStart.isParallel(facingEnd)
								&& ((facingStart.equals(RailAngle.N) || facingStart.equals(RailAngle.S)) && differenceX == 0
								|| (facingStart.equals(RailAngle.E) || facingStart.equals(RailAngle.W)) && differenceZ == 0
								|| (facingStart.equals(RailAngle.NE) || facingStart.equals(RailAngle.SW)) && differenceX == -differenceZ
								|| (facingStart.equals(RailAngle.SE) || facingStart.equals(RailAngle.NW)) && differenceX == differenceZ);
						newRailType = RailType.CABLE_CAR;
					}
				} else {
					isValidContinuousMovement = false;
					newRailType = railType;
				}
			} else {
				isValidContinuousMovement = true;
				newRailType = railType;
			}

			final Rail rail1 = new Rail(posStart, facingStart, posEnd, facingEnd, isOneWay ? RailType.NONE : newRailType, transportMode);
			final Rail rail2 = new Rail(posEnd, facingEnd, posStart, facingStart, newRailType, transportMode);

			final boolean goodRadius = rail1.goodRadius() && rail2.goodRadius();
			final boolean isValid = rail1.isValid() && rail2.isValid();

			if (goodRadius && isValid && isValidContinuousMovement) {
				((RailExtraSupplier)rail1).setIsSecondaryDir(true);
				((RailExtraSupplier)rail2).setIsSecondaryDir(false);
				ItemNodeModifierBase.applyQueuedFreeNodeAngles(world);
				railwayData.addRail(player, transportMode, posStart, posEnd, rail1, false);
				final long newId = railwayData.addRail(player, transportMode, posEnd, posStart, rail2, true);
				world.setBlockAndUpdate(posStart, stateStart.setValue(BlockNode.IS_CONNECTED, true));
				world.setBlockAndUpdate(posEnd, stateEnd.setValue(BlockNode.IS_CONNECTED, true));
				PacketTrainDataGuiServer.createRailS2C(world, transportMode, posStart, posEnd, rail1, rail2, newId);
			} else if (player != null) {
				player.sendOverlayMessage(Text.translatable(isValidContinuousMovement ? goodRadius ? "gui.mtr.invalid_orientation" : "gui.mtr.radius_too_small" : "gui.mtr.cable_car_invalid_orientation"));
			}
		}
	}

	@Override
	protected void onRemove(Level world, BlockPos posStart, BlockPos posEnd, Player player, RailwayData railwayData) {
		railwayData.removeRailConnection(player, posStart, posEnd);
		PacketTrainDataGuiServer.removeRailConnectionS2C(world, posStart, posEnd);
	}
}
