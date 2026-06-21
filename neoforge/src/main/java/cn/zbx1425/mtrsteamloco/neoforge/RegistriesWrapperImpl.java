package cn.zbx1425.mtrsteamloco.neoforge;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.RegistriesWrapper;
import mtr.*;
import mtr.item.ItemWithCreativeTabBase;
import mtr.neoforge.DeferredRegisterHolder;
import mtr.neoforge.mappings.ForgeUtilities;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;

public class RegistriesWrapperImpl implements RegistriesWrapper {

    private static final DeferredRegisterHolder<Item> ITEMS = new DeferredRegisterHolder<>(Main.MOD_ID, ForgeUtilities.registryGetItem());
    private static final DeferredRegisterHolder<Block> BLOCKS = new DeferredRegisterHolder<>(Main.MOD_ID, ForgeUtilities.registryGetBlock());
    private static final DeferredRegisterHolder<BlockEntityType<?>> BLOCK_ENTITY_TYPES = new DeferredRegisterHolder<>(Main.MOD_ID, ForgeUtilities.registryGetBlockEntityType());
    private static final DeferredRegisterHolder<EntityType<?>> ENTITY_TYPES = new DeferredRegisterHolder<>(Main.MOD_ID, ForgeUtilities.registryGetEntityType());
    private static final DeferredRegisterHolder<SoundEvent> SOUND_EVENTS = new DeferredRegisterHolder<>(Main.MOD_ID, ForgeUtilities.registryGetSoundEvent());
    private static final DeferredRegisterHolder<ParticleType<?>> PARTICLE_TYPES = new DeferredRegisterHolder<>(Main.MOD_ID, ForgeUtilities.registryGetParticleType());


    @Override
    public void registerBlock(String path, BrandNewEpicRegistryObject<Block> block) {
        block.setResourceKey(ResourceKey.create(Registries.BLOCK, Main.id(path)));
        BLOCKS.register(path,id -> block.get());
    }

    @Override
    public void registerBlockAndItem(String path, BrandNewEpicRegistryObject<Block> block, CreativeModeTabs.Wrapper tab) {
        block.setResourceKey(ResourceKey.create(Registries.BLOCK, Main.id(path)));
        BLOCKS.register(path,id -> block.get());

        final Item.Properties itemProperties = new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, Main.id(path)))
                .useBlockDescriptionPrefix();

        ITEMS.register(path, (id) -> {
            final BlockItem blockItem = new BlockItem(block.get(), itemProperties);
            Registry.registerCreativeModeTab(tab.resourceLocation, blockItem);
            return blockItem;
        });
    }

    @Override
    public void registerItem(String path, BrandNewEpicRegistryObject<Item> item) {
        item.setResourceKey(ResourceKey.create(Registries.ITEM, Main.id(path)));

        ITEMS.register(path, (id) -> {
            final ItemWithCreativeTabBase itemObject = (ItemWithCreativeTabBase) item.get();
            Registry.registerCreativeModeTab(itemObject.creativeModeTab.resourceLocation, itemObject);
            return itemObject;
        });
    }

    @Override
    public void registerBlockEntityType(String path, RegistryObject<? extends BlockEntityType<? extends BlockEntity>> blockEntityType) {
        BLOCK_ENTITY_TYPES.register(path, blockEntityType::get);
    }

    @Override
    public void registerEntityType(String path, RegistryObject<? extends EntityType<? extends Entity>> entityType) {
        ENTITY_TYPES.register(path, entityType::get);
    }

    @Override
    public void registerSoundEvent(String path, SoundEvent soundEvent) {
        SOUND_EVENTS.register(path, () -> soundEvent);
    }

    @Override
    public void registerParticleType(String path, ParticleType<?> particleType) {
        PARTICLE_TYPES.register(path, () -> particleType);
    }

    @Override
    public SimpleParticleType createParticleType(boolean overrideLimiter) {
        return new SimpleParticleType(overrideLimiter);
    }

    public void registerAllDeferred(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITY_TYPES.register(eventBus);
        ENTITY_TYPES.register(eventBus);
        SOUND_EVENTS.register(eventBus);
        PARTICLE_TYPES.register(eventBus);
    }
}