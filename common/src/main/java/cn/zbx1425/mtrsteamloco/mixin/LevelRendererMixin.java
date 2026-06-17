package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.sowcerext.model.integration.BufferSourceProxy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 100)
public class LevelRendererMixin {

    // Sodium applies @Overwrite to them so have to inject them all, rather than just setSectionDirty(IIIZ)
    // TODO Will it include unnecessary updates?

    @Inject(method = "setSectionDirtyWithNeighbors", at = @At("HEAD"))
    private void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ, CallbackInfo ci) {
        for (int i = sectionZ - 1; i <= sectionZ + 1; ++i) {
            for (int j = sectionX - 1; j <= sectionX + 1; ++j) {
                MainClient.railRenderDispatcher.registerLightUpdate(j, sectionY - 1, sectionY + 1, i);
            }
        }
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void setSectionDirty(int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread, CallbackInfo ci) {
        MainClient.railRenderDispatcher.registerLightUpdate(sectionX, sectionY, sectionY, sectionZ);
    }

    @Inject(method = "setSectionDirty(III)V", at = @At("HEAD"))
    private void setSectionDirty(int sectionX, int sectionY, int sectionZ, CallbackInfo ci) {
        MainClient.railRenderDispatcher.registerLightUpdate(sectionX, sectionY, sectionY, sectionZ);
    }

}
