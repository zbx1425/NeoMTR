package cn.zbx1425.sowcer.shader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.Util;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.function.BiFunction;
import java.util.function.Function;

public class BlazeRenderType {

    private static final Function<Identifier, RenderType> ENTITY_CUTOUT = Util.memoize(resourceLocation ->
            RenderType.create(
                "entity_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES,
                256, true, false,
                ((RenderType.CompositeRenderType)RenderType.entityCutout(resourceLocation)).state
            ));
    private static final Function<Identifier, RenderType> ENTITY_TRANSLUCENT_CULL = Util.memoize(resourceLocation ->
            RenderType.create(
                    "entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES,
                    256, true, true,
                    ((RenderType.CompositeRenderType)RenderType.entityTranslucentCull(resourceLocation)).state
            ));
    private static final BiFunction<Identifier, Boolean, RenderType> BEACON_BEAM = Util.memoize((resourceLocation, translucent) ->
            RenderType.create(
                    "beacon_beam", DefaultVertexFormat.BLOCK, VertexFormat.Mode.TRIANGLES,
                    256, false, true,
                    ((RenderType.CompositeRenderType)RenderType.beaconBeam(resourceLocation, translucent)).state
            ));

    public static RenderType entityCutout(Identifier resourceLocation) {
        return ENTITY_CUTOUT.apply(resourceLocation);
    }

    public static RenderType entityTranslucentCull(Identifier resourceLocation) {
        return ENTITY_TRANSLUCENT_CULL.apply(resourceLocation);
    }

    public static RenderType beaconBeam(Identifier resourceLocation, boolean bl) {
        return BEACON_BEAM.apply(resourceLocation, bl);
    }
}
