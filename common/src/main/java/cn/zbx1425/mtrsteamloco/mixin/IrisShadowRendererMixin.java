package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.render.ShadowRenderHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.irisshaders.iris.shadows.ShadowRenderer")
public class IrisShadowRendererMixin {

    @Inject(
            method = "renderShadows",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V",
                     shift = At.Shift.AFTER),
            require = 0
    )
    private void afterShadowOpaqueFeatures(CallbackInfo ci) {
        ShadowRenderHook.afterOpaqueFeatures();
    }
}
