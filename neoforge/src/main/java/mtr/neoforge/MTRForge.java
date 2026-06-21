package mtr.neoforge;

import cn.zbx1425.mtrsteamloco.neoforge.ClientProxy;
import cn.zbx1425.mtrsteamloco.neoforge.RegistriesWrapperImpl;
import mtr.*;
import mtr.client.CustomResources;
import mtr.item.ItemBlockEnchanted;
import mtr.item.ItemWithCreativeTabBase;
import mtr.mappings.BlockEntityMapper;
import mtr.block.behaviour.BlockItemDecorator;
import mtr.neoforge.mappings.ForgeUtilities;
import mtr.render.RenderDrivingOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(MTR.MOD_ID)
public class MTRForge {

	private static final DeferredRegisterHolder<Item> ITEMS = new DeferredRegisterHolder<>(MTR.MOD_ID, ForgeUtilities.registryGetItem());
	private static final DeferredRegisterHolder<Block> BLOCKS = new DeferredRegisterHolder<>(MTR.MOD_ID, ForgeUtilities.registryGetBlock());
	private static final DeferredRegisterHolder<BlockEntityType<?>> BLOCK_ENTITY_TYPES = new DeferredRegisterHolder<>(MTR.MOD_ID, ForgeUtilities.registryGetBlockEntityType());
	private static final DeferredRegisterHolder<DataComponentType<?>> DATA_COMPONENT_TYPES = new DeferredRegisterHolder<>(MTR.MOD_ID, Registries.DATA_COMPONENT_TYPE);
	private static final DeferredRegisterHolder<EntityType<?>> ENTITY_TYPES = new DeferredRegisterHolder<>(MTR.MOD_ID, ForgeUtilities.registryGetEntityType());
	private static final DeferredRegisterHolder<SoundEvent> SOUND_EVENTS = new DeferredRegisterHolder<>(MTR.MOD_ID, ForgeUtilities.registryGetSoundEvent());
	private static final DeferredRegisterHolder<CreativeModeTab> CREATIVE_MODE_TABS = new DeferredRegisterHolder<>(MTR.MOD_ID, Registries.CREATIVE_MODE_TAB);
	private static final RegistriesWrapperImpl registries = new RegistriesWrapperImpl();

	public static final CompatPacketRegistry PACKET_REGISTRY = new CompatPacketRegistry();

	static {
		MTR.init(MTRForge::registerItem, MTRForge::registerBlock, MTRForge::registerBlockItem, MTRForge::registerEnchantedBlock, MTRForge::registerBlockEntityType, MTRForge::registerEntityType, MTRForge::registerSoundEvent, MTRForge::registerDataComponentType);
		cn.zbx1425.mtrsteamloco.Main.init(registries);
	}

	public MTRForge(IEventBus eventBus) {
		BLOCKS.register(eventBus);
		ITEMS.register(eventBus);
		BLOCK_ENTITY_TYPES.register(eventBus);
		ENTITY_TYPES.register(eventBus);
		DATA_COMPONENT_TYPES.register(eventBus);
		SOUND_EVENTS.register(eventBus);
		registries.registerAllDeferred(eventBus);

		ForgeUtilities.registerCreativeModeTabsToDeferredRegistry(CREATIVE_MODE_TABS);
		CREATIVE_MODE_TABS.register(eventBus);

		eventBus.register(MTRModEventBus.class);
		eventBus.register(ForgeUtilities.RegisterCreativeTabs.class);
		if (FMLEnvironment.getDist().isClient()) {
			ForgeUtilities.renderGameOverlayAction((guiGraphics) -> {
				RenderDrivingOverlay.render((GuiGraphicsExtractor) guiGraphics);
			});
			NeoForge.EVENT_BUS.register(ForgeUtilities.Events.class);
			eventBus.register(ForgeUtilities.ClientsideEvents.class);

			// NTE
			NeoForge.EVENT_BUS.register(ClientProxy.ForgeEventBusListener.class);
			eventBus.register(ClientProxy.ModEventBusListener.class);

			ClientProxy.registerConfigScreen();
		}
	}

	private static void registerItem(String path, BrandNewEpicRegistryObject<Item> item) {
		item.setResourceKey(ResourceKey.create(Registries.ITEM, MTR.id(path)));

		ITEMS.register(path, () -> {
			final Item itemObject = item.get();
			if (itemObject instanceof ItemWithCreativeTabBase) {
				Registry.registerCreativeModeTab(((ItemWithCreativeTabBase) itemObject).creativeModeTab.resourceLocation, itemObject);
			} else if (itemObject instanceof ItemWithCreativeTabBase.ItemPlaceOnWater) {
				Registry.registerCreativeModeTab(((ItemWithCreativeTabBase.ItemPlaceOnWater) itemObject).creativeModeTab.resourceLocation, itemObject);
			}
			return itemObject;
		});
	}

	private static void registerBlock(String path, BrandNewEpicRegistryObject<Block> block) {
		block.setResourceKey(ResourceKey.create(Registries.BLOCK, MTR.id(path)));
		BLOCKS.register(path, block::get);
	}

	private static void registerBlockItem(String path, BrandNewEpicRegistryObject<Block> block, CreativeModeTabs.Wrapper creativeModeTabWrapper) {
		registerBlock(path, block);
		ITEMS.register(path, () -> {
			final Item.Properties itemProperties = new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, MTR.id(path)))
					.useBlockDescriptionPrefix();

			tryDecorateBlockItem(block.get(), itemProperties);

			final BlockItem blockItem = new BlockItem(block.get(), itemProperties);
			Registry.registerCreativeModeTab(creativeModeTabWrapper.resourceLocation, blockItem);
			return blockItem;
		});
	}

	private static void registerEnchantedBlock(String path, BrandNewEpicRegistryObject<Block> block, CreativeModeTabs.Wrapper creativeModeTab) {
		registerBlock(path, block);

		ITEMS.register(path, () -> {
			final Item.Properties itemProperties = new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, MTR.id(path)))
					.useBlockDescriptionPrefix();

			tryDecorateBlockItem(block.get(), itemProperties);

			final ItemBlockEnchanted itemBlockEnchanted = new ItemBlockEnchanted(block.get(), itemProperties);
			Registry.registerCreativeModeTab(creativeModeTab.resourceLocation, itemBlockEnchanted);
			return itemBlockEnchanted;
		});
	}

	private static void registerBlockEntityType(String path, RegistryObject<? extends BlockEntityType<? extends BlockEntityMapper>> blockEntityType) {
		BLOCK_ENTITY_TYPES.register(path, blockEntityType::get);
	}

	private static void registerDataComponentType(String path, RegistryObject<? extends DataComponentType<?>> dataComponentType) {
		DATA_COMPONENT_TYPES.register(path, dataComponentType::get);
	}

	private static void registerEntityType(String path, RegistryObject<? extends EntityType<? extends Entity>> entityType) {
		ENTITY_TYPES.register(path, entityType::get);
	}

	private static void registerSoundEvent(String path, SoundEvent soundEvent) {
		SOUND_EVENTS.register(path, () -> soundEvent);
	}

	private static class MTRModEventBus {

		@SubscribeEvent
		public static void onClientSetupEvent(FMLClientSetupEvent event) {
			MTRClient.init();
			event.enqueueWork(MTRClient::initItemModelPredicate);
			ForgeUtilities.registerTextureStitchEvent(textureAtlas -> {
				if (((TextureAtlas) textureAtlas).location().getPath().equals("textures/atlas/blocks.png")) {
					CustomResources.reload(Minecraft.getInstance().getResourceManager());
				}
			});
		}

		@SubscribeEvent
		public static void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
			PayloadRegistrar registrar = event.registrar("1");
			MTRForge.PACKET_REGISTRY.commit(registrar);
		}
	}

	private static void tryDecorateBlockItem(Block block, Item.Properties prop) {
		if(block instanceof BlockItemDecorator decorator) {
			decorator.decorateBlockItem(prop);
		}
	}
}
