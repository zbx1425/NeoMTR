package mtr.render;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class MoreRenderLayers {

	private static final Function<Identifier, RenderType> ENTITY_TRANSLUCENT_CULL = Util.memoize((texture) -> {
		RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_CULL)
				.withTexture("Sampler0", texture)
				.useLightmap()
				.useOverlay()
				.affectsCrumbling()
				.sortOnUpload()
				.setOutline(RenderSetup.OutlineProperty.NONE)
				.createRenderSetup();
		return RenderType.create("mtr_entity_translucent_cull", state);
	});

	private static final Map<String, RenderType> LIGHT_CACHE = new HashMap<>();
	private static final Map<Identifier, RenderType> INTERIOR_CACHE = new HashMap<>();
	private static final Map<Identifier, RenderType> INTERIOR_TRANSLUCENT_CACHE = new HashMap<>();
	private static final Map<Identifier, RenderType> EXTERIOR_CACHE = new HashMap<>();
	private static final Map<Identifier, RenderType> EXTERIOR_TRANSLUCENT_CACHE = new HashMap<>();

	public static RenderType getLight(Identifier texture, boolean isTranslucent) {
		return checkCache(texture.toString() + isTranslucent, () -> RenderTypes.beaconBeam(texture, isTranslucent), LIGHT_CACHE);
	}

	public static RenderType getInterior(Identifier texture) {
		return checkCache(texture, () -> RenderTypes.entityCutoutCull(texture), INTERIOR_CACHE);
	}

	public static RenderType getInteriorTranslucent(Identifier texture) {
		return checkCache(texture, () -> ENTITY_TRANSLUCENT_CULL.apply(texture), INTERIOR_TRANSLUCENT_CACHE);
	}

	public static RenderType getExterior(Identifier texture) {
		return checkCache(texture, () -> RenderTypes.entityCutoutCull(texture), EXTERIOR_CACHE);
	}

	public static RenderType getExteriorTranslucent(Identifier texture) {
		return checkCache(texture, () -> ENTITY_TRANSLUCENT_CULL.apply(texture), EXTERIOR_TRANSLUCENT_CACHE);
	}

	private static <T> RenderType checkCache(T identifier, Supplier<RenderType> supplier, Map<T, RenderType> cache) {
		if (cache.containsKey(identifier)) {
			return cache.get(identifier);
		} else {
			final RenderType renderLayer = supplier.get();
			cache.put(identifier, renderLayer);
			return renderLayer;
		}
	}
}
