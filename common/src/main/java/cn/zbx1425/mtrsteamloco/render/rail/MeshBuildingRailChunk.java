package cn.zbx1425.mtrsteamloco.render.rail;

import cn.zbx1425.mtrsteamloco.data.RailModelProperties;
import cn.zbx1425.mtrsteamloco.data.RailModelRegistry;
import cn.zbx1425.sowcer.batch.BatchManager;
import cn.zbx1425.sowcer.batch.BatchType;
import cn.zbx1425.sowcer.batch.EnqueueProp;
import cn.zbx1425.sowcer.batch.ShaderProp;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcer.model.Model;
import cn.zbx1425.sowcer.model.VertArrays;
// import cn.zbx1425.sowcer.vertex.VertAttrMapping;
// import cn.zbx1425.sowcer.vertex.VertAttrSrc;
// import cn.zbx1425.sowcer.vertex.VertAttrState;
// import cn.zbx1425.sowcer.vertex.VertAttrType;
import cn.zbx1425.sowcer.vertex.HackGlState;
import cn.zbx1425.sowcerext.model.RawModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import java.util.HashSet;
import java.util.Map;

public class MeshBuildingRailChunk extends RailChunkBase {

    private final RawModel railModel;

    private Model uploadedCombinedModel;
    private VertArrays vertArrays;

    /*private static final VertAttrMapping RAIL_MAPPING = new VertAttrMapping.Builder()
            .set(VertAttrType.POSITION, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.COLOR, VertAttrSrc.VERTEX_BUF_OR_GLOBAL)
            .set(VertAttrType.UV_TEXTURE, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.UV_OVERLAY, VertAttrSrc.GLOBAL)
            .set(VertAttrType.UV_LIGHTMAP, VertAttrSrc.VERTEX_BUF_OR_GLOBAL)
            .set(VertAttrType.NORMAL, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.MATRIX_MODEL, VertAttrSrc.GLOBAL)
            .build();*/

    protected MeshBuildingRailChunk(Long chunkId, ModelRef modelRef) {
        super(chunkId, modelRef);
        RailModelProperties props = RailModelRegistry.getProperty(modelRef.typeKey);
        if (props.getModelCount() > 0) {
            this.railModel = props.rawModels.get(modelRef.modelIndex % props.getModelCount());
        } else {
            this.railModel = null;
        }
    }

    @Override
    public void rebuildBuffer(Level world) {
        super.rebuildBuffer(world);
        if (railModel == null) return;

        float yMin = 256, yMax = -64;
        RawModel combinedModel = new RawModel();
        HashSet<Long> seenBoundaryKeys = new HashSet<>();

        for (Map.Entry<BakedRail, RailChunkBase.RailTranformList> entry : containingRails.entrySet()) {
            int color = entry.getKey().color;
            RailChunkBase.RailTranformList transforms = entry.getValue();

            for (Matrix4f pieceMat : transforms.interiorTransforms()) {
                final Vector3f lightPos = pieceMat.getTranslationPart();
                yMin = Math.min(yMin, lightPos.y());
                yMax = Math.max(yMax, lightPos.y());
                final BlockPos lightBlockPos = new BlockPos(Mth.floor(lightPos.x()), Mth.floor(lightPos.y() + 0.1), Mth.floor(lightPos.z()));
                final int light = LightCoordsUtil.pack(world.getBrightness(LightLayer.BLOCK, lightBlockPos), world.getBrightness(LightLayer.SKY, lightBlockPos));
                combinedModel.appendTransformed(railModel, pieceMat, color, light);
            }
            for (BakedRail.TransformOnBoundary bt : transforms.boundaryTransforms()) {
                if (!seenBoundaryKeys.add(bt.dedupHash())) continue;
                Matrix4f pieceMat = bt.matrix();
                final Vector3f lightPos = pieceMat.getTranslationPart();
                yMin = Math.min(yMin, lightPos.y());
                yMax = Math.max(yMax, lightPos.y());
                final BlockPos lightBlockPos = new BlockPos(Mth.floor(lightPos.x()), Mth.floor(lightPos.y() + 0.1), Mth.floor(lightPos.z()));
                final int light = LightCoordsUtil.pack(world.getBrightness(LightLayer.BLOCK, lightBlockPos), world.getBrightness(LightLayer.SKY, lightBlockPos));
                combinedModel.appendTransformed(railModel, pieceMat, color, light);
            }
        }

        if (vertArrays != null) vertArrays.close();
        if (uploadedCombinedModel != null) uploadedCombinedModel.close();
        uploadedCombinedModel = combinedModel.upload(BatchType.REGULAR);
        vertArrays = VertArrays.createAll(uploadedCombinedModel, BatchType.REGULAR, null);

        if (yMin > yMax) yMin = yMax;
        setBoundingBox(yMin, yMax);
    }

    @Override
    public void enqueue(BatchManager batchManager, ShaderProp shaderProp) {
        if (railModel == null) return;

        if (vertArrays == null) return;
        /*VertAttrState*/ HackGlState attrState = new /*VertAttrState().setModelMatrix(shaderProp.viewMatrix).setOverlayUVNoOverlay()*/ HackGlState();
        if (!RailRenderDispatcher.isHoldingMtrRailRelated) attrState.setColor(-1);
        batchManager.enqueue(vertArrays, new EnqueueProp(attrState), /*ShaderProp.DEFAULT*/ shaderProp);
    }

    @Override
    public void close() {
        if (vertArrays != null) vertArrays.close();
        if (uploadedCombinedModel != null) uploadedCombinedModel.close();
    }
}
