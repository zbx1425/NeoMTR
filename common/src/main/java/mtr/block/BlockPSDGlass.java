package mtr.block;

import mtr.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockPSDGlass extends BlockPSDAPGGlassBase {

	private final int style;

	public BlockPSDGlass(BlockBehaviour.Properties properties, int style) {
		super(properties);
		this.style = style;
	}

	@Override
	public Item asItem() {
		return style == 0 ? Items.PSD_GLASS_1.get() : Items.PSD_GLASS_2.get();
	}
}
