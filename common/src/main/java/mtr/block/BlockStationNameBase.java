package mtr.block;

import mtr.data.IGui;
import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public abstract class BlockStationNameBase extends BlockDirectionalMapper implements EntityBlockMapper {

	public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 2);

	protected BlockStationNameBase(Properties settings) {
		super(settings);
	}

//	@Override
//	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
//		tooltip.add(Text.translatable("tooltip.mtr.station_color_name").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
//	}

	public abstract static class TileEntityStationNameBase extends BlockEntityMapper implements IGui {

		public final float yOffset;
		public final float zOffset;
		public final boolean isDoubleSided;

		public TileEntityStationNameBase(BlockEntityType<?> type, BlockPos pos, BlockState state, float yOffset, float zOffset, boolean isDoubleSided) {
			super(type, pos, state);
			this.yOffset = yOffset;
			this.zOffset = zOffset;
			this.isDoubleSided = isDoubleSided;
		}

		public abstract int getColor(BlockState state);
	}
}
