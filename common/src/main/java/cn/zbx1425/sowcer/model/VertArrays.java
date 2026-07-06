package cn.zbx1425.sowcer.model;

import cn.zbx1425.sowcer.batch.BatchType;
import cn.zbx1425.sowcer.object.InstanceBuf;
import cn.zbx1425.sowcer.object.VertArray;
// import cn.zbx1425.sowcer.vertex.VertAttrMapping;
import net.minecraft.resources.Identifier;

import java.io.Closeable;
import java.util.ArrayList;

public class VertArrays implements Closeable {

    public final ArrayList<VertArray> meshList = new ArrayList<>();

    public static VertArrays createAll(Model model, /*VertAttrMapping*/ BatchType mapping, InstanceBuf instanceBuf) {
        VertArrays result = new VertArrays();
        for (Mesh mesh : model.meshList) {
            VertArray meshVertArray = new VertArray();
            meshVertArray.create(mesh, mapping, instanceBuf);
            result.meshList.add(meshVertArray);
        }
        return result;
    }

    public void replaceTexture(String oldTexture, Identifier newTexture) {
        for (VertArray vertArray : meshList) {
            if (vertArray.materialProp.texture == null) continue;
            String oldPath = vertArray.materialProp.texture.getPath();
            if (oldPath.substring(oldPath.lastIndexOf("/") + 1).equals(oldTexture)) {
                vertArray.materialProp.texture = newTexture;
            }
        }
    }

    public void replaceAllTexture(Identifier newTexture) {
        for (VertArray vertArray : meshList) {
            vertArray.materialProp.texture = newTexture;
        }
    }

    public VertArrays copyForMaterialChanges() {
        VertArrays result = new VertArrays();
        for (VertArray vertArray : meshList) {
            VertArray newVertArray = vertArray.copyForMaterialChanges();
            result.meshList.add(newVertArray);
        }
        return result;
    }

    @Override
    public void close() {
        for (VertArray mesh : meshList) {
            mesh.close();
        }
    }
}
