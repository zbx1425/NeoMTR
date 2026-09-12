package cn.zbx1425.mtrsteamloco.render;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.util.GlStateTracker;
import cn.zbx1425.sowcerext.model.integration.BufferSourceProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class ShadowRenderHook {

    public static void afterOpaqueFeatures() {
        ShadersModHandler.setShadowRenderHookAvailable(true);

        org.joml.Matrix4f shadowMV = ShadersModHandler.getShadowModelView();
        if (shadowMV == null) return;

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();

        Matrix4f viewMatrix = new Matrix4f(new org.joml.Matrix4f(shadowMV));
        viewMatrix.translate(-(float) cameraPos.x, -(float) cameraPos.y, -(float) cameraPos.z);

        if (ClientConfig.getRailRenderLevel() >= 2) {
            GlStateTracker.capture();
            MainClient.railRenderDispatcher.drawRailsWithoutHousekeeping(MainClient.drawScheduler.batchManager, viewMatrix);
            MainClient.drawScheduler.commitRaw(MainClient.drawContext);
            GlStateTracker.restore();
        }

        boolean prevDrawWithBlaze = MainClient.drawContext.drawWithBlaze;
        boolean prevSortTranslucentFaces = MainClient.drawContext.sortTranslucentFaces;
        MainClient.drawContext.drawWithBlaze = false;
        MainClient.drawContext.sortTranslucentFaces = false;
        BufferSourceProxy proxy = new BufferSourceProxy(Minecraft.getInstance().renderBuffers().bufferSource());
        MainClient.drawScheduler.commit(proxy, MainClient.drawContext);
        proxy.commit();
        MainClient.drawContext.drawWithBlaze = prevDrawWithBlaze;
        MainClient.drawContext.sortTranslucentFaces = prevSortTranslucentFaces;
    }
}
