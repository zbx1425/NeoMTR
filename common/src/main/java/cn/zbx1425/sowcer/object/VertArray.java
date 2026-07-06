package cn.zbx1425.sowcer.object;

import cn.zbx1425.sowcer.batch.BatchType;
import cn.zbx1425.sowcer.batch.MaterialProp;
import cn.zbx1425.sowcer.model.Mesh;
// import cn.zbx1425.sowcer.util.GlStateTracker;
// import cn.zbx1425.sowcer.vertex.VertAttrMapping;
import com.mojang.blaze3d.systems.RenderPass;
// import com.mojang.blaze3d.systems.RenderSystem;
// import net.minecraft.client.Minecraft;
// import org.lwjgl.opengl.GL33;

import java.io.Closeable;

public class VertArray implements Closeable {

    // public int id;
    public MaterialProp materialProp;
    public VertBuf vertBuf;
    public IndexBuf indexBuf;
    public InstanceBuf instanceBuf;
    //public VertAttrMapping mapping;
    public BatchType batchType;

    public VertArray() {
        // id = GL33.glGenVertexArrays();
    }

    private VertArray(VertArray other) {
        // this.id = other.id;
        this.materialProp = other.materialProp;
        this.vertBuf = other.vertBuf;
        this.indexBuf = other.indexBuf;
        this.instanceBuf = other.instanceBuf;
        this.batchType = other.batchType;
        //this.mapping = other.mapping;
    }

    public void create(Mesh mesh, /*VertAttrMapping mapping*/ BatchType batchType, InstanceBuf instanceBuf) {
        this.materialProp = mesh.materialProp;
        this.vertBuf = mesh.vertBuf;
        this.indexBuf = mesh.indexBuf;
        this.instanceBuf = instanceBuf;
        this.batchType = batchType;
        // this.mapping = mapping;
        // GL33.glBindVertexArray(id);
        // mapping.setupAttrsToVao(mesh.vertBuf, instanceBuf);
        // mesh.indexBuf.bind(GL33.GL_ELEMENT_ARRAY_BUFFER);
        // unbind();
    }

    /*public void bind() {
        GlStateTracker.assertProtected();
        GL33.glBindVertexArray(id);
    }

    public static void unbind() {
        GlStateTracker.assertProtected();
        GL33.glBindVertexArray(0);
    }

    public void draw() {
        if (instanceBuf == null) {
            GL33.glDrawElements(GL33.GL_TRIANGLES, indexBuf.vertexCount, indexBuf.indexType, 0L);
        } else {
            if (instanceBuf.size < 1) return;
            GL33.glDrawElementsInstanced(GL33.GL_TRIANGLES, indexBuf.vertexCount, indexBuf.indexType, 0L, instanceBuf.size);
        }
    }*/

    public void draw(RenderPass renderPass) {
        renderPass.setVertexBuffer(0, vertBuf.impl);
        renderPass.setIndexBuffer(indexBuf.impl, indexBuf.indexType);

        if (instanceBuf != null) {
            renderPass.setUniform("InstanceBuffer", instanceBuf.impl);
            renderPass.drawIndexed(0, 0, indexBuf.vertexCount, instanceBuf.size);
        } else {
            renderPass.drawIndexed(0, 0, indexBuf.vertexCount, 1);
        }
    }

    public int getFaceCount() {
        return indexBuf.faceCount * (instanceBuf == null ? 1 : instanceBuf.size);
    }

    public BatchType getBatchType() {
        return batchType;
    }

    public VertArray copyForMaterialChanges() {
        VertArray result = new VertArray(this);
        result.materialProp = result.materialProp.copy();
        return result;
    }

    @Override
    public void close() {
        /*if (RenderSystem.isOnRenderThread()) {
            GL33.glDeleteVertexArrays(id);
            id = 0;
        } else {
            Minecraft.getInstance().execute(this::close);
        }*/
    }
}
