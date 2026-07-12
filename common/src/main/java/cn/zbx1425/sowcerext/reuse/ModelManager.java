package cn.zbx1425.sowcerext.reuse;

import cn.zbx1425.sowcer.batch.BatchType;
import cn.zbx1425.sowcer.model.Mesh;
import cn.zbx1425.sowcer.model.Model;
// import cn.zbx1425.sowcer.vertex.VertAttrMapping;
// import cn.zbx1425.sowcer.vertex.VertAttrSrc;
// import cn.zbx1425.sowcer.vertex.VertAttrType;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.model.RawMesh;
import cn.zbx1425.sowcerext.model.RawModel;
import cn.zbx1425.sowcerext.model.loader.CsvModelLoader;
import cn.zbx1425.sowcerext.model.loader.NmbModelLoader;
import cn.zbx1425.sowcerext.model.loader.ObjModelLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.util.*;

public class ModelManager {

    public HashMap<Identifier, Model> modelCache = new HashMap<>();
    public IdentityHashMap<Model, RawModel> modelSources = new IdentityHashMap<>();
    public HashMap<Identifier, ModelCluster> modelClusterCache = new HashMap<>();
    public HashMap<Identifier, RawModel> rawModelCache = new HashMap<>();

    public int vaoCount, vboCount;

    /*public static final VertAttrMapping DEFAULT_MAPPING = new VertAttrMapping.Builder()
            .set(VertAttrType.POSITION, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.COLOR, VertAttrSrc.GLOBAL)
            .set(VertAttrType.UV_TEXTURE, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.UV_OVERLAY, VertAttrSrc.GLOBAL)
            .set(VertAttrType.UV_LIGHTMAP, VertAttrSrc.GLOBAL)
            .set(VertAttrType.NORMAL, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.MATRIX_MODEL, VertAttrSrc.GLOBAL)
            .build();*/

    public void clear() {
        vaoCount = 0;
        for (ModelCluster vertArrays : modelClusterCache.values()) {
            vertArrays.close();
        }
        modelClusterCache.clear();
        vboCount = 0;
        for (Model model : modelSources.keySet()) {
            model.close();
        }
        modelCache.clear();
        modelSources.clear();
        rawModelCache.clear();
    }

    public void clearNamespace(String namespace) {
        modelClusterCache.entrySet().stream()
                .filter(k -> k.getKey().getNamespace().equals(namespace))
                .forEach(k -> {
                    vaoCount -= k.getValue().uploadedOpaqueParts == null ? 0 : k.getValue().uploadedOpaqueParts.meshList.size();
                    k.getValue().close();
                });
        modelClusterCache.keySet().removeIf(k -> k.getNamespace().equals(namespace));
        Set<Model> removedModels = new HashSet<>();
        modelCache.entrySet().stream()
                .filter(k -> k.getKey().getNamespace().equals(namespace))
                .forEach(k -> {
                    vboCount -= k.getValue().meshList.size();
                    removedModels.add(k.getValue());
                    k.getValue().close();
                });
        modelCache.keySet().removeIf(k -> k.getNamespace().equals(namespace));
        for (Model removed : removedModels) {
            modelSources.remove(removed);
        }
        rawModelCache.keySet().removeIf(k -> k.getNamespace().equals(namespace));
    }

    public RawModel loadRawModel(ResourceManager resourceManager, Identifier objLocation, AtlasManager atlasManager) throws IOException {
        if (rawModelCache.containsKey(objLocation)) return rawModelCache.get(objLocation);
        String crntStatExt = FilenameUtils.getExtension(objLocation.getPath());
        RawModel result;
        switch (crntStatExt) {
            case "obj":
                result = ObjModelLoader.loadModel(resourceManager, objLocation, atlasManager);
                break;
            case "csv":
                result = CsvModelLoader.loadModel(resourceManager, objLocation, atlasManager);
                break;
            case "nmb":
                result = NmbModelLoader.loadModel(resourceManager, objLocation, atlasManager);
                // result = CsvModelLoader.loadModel(resourceManager, Identifier.parse(objLocation.toString().replace(".nmb", ".csv")), atlasManager);
                break;
            case "animated":
                throw new IllegalArgumentException("ANIMATED model cannot be loaded as RawModel.");
            default:
                throw new IllegalArgumentException("Unknown model format: " + resourceManager);
        };
        rawModelCache.put(objLocation, result);
        return result;
    }

    public Map<String, RawModel> loadPartedRawModel(ResourceManager resourceManager, Identifier objLocation, AtlasManager atlasManager) throws IOException {
        String crntStatExt = FilenameUtils.getExtension(objLocation.getPath());
        Map<String, RawModel> result;
        switch (crntStatExt) {
            case "obj":
                result = ObjModelLoader.loadModels(resourceManager, objLocation, atlasManager);
                break;
            case "csv":
            case "nmb":
                throw new IllegalArgumentException("CSV/NMB model cannot be loaded as parted RawModel.");
            case "animated":
                throw new IllegalArgumentException("ANIMATED model cannot be loaded as RawModel.");
            default:
                throw new IllegalArgumentException("Unknown model format: " + resourceManager);
        };
        return result;
    }

    public Model uploadModel(RawModel rawModel) {
        if (rawModel.sourceLocation != null) {
            Model cached = modelCache.get(rawModel.sourceLocation);
            if (cached != null) return cached;
        }
        Model result = rawModel.upload(/*DEFAULT_MAPPING*/ BatchType.REGULAR);
        vboCount += result.meshList.size();
        modelSources.put(result, rawModel);
        if (rawModel.sourceLocation != null) {
            modelCache.put(rawModel.sourceLocation, result);
        }
        return result;
    }

    public void closeModel(Model model) {
        modelSources.remove(model);
        modelCache.values().remove(model);
        vboCount -= model.meshList.size();
        model.close();
    }

    public void reUploadAllModels() {
        for (Map.Entry<Model, RawModel> entry : modelSources.entrySet()) {
            Model model = entry.getKey();
            RawModel rawModel = entry.getValue();
            Iterator<Mesh> meshIt = model.meshList.iterator();
            for (RawMesh rawMesh : rawModel.meshList.values()) {
                if (rawMesh.faces.isEmpty()) continue;
                if (!meshIt.hasNext()) break;
                Mesh mesh = meshIt.next();
                rawMesh.upload(mesh, BatchType.REGULAR);
            }
        }
    }

    public ModelCluster uploadVertArrays(RawModel rawModel) {
        if (rawModel.sourceLocation == null) {
            ModelCluster result = new ModelCluster(rawModel, /*DEFAULT_MAPPING*/ BatchType.REGULAR, this);
            vaoCount += result.uploadedOpaqueParts == null ? 0 : result.uploadedOpaqueParts.meshList.size();
            modelClusterCache.put(Identifier.parse("sowcerext-anonymous:vertarrays/" + UUID.randomUUID()), result);
            return result;
        } else {
            if (modelClusterCache.containsKey(rawModel.sourceLocation)) return modelClusterCache.get(rawModel.sourceLocation);
            ModelCluster result = new ModelCluster(rawModel, /*DEFAULT_MAPPING*/ BatchType.REGULAR, this);
            vaoCount += result.uploadedOpaqueParts == null ? 0 : result.uploadedOpaqueParts.meshList.size();
            modelClusterCache.put(rawModel.sourceLocation, result);
            return result;
        }
    }

}
