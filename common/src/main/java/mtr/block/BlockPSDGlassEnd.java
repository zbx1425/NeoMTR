package mtr.block;

import mtr.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockPSDGlassEnd extends BlockPSDAPGGlassEndBase {

	private final int style;

	public BlockPSDGlassEnd(BlockBehaviour.Properties properties, int style) {
		super(properties);
		this.style = style;
	}

	@Override
	public Item asItem() {
		return style == 0 ? Items.PSD_GLASS_END_1.get() : Items.PSD_GLASS_END_2.get();
	}
}
