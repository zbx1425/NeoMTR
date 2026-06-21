package mtr.block;

import mtr.mappings.BlockDirectionalMapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BlockPoleCheckBase extends BlockDirectionalMapper {

	public BlockPoleCheckBase(Properties properties) {
		super(properties);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState stateBelow = ctx.getLevel().getBlockState(ctx.getClickedPos().below());
		if (isBlock(stateBelow.getBlock())) {
			return placeWithState(stateBelow);
		} else {
			return null;
		}
	}

//	@Override
//	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
//		final String[] strings = Text.translatable("tooltip.mtr.pole_placement", getTooltipBlockText()).getString().split("\n");
//		for (final String string : strings) {
//			tooltip.add(Text.literal(string).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
//		}
//	}

	protected BlockState placeWithState(BlockState stateBelow) {
		return defaultBlockState().setValue(FACING, IBlock.getStatePropertySafe(stateBelow, FACING));
	}

	protected abstract boolean isBlock(Block block);

	protected abstract Component getTooltipBlockText();
}
