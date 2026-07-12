package cn.zbx1425.sowcerext.model;

import cn.zbx1425.sowcer.batch.BatchManager;
import cn.zbx1425.sowcer.batch.BatchType;
import cn.zbx1425.sowcer.batch.EnqueueProp;
import cn.zbx1425.sowcer.batch.ShaderProp;
import cn.zbx1425.sowcer.model.Model;
import cn.zbx1425.sowcer.model.VertArrays;
import cn.zbx1425.sowcer.util.AttrUtil;
import cn.zbx1425.sowcer.util.DrawContext;
// import cn.zbx1425.sowcer.vertex.VertAttrMapping;
import cn.zbx1425.sowcer.vertex.HackGlState;
// import cn.zbx1425.sowcer.vertex.VertAttrState;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcerext.model.integration.BufferSourceProxy;
import cn.zbx1425.sowcerext.reuse.ModelManager;
import net.minecraft.resources.Identifier;

import java.io.Closeable;

public class ModelCluster implements Closeable {

    public final VertArrays uploadedOpaqueParts;
    public final RawModel opaqueParts;
    private final Model opaqueModel;
    public final VertArrays uploadedTranslucentParts;
    public final RawModel translucentParts;
    private final Model translucentModel;
    private final ModelManager modelManager;

    public ModelCluster(RawModel source, /*VertAttrMapping*/ BatchType mapping, ModelManager modelManager) {
        this.modelManager = modelManager;
        this.translucentParts = new RawModel();
        this.opaqueParts = new RawModel();
        for (RawMesh mesh : source.meshList.values()) {
            if (mesh.materialProp.translucent) {
                translucentParts.append(mesh);
            } else {
                opaqueParts.append(mesh);
            }
        }
        this.opaqueModel = modelManager.uploadModel(opaqueParts);
        this.uploadedOpaqueParts = VertArrays.createAll(opaqueModel, mapping, null);
        this.translucentModel = modelManager.uploadModel(translucentParts);
        this.uploadedTranslucentParts = VertArrays.createAll(translucentModel, mapping, null);
    }

    private ModelCluster(VertArrays uploadedOpaqueParts, RawModel opaqueParts, VertArrays uploadedTranslucentParts, RawModel translucentParts) {
        this.modelManager = null;
        this.opaqueModel = null;
        this.translucentModel = null;
        this.uploadedOpaqueParts = uploadedOpaqueParts;
        this.opaqueParts = opaqueParts;
        this.uploadedTranslucentParts = uploadedTranslucentParts;
        this.translucentParts = translucentParts;
    }

    public void enqueueOpaqueGl(BatchManager batchManager, Matrix4f pose, int light, DrawContext drawContext) {
        // KHRDebug.glDebugMessageInsert(KHRDebug.GL_DEBUG_SOURCE_APPLICATION, KHRDebug.GL_DEBUG_TYPE_MARKER,
        //        0, KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION, "RenderOptimized " + (source.sourceLocation == null ? "unknown" : source.sourceLocation.toString()));
        int shaderLightmapUV = AttrUtil.exchangeLightmapUVBits(light);
        /*batchManager.enqueue(uploadedOpaqueParts, new EnqueueProp(
                new VertAttrState()
                        .setColor(255, 255, 255, 255).setOverlayUVNoOverlay()
                        .setLightmapUV(shaderLightmapUV).setModelMatrix(pose)
        ), ShaderProp.DEFAULT);*/
        batchManager.enqueue(uploadedOpaqueParts, new EnqueueProp(new HackGlState().setColor(255, 255, 255, 255).setLightmap(shaderLightmapUV)), new ShaderProp().setViewMatrix(pose));
    }

    public void enqueueOpaqueBlaze(BufferSourceProxy vertexConsumers, Matrix4f pose, int light, DrawContext drawContext) {
        opaqueParts.writeBlazeBuffer(vertexConsumers, pose, light, drawContext);
    }

    public void enqueueTranslucentGl(BatchManager batchManager, Matrix4f matrix4f, int light, DrawContext drawContext) {
        int shaderLightmapUV = AttrUtil.exchangeLightmapUVBits(light);
        /*batchManager.enqueue(uploadedTranslucentParts, new EnqueueProp(
                new VertAttrState()
                        .setColor(255, 255, 255, 255).setOverlayUVNoOverlay()
                        .setLightmapUV(shaderLightmapUV).setModelMatrix(matrix4f)
        ), ShaderProp.DEFAULT);*/
        batchManager.enqueue(uploadedTranslucentParts, new EnqueueProp(new HackGlState().setColor(255, 255, 255, 255).setLightmap(shaderLightmapUV)), new ShaderProp().setViewMatrix(matrix4f));
    }

    public void enqueueTranslucentBlaze(BufferSourceProxy vertexConsumers, Matrix4f pose, int light, DrawContext drawContext) {
        translucentParts.writeBlazeBuffer(vertexConsumers, pose, light, drawContext);
    }

    @Override
    public void close(){
        uploadedOpaqueParts.close();
        uploadedTranslucentParts.close();
        if (modelManager != null) {
            if (opaqueModel != null) modelManager.closeModel(opaqueModel);
            if (translucentModel != null) modelManager.closeModel(translucentModel);
        }
    }


    public void replaceTexture(String oldTexture, Identifier newTexture) {
        uploadedOpaqueParts.replaceTexture(oldTexture, newTexture);
        opaqueParts.replaceTexture(oldTexture, newTexture);
        uploadedTranslucentParts.replaceTexture(oldTexture, newTexture);
        translucentParts.replaceTexture(oldTexture, newTexture);
    }

    public void replaceAllTexture(Identifier newTexture) {
        uploadedOpaqueParts.replaceAllTexture(newTexture);
        opaqueParts.replaceAllTexture(newTexture);
        uploadedTranslucentParts.replaceAllTexture(newTexture);
        translucentParts.replaceAllTexture(newTexture);
    }

    public ModelCluster copyForMaterialChanges() {
        return new ModelCluster(
                uploadedOpaqueParts.copyForMaterialChanges(),
                opaqueParts.copyForMaterialChanges(),
                uploadedTranslucentParts.copyForMaterialChanges(),
                translucentParts.copyForMaterialChanges()
        );
    }
}
