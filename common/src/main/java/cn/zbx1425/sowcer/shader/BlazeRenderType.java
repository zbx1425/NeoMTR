package cn.zbx1425.sowcer.shader;

import mtr.render.MoreRenderLayers;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.NotImplementedException;

public class BlazeRenderType {

//    private static final Function<Identifier, RenderType> ENTITY_CUTOUT = Util.memoize(resourceLocation ->
//            RenderType.create(
//                "entity_cutout", RenderSetup.builder().createRenderSetup() DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES,
//                256, true, false,
//                ((RenderType.CompositeRenderType)RenderType.entityCutout(resourceLocation)).state
//            ));
//    private static final Function<Identifier, RenderType> ENTITY_TRANSLUCENT_CULL = Util.memoize(resourceLocation ->
//            RenderType.create(
//                    "entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES,
//                    256, true, true,
//                    ((RenderType.CompositeRenderType)RenderType.entityTranslucentCull(resourceLocation)).state
//            ));
//    private static final BiFunction<Identifier, Boolean, RenderType> BEACON_BEAM = Util.memoize((resourceLocation, translucent) ->
//            RenderType.create(
//                    "beacon_beam", DefaultVertexFormat.BLOCK, VertexFormat.Mode.TRIANGLES,
//                    256, false, true,
//                    ((RenderType.CompositeRenderType)RenderType.beaconBeam(resourceLocation, translucent)).state
//            ));

    public static RenderType entityCutout(Identifier resourceLocation) {
//        return ENTITY_CUTOUT.apply(resourceLocation);
        return RenderTypes.entityCutout(resourceLocation);
    }

    public static RenderType entityTranslucentCull(Identifier resourceLocation) {
//        return ENTITY_TRANSLUCENT_CULL.apply(resourceLocation);
        return MoreRenderLayers.ENTITY_TRANSLUCENT_CULL.apply(resourceLocation);
    }

    public static RenderType beaconBeam(Identifier resourceLocation, boolean bl) {
//        return BEACON_BEAM.apply(resourceLocation, bl);
        return RenderTypes.beaconBeam(resourceLocation, bl);
    }

    public static RenderType entityTranslucentEmissive(Identifier resourceLocation) {
        return RenderTypes.entityTranslucentEmissive(resourceLocation);
    }
}
