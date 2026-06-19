package mtr.item;

import mtr.data.RailwayData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemTunnelCreator extends ItemNodeModifierSelectableBlockBase {

	public ItemTunnelCreator(Item.Properties properties, int height, int width) {
		super(properties, false, height, width);
	}

	@Override
	protected boolean onConnect(Player player, ItemStack stack, RailwayData railwayData, BlockPos posStart, BlockPos posEnd, int radius, int height) {
		return railwayData.railwayDataRailActionsModule.markRailForTunnel(player, posStart, posEnd, radius, height);
	}
}
