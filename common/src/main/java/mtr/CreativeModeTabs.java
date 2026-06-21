package mtr;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public interface CreativeModeTabs {

	Wrapper CORE = Keys.LIFTS_ONLY ? new Wrapper() : new Wrapper(MTR.id("mtr_core"), () -> new ItemStack(Items.RAILWAY_DASHBOARD.get()));
	Wrapper RAILWAY_FACILITIES = Keys.LIFTS_ONLY ? new Wrapper() : new Wrapper(MTR.id("mtr_railway_facilities"), () -> new ItemStack(Blocks.TICKET_PROCESSOR.get()));
	Wrapper STATION_BUILDING_BLOCKS = Keys.LIFTS_ONLY ? new Wrapper() : new Wrapper(MTR.id("mtr_station_building_blocks"), () -> new ItemStack(Blocks.LOGO.get()));
	Wrapper ESCALATORS_LIFTS = new Wrapper(MTR.id("mtr_escalators_lifts"), () -> new ItemStack(Items.ESCALATOR.get()));

	class Wrapper {

		public final Identifier resourceLocation;
		private final Supplier<CreativeModeTab> creativeModeTabSupplier;
		private CreativeModeTab creativeModeTab;

		public Wrapper(Identifier resourceLocation, Supplier<ItemStack> itemSupplier) {
			this.resourceLocation = resourceLocation;
			creativeModeTabSupplier = Registry.getCreativeModeTab(resourceLocation, itemSupplier);
		}

		public CreativeModeTab get() {
			if (creativeModeTab == null) {
				creativeModeTab = creativeModeTabSupplier.get();
			}
			return creativeModeTab;
		}

		public Wrapper() {
			resourceLocation = Identifier.parse("");
			creativeModeTabSupplier = net.minecraft.world.item.CreativeModeTabs::getDefaultTab;
		}
	}
}
