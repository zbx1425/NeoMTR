package cn.zbx1425.sowcer.object;

import cn.zbx1425.sowcer.util.GlStateTracker;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.nio.ByteBuffer;

public class VertBuf implements Closeable {

    // public int id;
    public GpuBuffer impl;

    /*public static final int USAGE_STATIC_DRAW = GL33.GL_STATIC_DRAW;
    public static final int USAGE_DYNAMIC_DRAW = GL33.GL_DYNAMIC_DRAW;
    public static final int USAGE_STREAM_DRAW = GL33.GL_STREAM_DRAW;*/

    public static final int USAGE_VBO       = GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX;
    public static final int USAGE_IBO       = GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_INDEX;
    public static final int USAGE_INSTANCE  = GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER;

    public VertBuf() {
        
        // id = GL33.glGenBuffers();
    }

    /*public void bind(int target) {
        GlStateTracker.assertProtected();
        GL33.glBindBuffer(target, id);
    }*/

    public void upload(ByteBuffer buffer, int usage) {
        impl = RenderSystem.getDevice().createBuffer(null, usage, buffer);
        /*int vboPrev = GL33.glGetInteger(GL33.GL_ARRAY_BUFFER_BINDING);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, id);
        buffer.clear();
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, buffer, usage);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vboPrev);*/
    }

    public void upload(ByteBuffer buffer, int size, int usage) {
        impl = RenderSystem.getDevice().createBuffer(null, usage, size);

        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(impl.slice(0, size), buffer);
        /*int vboPrev = GL33.glGetInteger(GL33.GL_ARRAY_BUFFER_BINDING);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, id);
        buffer.clear();
        GL33.nglBufferData(GL33.GL_ARRAY_BUFFER, size, MemoryUtil.memAddress0(buffer), usage);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vboPrev);*/
    }

    @Override
    public void close() {
        if (RenderSystem.isOnRenderThread()) {
            /*GL33.glDeleteBuffers(id);
            id = 0;*/
            if (impl != null) {
                impl.close();
            }
        } else {
            Minecraft.getInstance().execute(this::close);
        }
    }
}
