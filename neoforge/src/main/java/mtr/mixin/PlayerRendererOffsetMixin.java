package mtr.mixin;

import mtr.client.ClientData;
import mtr.render.RenderTrains;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class PlayerRendererOffsetMixin {
//
//	@Inject(method = "getRenderOffset", at = @At(value = "RETURN"), cancellable = true)
//	public void getRenderOffset(EntityRenderState state, CallbackInfoReturnable<Vec3> cir) {
//		if (state instanceof  ClientData.isRiding(state.getUUID())) {
//			callbackInfoReturnable.setReturnValue(new Vec3(0, -RenderTrains.PLAYER_RENDER_OFFSET, 0));
//		}
//	}
}
