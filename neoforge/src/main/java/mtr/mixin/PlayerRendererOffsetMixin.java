package mtr.mixin;

import mtr.client.ClientData;
import mtr.render.RenderTrains;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class PlayerRendererOffsetMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    public void mtr$hidePlayerForCustomRenderer(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
        if(ClientData.isRiding(entity.getUUID())) {
            state.passengerOffset = new Vec3(0, RenderTrains.PLAYER_RENDER_OFFSET, 0);
        }
    }
}