package mtr.item;

import mtr.CreativeModeTabs;
import mtr.mappings.PlaceOnWaterBlockItem;
import mtr.mappings.RegistryUtilities;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Function;

public class ItemWithCreativeTabBase extends Item {

	public final CreativeModeTabs.Wrapper creativeModeTab;

	public ItemWithCreativeTabBase(Item.Properties properties, CreativeModeTabs.Wrapper creativeModeTab) {
		super(properties);
		this.creativeModeTab = creativeModeTab;
	}

	public ItemWithCreativeTabBase(Item.Properties properties, CreativeModeTabs.Wrapper creativeModeTab, Function<Properties, Properties> propertiesConsumer) {
		super(propertiesConsumer.apply(properties));
		this.creativeModeTab = creativeModeTab;
	}

	public static class ItemPlaceOnWater extends PlaceOnWaterBlockItem {

		public final CreativeModeTabs.Wrapper creativeModeTab;

		public ItemPlaceOnWater(Properties properties, CreativeModeTabs.Wrapper creativeModeTab, Block block) {
			super(block, properties);
			this.creativeModeTab = creativeModeTab;
		}
	}
}
