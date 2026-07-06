package cn.zbx1425.mtrsteamloco.data;

import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcer.model.Model;
// import cn.zbx1425.sowcer.vertex.VertAttrType;
import cn.zbx1425.sowcerext.model.RawMesh;
import cn.zbx1425.sowcerext.model.RawModel;
import cn.zbx1425.sowcerext.model.Vertex;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RailModelProperties {

    public Component name;

    public List<RawModel> rawModels;
    public List<Model> uploadedModels;
    public List<Long> boundingBoxes;

    /** Combined bounding box (union of all sub-models). */
    public Long boundingBox;
    public float repeatInterval;

    public float yOffset;

    public boolean tiltToGradient;

    /** @deprecated Use {@link #rawModels} get(0) instead. Kept for transitional compatibility. */
    @Deprecated
    public RawModel rawModel;
    /** @deprecated Use {@link #uploadedModels} get(0) instead. */
    @Deprecated
    public Model uploadedModel;

    public RailModelProperties(Component name, RawModel rawModel, float repeatInterval,
                               float yOffset, boolean tiltToGradient) {
        this(name, rawModel == null ? Collections.<RawModel>emptyList() : Collections.singletonList(rawModel),
                repeatInterval, yOffset, tiltToGradient);
    }

    public RailModelProperties(Component name, List<RawModel> rawModels, float repeatInterval,
                               float yOffset, boolean tiltToGradient) {
        this.name = name;
        this.yOffset = yOffset;
        this.repeatInterval = repeatInterval;
        this.tiltToGradient = tiltToGradient;

        if (rawModels.isEmpty()) {
            this.rawModels = Collections.emptyList();
            this.uploadedModels = Collections.emptyList();
            this.boundingBoxes = Collections.emptyList();
            this.boundingBox = 0L;
            this.rawModel = null;
            this.uploadedModel = null;
            return;
        }

        this.rawModels = new ArrayList<>(rawModels.size());
        this.uploadedModels = new ArrayList<>(rawModels.size());
        this.boundingBoxes = new ArrayList<>(rawModels.size());

        float globalYMin = 0f, globalYMax = 0f;

        for (RawModel rm : rawModels) {
            // rm.clearAttrState(VertAttrType.COLOR);
            rm.applyRotation(new Vector3f(0.577f, 0.577f, 0.577f), (float) Math.toRadians(1));
            this.rawModels.add(rm);
            this.uploadedModels.add(MainClient.modelManager.uploadModel(rm));

            float yMin = 0f, yMax = 0f;
            for (RawMesh mesh : rm.meshList.values()) {
                for (Vertex vertex : mesh.vertices) {
                    yMin = Math.min(yMin, vertex.position.y() + yOffset);
                    yMax = Math.max(yMax, vertex.position.y() + yOffset);
                }
            }
            this.boundingBoxes.add(((long) Float.floatToIntBits(yMin) << 32) | (long) Float.floatToIntBits(yMax));
            globalYMin = Math.min(globalYMin, yMin);
            globalYMax = Math.max(globalYMax, yMax);
        }

        this.boundingBox = ((long) Float.floatToIntBits(globalYMin) << 32) | (long) Float.floatToIntBits(globalYMax);
        this.rawModel = this.rawModels.get(0);
        this.uploadedModel = this.uploadedModels.get(0);
    }

    public int getModelCount() {
        return rawModels.size();
    }
}
