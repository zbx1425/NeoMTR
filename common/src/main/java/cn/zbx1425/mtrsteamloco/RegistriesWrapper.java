package cn.zbx1425.mtrsteamloco;

import mtr.BrandNewEpicRegistryObject;
import mtr.CreativeModeTabs;
import mtr.RegistryObject;
import mtr.item.ItemWithCreativeTabBase;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface RegistriesWrapper {

    void registerBlock(String path, BrandNewEpicRegistryObject<Block> block);

    void registerItem(String path, BrandNewEpicRegistryObject<Item> item);

    void registerBlockAndItem(String path, BrandNewEpicRegistryObject<Block> block, CreativeModeTabs.Wrapper tab);

    void registerBlockEntityType(String path, RegistryObject<? extends BlockEntityType<? extends BlockEntity>> blockEntityType);

    void registerEntityType(String path, RegistryObject<? extends EntityType<? extends Entity>> entityType);

    void registerSoundEvent(String path, SoundEvent soundEvent);

    void registerParticleType(String path, ParticleType<?> particleType);

    SimpleParticleType createParticleType(boolean overrideLimiter);

}
