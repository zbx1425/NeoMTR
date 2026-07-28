package cn.zbx1425.mtrsteamloco.data;

import cn.zbx1425.mtrsteamloco.render.scripting.ScriptHolder;
import cn.zbx1425.sowcerext.model.ModelCluster;
import net.minecraft.network.chat.Component;

import java.io.Closeable;
import java.io.IOException;

public class EyeCandyProperties implements Closeable {

    public Component name;

    public ModelCluster model;
    public ScriptHolder script;

    public int[] voxelShape;

    public EyeCandyProperties(Component name, ModelCluster model, ScriptHolder script, int[] voxelShape) {
        this.name = name;
        this.model = model;
        this.script = script;
        this.voxelShape = voxelShape;
        if(voxelShape != null && voxelShape.length != 6) throw new IllegalStateException("voxelShape expected to have 6 values!");
    }

    @Override
    public void close() throws IOException {
        if (model != null) model.close();
    }
}
