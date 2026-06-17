package cn.zbx1425.sowcerext.model.integration;

import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcerext.model.Vertex;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.ArrayList;
import java.util.List;

public class FaceList {

    private final RenderType renderType;
    private final boolean needSorting;
    private final List<TransformedFace> queuedFaces = new ArrayList<>();

    public FaceList(RenderType renderType, boolean needSorting) {
        this.renderType = renderType;
        this.needSorting = needSorting;
    }

    public void addFace(Vertex[] vertices, int color, int light) {
        queuedFaces.add(new TransformedFace(vertices, color, light));
    }

    public void commit(MultiBufferSource bufferSource) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        if (needSorting) {
            queuedFaces.sort((a, b) ->
                    -Float.compare(a.sortingVector.distanceSq(Vector3f.ZERO), b.sortingVector.distanceSq(Vector3f.ZERO))
            );
        }
        for (TransformedFace face : queuedFaces) {
            for (Vertex vertex : face.vertices) {
                vertexConsumer
                        .addVertex(vertex.position.x(), vertex.position.y(), vertex.position.z())
                        .setColor((byte)(face.color >>> 24), (byte)(face.color >>> 16), (byte)(face.color >>> 8), (byte)(int)face.color)
                        .setUv(vertex.u, vertex.v)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(face.light)
                        .setNormal(vertex.normal.x(), vertex.normal.y(), vertex.normal.z());
            }
        }
    }

    private static class TransformedFace {
        private final Vertex[] vertices;
        private final Vector3f sortingVector;
        int color;
        int light;

        public TransformedFace(Vertex[] vertices, int color, int light) {
            this.vertices = vertices;
            this.sortingVector = new Vector3f(0, 0, 0);
            for (Vertex vertex : vertices) this.sortingVector.add(vertex.position);
            this.color = color;
            this.light = light;
        }
    }
}
