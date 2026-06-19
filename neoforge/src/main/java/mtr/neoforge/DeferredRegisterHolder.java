package mtr.neoforge;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class DeferredRegisterHolder<T> {

	private final DeferredRegister<T> deferredRegister;

	public DeferredRegisterHolder(String modId, ResourceKey<Registry<T>> key) {
		deferredRegister = DeferredRegister.create(key, modId);
	}

	public void register(IEventBus modBus) {
		deferredRegister.register(modBus);
	}

	public void register(String id, Supplier<? extends T> supplier) {
		deferredRegister.register(id, supplier);
	}

	public void register(String id, Function<Identifier, ? extends T> callback) {
		deferredRegister.register(id, callback);
	}
}
