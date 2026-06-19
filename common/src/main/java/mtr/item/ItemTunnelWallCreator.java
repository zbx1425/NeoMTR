package mtr.item;

import mtr.data.RailwayData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ItemTunnelWallCreator extends ItemNodeModifierSelectableBlockBase {

	public ItemTunnelWallCreator(Item.Properties properties, int height, int width) {
		super(properties, true, height, width);
	}

	@Override
	protected boolean onConnect(Player player, ItemStack stack, RailwayData railwayData, BlockPos posStart, BlockPos posEnd, int radius, int height) {
		final BlockState state = getSavedState(stack);
		return state == null || railwayData.railwayDataRailActionsModule.markRailForTunnelWall(player, posStart, posEnd, radius, height, state);
	}
}
