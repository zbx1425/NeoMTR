package cn.zbx1425.mtrsteamloco.render.rail;

import cn.zbx1425.mtrsteamloco.render.ByteBufferOutputStream;
import cn.zbx1425.sowcer.batch.BatchManager;
import cn.zbx1425.sowcer.batch.BatchType;
import cn.zbx1425.sowcer.batch.EnqueueProp;
import cn.zbx1425.sowcer.batch.ShaderProp;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcer.model.Model;
import cn.zbx1425.sowcer.model.VertArrays;
import cn.zbx1425.sowcer.object.InstanceBuf;
import cn.zbx1425.sowcer.object.VertBuf;
import cn.zbx1425.sowcer.util.OffHeapAllocator;
// import cn.zbx1425.sowcer.vertex.VertAttrMapping;
// import cn.zbx1425.sowcer.vertex.VertAttrSrc;
// import cn.zbx1425.sowcer.vertex.VertAttrState;
// import cn.zbx1425.sowcer.vertex.VertAttrType;
import cn.zbx1425.sowcer.vertex.HackGlState;
import com.google.common.io.LittleEndianDataOutputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Map;

public class InstancedRailChunk extends RailChunkBase {

    private final InstanceBuf instanceBuf;
    private final VertArrays vertArrays;

    /*private static final VertAttrMapping RAIL_MAPPING = new VertAttrMapping.Builder()
            .set(VertAttrType.POSITION, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.COLOR, VertAttrSrc.INSTANCE_BUF_OR_GLOBAL)
            .set(VertAttrType.UV_TEXTURE, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.UV_OVERLAY, VertAttrSrc.GLOBAL)
            .set(VertAttrType.UV_LIGHTMAP, VertAttrSrc.INSTANCE_BUF_OR_GLOBAL)
            .set(VertAttrType.NORMAL, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.MATRIX_MODEL, VertAttrSrc.INSTANCE_BUF)
            .build();*/

    public InstancedRailChunk(Long chunkId, ModelRef modelRef) {
        super(chunkId, modelRef);
        Model railModel = modelRef.getModel();
        if (railModel != null) {
            instanceBuf = new InstanceBuf(0);
            vertArrays = VertArrays.createAll(railModel, /*RAIL_MAPPING*/ BatchType.INSTANCED, instanceBuf);
        } else {
            instanceBuf = null;
            vertArrays = null;
        }
    }

    @Override
    public void rebuildBuffer(Level world) {
        super.rebuildBuffer(world);
        if (vertArrays == null) return;

        HashSet<Long> seenBoundaryKeys = new HashSet<>();
        int instanceCount = 0;
        for (RailChunkBase.RailTranformList transforms : containingRails.values()) {
            instanceCount += transforms.interiorTransforms().size();
            for (BakedRail.TransformOnBoundary bt : transforms.boundaryTransforms()) {
                if (seenBoundaryKeys.add(bt.dedupHash())) instanceCount++;
            }
        }
        float yMin = 256, yMax = -64;

        ByteBuffer byteBuf = OffHeapAllocator.allocate(instanceCount * /*RAIL_MAPPING.strideInstance*/ 72);
        ByteBufferOutputStream byteArrayOutputStream = new ByteBufferOutputStream(byteBuf, false);
        LittleEndianDataOutputStream oStream = new LittleEndianDataOutputStream(byteArrayOutputStream);

        seenBoundaryKeys.clear();
        for (Map.Entry<BakedRail, RailChunkBase.RailTranformList> entry : containingRails.entrySet()) {
            int color = entry.getKey().color;
            RailChunkBase.RailTranformList transforms = entry.getValue();

            for (Matrix4f pieceMat : transforms.interiorTransforms()) {
                yMin = writeInstance(oStream, pieceMat, color, world, yMin);
                yMax = Math.max(yMax, pieceMat.getTranslationPart().y());
            }
            for (BakedRail.TransformOnBoundary bt : transforms.boundaryTransforms()) {
                if (!seenBoundaryKeys.add(bt.dedupHash())) continue;
                Matrix4f pieceMat = bt.matrix();
                yMin = writeInstance(oStream, pieceMat, color, world, yMin);
                yMax = Math.max(yMax, pieceMat.getTranslationPart().y());
            }
        }

        instanceBuf.size = instanceCount;
        instanceBuf.upload(byteBuf.flip(), /*VertBuf.USAGE_DYNAMIC_DRAW*/VertBuf.USAGE_INSTANCE);
        OffHeapAllocator.free(byteBuf);

        if (yMin > yMax) yMin = yMax;
        setBoundingBox(yMin, yMax);
    }

    private float writeInstance(LittleEndianDataOutputStream oStream, Matrix4f pieceMat,
                                int color, Level world, float yMin) {
        int effectiveColor = RailRenderDispatcher.isHoldingMtrRailRelated ? color : 0xFFFFFFFF;
        try {
            oStream.writeInt(effectiveColor);

            final Vector3f lightPos = pieceMat.getTranslationPart();
            yMin = Math.min(yMin, lightPos.y());
            final BlockPos lightBlockPos = new BlockPos(Mth.floor(lightPos.x()), Mth.floor(lightPos.y() + 0.1), Mth.floor(lightPos.z()));
            final int light = LightCoordsUtil.pack(world.getBrightness(LightLayer.BLOCK, lightBlockPos), world.getBrightness(LightLayer.SKY, lightBlockPos));
            oStream.writeInt(light);

            byte[] lookAtBytes = new byte[4 * 16];
            ByteBuffer matByteBuf = ByteBuffer.wrap(lookAtBytes).order(ByteOrder.nativeOrder());
            FloatBuffer matFloatBuf = matByteBuf.asFloatBuffer();
            pieceMat.store(matFloatBuf);
            oStream.write(lookAtBytes);

            // for (int k = 0; k < RAIL_MAPPING.paddingInstance; k++) oStream.writeByte(0);
        } catch (IOException ignored) {
        }
        return yMin;
    }

    @Override
    public void enqueue(BatchManager batchManager, ShaderProp shaderProp) {
        if (vertArrays == null) return;

        if (instanceBuf.size < 1) return;
        /*VertAttrState*/ HackGlState attrState = new /*VertAttrState().setOverlayUVNoOverlay()*/HackGlState();
        if (!RailRenderDispatcher.isHoldingMtrRailRelated) attrState.setColor(-1);
        batchManager.enqueue(vertArrays, new EnqueueProp(attrState), shaderProp);
    }

    @Override
    public void close() {
        if (vertArrays == null) return;

        vertArrays.close();
        instanceBuf.close();
    }
}
