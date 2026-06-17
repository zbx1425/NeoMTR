package mtr.mappings;

import com.mojang.brigadier.CommandDispatcher;
import mtr.MTR;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface FabricRegistryUtilities {

	static void registerItemModelPredicate(String id, Item item, String tag) {
		FabricModelPredicateProviderRegistry.register(item, Identifier.parse(id), (itemStack, clientWorld, livingEntity, i) -> itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(tag) ? 1 : 0);
	}

	static <T extends BlockEntityMapper> void registerTileEntityRenderer(BlockEntityType<T> type, Function<BlockEntityRenderDispatcher, BlockEntityRendererMapper<T>> factory) {
		BlockEntityRendererRegistry.register(type, context -> factory.apply(null));
	}

	static <T extends Entity> void registerEntityRenderer(EntityType<T> type, Function<EntityRendererProvider.Context, EntityRendererMapper<T>> factory) {
		EntityRendererRegistry.register(type, factory::apply);
	}

	static void registerCommand(Consumer<CommandDispatcher<CommandSourceStack>> callback) {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, commandSelection) -> callback.accept(dispatcher));
	}

	static void registerCreativeModeTab(CreativeModeTab creativeModeTab, Item item) {
		CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register((tab, entries) -> {
			if (tab == creativeModeTab) entries.accept(item);
		});
	}

	static CreativeModeTab createCreativeModeTab(Identifier id, Supplier<ItemStack> supplier) {
		String normalizedPath = id.getPath().startsWith(id.getNamespace() + "_")
				? id.getPath().substring(id.getNamespace().length() + 1) : id.getPath();
		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.icon(supplier)
				.title(Text.translatable(String.format("itemGroup.%s.%s", id.getNamespace(), normalizedPath)))
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab);
		return tab;
	}
}
