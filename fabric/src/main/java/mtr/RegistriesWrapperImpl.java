package mtr;


import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.RegistriesWrapper;
import mtr.item.ItemWithCreativeTabBase;
import mtr.mappings.FabricRegistryUtilities;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class RegistriesWrapperImpl implements RegistriesWrapper {

    private static final String MOD_ID = Main.MOD_ID;

    @Override
    public void registerBlock(String path, BrandNewEpicRegistryObject<Block> block) {
        block.setResourceKey(ResourceKey.create(Registries.BLOCK, id(path)));
        Registry.register(BuiltInRegistries.BLOCK, id(path), block.get());
    }

    @Override
    public void registerBlockAndItem(String path, BrandNewEpicRegistryObject<Block> block, CreativeModeTabs.Wrapper tab) {
        block.setResourceKey(ResourceKey.create(Registries.BLOCK, id(path)));
        Registry.register(BuiltInRegistries.BLOCK, id(path), block.get());

        final Item.Properties itemProperties = new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id(path)))
                .useBlockDescriptionPrefix();

        final BlockItem blockItem = new BlockItem(block.get(), itemProperties);
        Registry.register(BuiltInRegistries.ITEM, id(path), blockItem);
        FabricRegistryUtilities.registerCreativeModeTab(tab.get(), blockItem);
    }

    @Override
    public void registerItem(String path, BrandNewEpicRegistryObject<Item> item) {
        item.setResourceKey(ResourceKey.create(Registries.ITEM, id(path)));
        Registry.register(BuiltInRegistries.ITEM, id(path), item.get());

        if(item.get() instanceof ItemWithCreativeTabBase itemWithCreativeTabBase) {
            FabricRegistryUtilities.registerCreativeModeTab(itemWithCreativeTabBase.creativeModeTab.get(), item.get());
        }
    }

    @Override
    public void registerBlockEntityType(String path, RegistryObject<? extends BlockEntityType<? extends BlockEntity>> blockEntityType) {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id(path), blockEntityType.get());
    }

    @Override
    public void registerDataComponents(String path, RegistryObject<? extends DataComponentType<?>> dataComponent) {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id(path), dataComponent.get());
    }

    @Override
    public void registerEntityType(String path, RegistryObject<? extends EntityType<? extends Entity>> entityType) {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id(path), entityType.get());
    }

    @Override
    public void registerSoundEvent(String path, SoundEvent soundEvent) {
        Registry.register(BuiltInRegistries.SOUND_EVENT, id(path), soundEvent);
    }

    @Override
    public void registerParticleType(String path, ParticleType<?> particleType) {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, id(path), particleType);
    }

    @Override
    public SimpleParticleType createParticleType(boolean overrideLimiter) {
        return FabricParticleTypes.simple(overrideLimiter);
    }

    private Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}