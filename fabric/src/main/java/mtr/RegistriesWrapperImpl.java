package mtr;


import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.RegistriesWrapper;
import mtr.item.ItemWithCreativeTabBase;
import mtr.mappings.FabricRegistryUtilities;
import mtr.mappings.RegistryUtilities;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class RegistriesWrapperImpl implements RegistriesWrapper {

    @Override
    public void registerBlock(String id, RegistryObject<Block> block) {
        Registry.register(RegistryUtilities.registryGetBlock(), Main.id(id), block.get());
    }

    @Override
    public void registerBlockAndItem(String id, RegistryObject<Block> block, CreativeModeTabs.Wrapper tab) {
        Registry.register(RegistryUtilities.registryGetBlock(), Main.id(id), block.get());
        final BlockItem blockItem = new BlockItem(block.get(), new Item.Properties());
        Registry.register(RegistryUtilities.registryGetItem(), Main.id(id), blockItem);
        FabricRegistryUtilities.registerCreativeModeTab(tab.get(), blockItem);
    }

    @Override
    public void registerItem(String id, RegistryObject<ItemWithCreativeTabBase> item) {
        Registry.register(RegistryUtilities.registryGetItem(), Main.id(id), item.get());
        FabricRegistryUtilities.registerCreativeModeTab(item.get().creativeModeTab.get(), item.get());
    }

    @Override
    public void registerBlockEntityType(String id, RegistryObject<? extends BlockEntityType<? extends BlockEntity>> blockEntityType) {
        Registry.register(RegistryUtilities.registryGetBlockEntityType(), Main.id(id), blockEntityType.get());
    }

    @Override
    public void registerEntityType(String id, RegistryObject<? extends EntityType<? extends Entity>> entityType) {
        Registry.register(RegistryUtilities.registryGetEntityType(), Main.id(id), entityType.get());
    }

    @Override
    public void registerSoundEvent(String id, SoundEvent soundEvent) {
        Registry.register(RegistryUtilities.registryGetSoundEvent(), Main.id(id), soundEvent);
    }

    @Override
    public void registerParticleType(String id, ParticleType<?> particleType) {
        Registry.register(RegistryUtilities.registryGetParticleType(), Main.id(id), particleType);
    }

    @Override
    public SimpleParticleType createParticleType(boolean overrideLimiter) {
        return FabricParticleTypes.simple(overrideLimiter);
    }
}