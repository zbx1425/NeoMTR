package cn.zbx1425.sowcer.batch;

import cn.zbx1425.sowcer.model.VertArrays;
import cn.zbx1425.sowcer.object.VertArray;
import cn.zbx1425.sowcer.shader.ShaderManager;
import cn.zbx1425.sowcer.util.DrawContext;
import com.mojang.blaze3d.systems.RenderPass;
#if DEBUG
import org.lwjgl.opengl.KHRDebug;
#endif

import java.util.*;

public class BatchManager {

    public HashMap<BatchTuple, Queue<RenderCall>> batches = new HashMap<>();

    public void enqueue(VertArrays model, EnqueueProp enqueueProp, ShaderProp shaderProp) {
        for (VertArray vertArray : model.meshList) {
            enqueue(vertArray, enqueueProp, shaderProp);
        }
    }

    public void enqueue(VertArray vertArray, EnqueueProp enqueueProp, ShaderProp shaderProp) {
        Queue<RenderCall> queue = batches.computeIfAbsent(
                new BatchTuple(vertArray.materialProp, shaderProp, vertArray.batchType),
                (key) -> new LinkedList<>()
        );
        queue.add(new RenderCall(vertArray, enqueueProp));
    }

    public void drawAll(ShaderManager shaderManager, DrawContext drawContext) {
        drawContext.recordBatches(batches.size());

        pushDebugGroup("SOWCER");
        // shaderManager.unbindShader();

        for (Map.Entry<BatchTuple, Queue<RenderCall>> entry : batches.entrySet()) {
            if (entry.getKey().materialProp.translucent || entry.getKey().materialProp.cutoutHack) continue;
            drawBatch(shaderManager, entry, drawContext);
        }

        for (Map.Entry<BatchTuple, Queue<RenderCall>> entry : batches.entrySet()) {
            if (!entry.getKey().materialProp.cutoutHack) continue;
            drawBatch(shaderManager, entry, drawContext);
        }

        for (Map.Entry<BatchTuple, Queue<RenderCall>> entry : batches.entrySet()) {
            if (!entry.getKey().materialProp.translucent) continue;
            drawBatch(shaderManager, entry, drawContext);
        }

        popDebugGroup();

        batches.clear();
    }

    private void drawBatch(ShaderManager shaderManager, Map.Entry<BatchTuple, Queue<RenderCall>> entry, DrawContext drawContext) {
        MaterialProp materialProp = entry.getKey().materialProp;
        ShaderProp shaderProp = entry.getKey().shaderProp;
        BatchType batchType = entry.getKey().batchType;
        pushDebugGroup(materialProp.toString());
        // shaderManager.setupShaderBatchState(materialProp, shaderProp);
        try (RenderPass renderPass = shaderManager.setupShaderBatchPass(materialProp, shaderProp, batchType)) {
            Queue<RenderCall> queue = entry.getValue();
            while (!queue.isEmpty()) {
                RenderCall renderCall = queue.poll();
                renderCall.draw(/*shaderProp*/ renderPass );
                drawContext.recordDrawCall(renderCall);
            }
        }
        // shaderManager.cleanupShaderBatchState(materialProp, shaderProp);
        popDebugGroup();
    }

    private static class BatchTuple {

        public MaterialProp materialProp;
        public ShaderProp shaderProp;
        public BatchType batchType;

        public BatchTuple(MaterialProp materialProp, ShaderProp shaderProp, BatchType batchType) {
            this.materialProp = materialProp;
            this.shaderProp = shaderProp;
            this.batchType = batchType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BatchTuple that = (BatchTuple) o;
            return materialProp.equals(that.materialProp) && shaderProp.equals(that.shaderProp) && batchType.equals(that.batchType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(materialProp, shaderProp, batchType);
        }
    }

    public static class RenderCall {

        public VertArray vertArray;
        public EnqueueProp enqueueProp;

        public RenderCall(VertArray vertArray, EnqueueProp enqueueProp) {
            this.vertArray = vertArray;
            this.enqueueProp = enqueueProp;
        }

        public void draw(/*ShaderProp shaderProp*/ RenderPass renderPass) {
            // vertArray.bind();
            // if (enqueueProp.attrState != null) enqueueProp.attrState.applyGlobal(shaderProp);
            // if (vertArray.materialProp.attrState != null) vertArray.materialProp.attrState.applyGlobal(shaderProp);
            // vertArray.mapping.applyToggleableAttr(enqueueProp.attrState, vertArray.materialProp.attrState);
            vertArray.draw(renderPass);
        }
    }

    private void pushDebugGroup(String name) {
#if DEBUG
        KHRDebug.glPushDebugGroup(KHRDebug.GL_DEBUG_SOURCE_APPLICATION, 65472, name);
#endif
    }

    private void popDebugGroup() {
#if DEBUG
        KHRDebug.glPopDebugGroup();
#endif
    }
}
