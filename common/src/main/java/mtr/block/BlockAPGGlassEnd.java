package mtr.block;

import mtr.Items;
import net.minecraft.world.item.Item;

public class BlockAPGGlassEnd extends BlockPSDAPGGlassEndBase {

	public BlockAPGGlassEnd(Properties properties) {
		super(properties);
	}

	@Override
	public Item asItem() {
		return Items.APG_GLASS_END.get();
	}
}
