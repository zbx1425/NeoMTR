package mtr;

import net.minecraft.resources.ResourceKey;

import java.util.function.Function;

public class BrandNewEpicRegistryObject<T> {
    private T object;
    private final Function<ResourceKey<T>, T> function;

    public BrandNewEpicRegistryObject(Function<ResourceKey<T>, T> function) {
        this.function = function;
    }

    public T get() {
        if(object == null) throw new IllegalStateException("Object not created.");
        return object;
    }

    public T create(ResourceKey<T> key) {
        if (object == null) {
            object = function.apply(key);
        }
        return object;
    }
}
