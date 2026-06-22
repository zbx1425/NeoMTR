package mtr;

import com.mojang.serialization.Codec;
import mtr.data.TransportMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;

public interface DataComponentTypes {
    RegistryObject<DataComponentType<BlockPos>> START_POS = new RegistryObject<>(() -> DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).build());
    RegistryObject<DataComponentType<TransportMode>> TRANSPORT_TYPE = new RegistryObject<>(() -> DataComponentType.<TransportMode>builder().persistent(TransportMode.CODEC).build());
    RegistryObject<DataComponentType<Integer>> SELECTED_BLOCK = new RegistryObject<>(() -> DataComponentType.<Integer>builder().persistent(Codec.INT).build());
}
