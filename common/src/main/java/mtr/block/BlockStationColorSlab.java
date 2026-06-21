package mtr.block;

import mtr.block.behaviour.BlockItemDecorator;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SlabBlock;

public class BlockStationColorSlab extends SlabBlock implements BlockItemDecorator {

	public BlockStationColorSlab(Properties settings) {
		super(settings);
	}

	@Override
	public void decorateBlockItem(Item.Properties properties) {
		properties.overrideDescription(super.getDescriptionId().replace("block.mtr.station_color_", "block.minecraft."));
	}

//	@Override
//	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
//		tooltip.add(Text.translatable("tooltip.mtr.station_color").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
//	}
}
