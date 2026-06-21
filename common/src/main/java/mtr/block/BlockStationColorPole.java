package mtr.block;

import mtr.mappings.BlockMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockStationColorPole extends BlockMapper {

	private final boolean showTooltip;

	public BlockStationColorPole(Properties settings, boolean showTooltip) {
		super(settings);
		this.showTooltip = showTooltip;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
		return getStationPoleShape();
	}

//	@Override
//	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
//		if (showTooltip) {
//			tooltip.add(Text.translatable("tooltip.mtr.station_color").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
//		}
//	}

	public static VoxelShape getStationPoleShape() {
		return Block.box(6, 0, 6, 10, 16, 10);
	}
}
