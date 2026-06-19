package mtr;

import net.minecraft.resources.ResourceKey;

import java.util.function.Function;

public class BrandNewEpicRegistryObject<T> {
    private T object;
    private final Function<ResourceKey<T>, T> function;
    private ResourceKey<T> resourceKey;

    public BrandNewEpicRegistryObject(Function<ResourceKey<T>, T> function) {
        this.function = function;
    }

    public void setResourceKey(ResourceKey<T> resourceKey) {
        this.resourceKey = resourceKey;
    }

    public T get() {
        if(this.resourceKey == null) throw new IllegalStateException("MTR Registry: ResourceKey not set!");

        if (object == null) {
            object = function.apply(this.resourceKey);
        }
        return object;
    }
}
