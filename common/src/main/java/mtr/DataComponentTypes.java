package mtr;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;

public interface DataComponentTypes {
    // TODO: Refactor all nbt to DataComponentType, then we can just detect if the selected block pos exists
    RegistryObject<DataComponentType<Boolean>> RAIL_MODIFIER_SELECTED = new RegistryObject<>(() -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build());
}
