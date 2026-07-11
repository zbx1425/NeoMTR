package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.sowcer.vertex.HackGlState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = { "com.mojang.blaze3d.opengl.GlCommandEncoder" })
public class GlCommandEncoderMixin {

    @Inject(method = "drawFromBuffers", at = @At(
        value = "INVOKE",
        target = "Lcom/mojang/blaze3d/opengl/VertexArrayCache;bindVertexArray(Lcom/mojang/blaze3d/vertex/VertexFormat;Lcom/mojang/blaze3d/opengl/GlBuffer;)V",
        shift = At.Shift.AFTER
    ))
    public void drawFromBuffers(CallbackInfo ci) {
        HackGlState.applyPendingState();
    }
}
