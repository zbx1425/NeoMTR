package cn.zbx1425.sowcer.shader;

import cn.zbx1425.mtrsteamloco.render.ShadersModHandler;
import cn.zbx1425.sowcer.batch.MaterialProp;
import cn.zbx1425.sowcer.batch.ShaderProp;
import cn.zbx1425.sowcer.util.AttrUtil;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.Window;
//import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import cn.zbx1425.sowcer.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
//import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.IOException;

public class ShaderManager {

    public static final VertexFormatElement MC_ELEMENT_MATRIX =
            new VertexFormatElement(6, 0, VertexFormatElement.Type.FLOAT, true, 16);

    public static final VertexFormat MC_FORMAT_ENTITY_MAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION).add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0).add("UV1", VertexFormatElement.UV1).add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .add("ModelMat", MC_ELEMENT_MATRIX)
            .padding(1)
            .build();

//    public final Map<String, ShaderInstance> shaders = new HashMap<>();

    public boolean isReady() {
//        return this.shaders.size() > 0;
        return false;
    }

    public void reloadShaders(ResourceManager resourceManager) throws IOException {
//        this.shaders.values().forEach(ShaderInstance::close);
//        this.shaders.clear();
        PatchingResourceProvider provider = new PatchingResourceProvider(resourceManager);

        loadShader(provider, "rendertype_entity_cutout");
        loadShader(provider, "rendertype_entity_translucent_cull");
        loadShader(provider, "rendertype_beacon_beam");
    }

    private void loadShader(ResourceProvider resourceManager, String name) throws IOException {
//        ShaderInstance shader = new ShaderInstance(resourceManager, name, MC_FORMAT_ENTITY_MAT);
//        shaders.put(name, shader);
    }

    public void setupShaderBatchState(MaterialProp materialProp, ShaderProp shaderProp) {
        final boolean useCustomShader = ShadersModHandler.canUseCustomShader();
//        ShaderInstance shaderInstance;

        if (useCustomShader) {
//            shaderInstance = shaders.get(materialProp.shaderName);
            materialProp.setupCompositeState();
        } else {
            RenderType renderType = materialProp.getBlazeRenderType();
//            renderType.setupRenderState();
//            shaderInstance = RenderSystem.getShader();
        }

//        if (shaderInstance == null) {
//            throw new IllegalArgumentException("Cannot get shader: " + materialProp.shaderName
//                    + (useCustomShader ? "_modelmat" : ""));
//        }

        Matrix4f mvMatrix = new Matrix4f(RenderSystem.getModelViewMatrix()).copy();
        if (shaderProp.viewMatrix != null) mvMatrix.multiply(shaderProp.viewMatrix);
        if (materialProp.billboard) AttrUtil.zeroRotation(mvMatrix);
        shaderProp.renderSystemViewMatrix = mvMatrix;

//        shaderInstance.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, mvMatrix.asMoj(),
//                RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
//        shaderInstance.apply();
//
//        if (shaderInstance.programId != ShaderInstance.lastProgramId) {
//            ProgramManager.glUseProgram(shaderInstance.programId);
//            ShaderInstance.lastProgramId = shaderInstance.programId;
//        }
    }

    public void cleanupShaderBatchState(MaterialProp materialProp, ShaderProp shaderProp) {
        final boolean useCustomShader = ShadersModHandler.canUseCustomShader();
        if (!useCustomShader) {
//            ShaderInstance shaderInstance = RenderSystem.getShader();
//            if (shaderInstance != null && shaderInstance.MODEL_VIEW_MATRIX != null) {
//                // ModelViewMatrix might have got set in VertAttrState, reset it
//                shaderInstance.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
//                if (ShadersModHandler.canUseCustomShader()) {
//                    shaderInstance.MODEL_VIEW_MATRIX.upload();
//                } else {
//                    shaderInstance.apply();
//                }
//            }
        }
    }

}
