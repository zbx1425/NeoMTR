package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.render.rail.RailRenderDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow @Final Minecraft minecraft;
    @Unique private Boolean hideGuiOptionCache = null;

    @Inject(method = "getProjectionMatrix", at = @At("TAIL"), cancellable = true)
    void getProjectionMatrixTail(double fov, CallbackInfoReturnable<org.joml.Matrix4f> cir) {
        if (RailRenderDispatcher.isPreviewingModel) {
            org.joml.Matrix4f result = new org.joml.Matrix4f();
            result.translation(0.5f, 0f, 0f);
            result.scale(0.8f, 0.8f, 1f);
            result.mul(cir.getReturnValue());

            cir.setReturnValue(result);
        }
        if (RailRenderDispatcher.isPreviewingModel && hideGuiOptionCache == null) {
            hideGuiOptionCache = minecraft.options.hideGui;
            minecraft.options.hideGui = true;
        } else if (!RailRenderDispatcher.isPreviewingModel && hideGuiOptionCache != null) {
            minecraft.options.hideGui = hideGuiOptionCache;
            hideGuiOptionCache = null;
        }
    }
}
