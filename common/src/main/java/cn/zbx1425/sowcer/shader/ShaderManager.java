package cn.zbx1425.sowcer.shader;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.render.ShadersModHandler;
import cn.zbx1425.sowcer.batch.BatchType;
import cn.zbx1425.sowcer.batch.MaterialProp;
import cn.zbx1425.sowcer.batch.ShaderProp;
import cn.zbx1425.sowcer.util.AttrUtil;
//import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import cn.zbx1425.sowcer.math.Matrix4f;
//import net.minecraft.client.renderer.ShaderInstance;
import mtr.RenderPipelineHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
//import net.minecraft.server.packs.resources.ResourceManager;
//import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.joml.Vector3f;
import org.joml.Vector4f;

//import java.io.IOException;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;

public class ShaderManager {

    /*public static final VertexFormatElement MC_ELEMENT_MATRIX =
            new VertexFormatElement(6, 0, VertexFormatElement.Type.FLOAT, true, 16);*/

    /*public static final VertexFormat INSTANCED_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION).add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0).add("UV1", VertexFormatElement.UV1).add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .build();*/

    /*public static final VertexFormat INSTANCED_FORMAT = VertexFormat.builder()
            .add        ("Position",    VertexFormatElement.POSITION)
            .add        ("UV0",         VertexFormatElement.UV0)
            .add        ("Normal",      VertexFormatElement.NORMAL)
            .padding    (1)
            .build      ();*/

    public static final RenderPipeline ENTITY_CUTOUT_INSTANCED_PIPELINE             = RenderPipelineHelper.asBuilder(RenderPipelines.ENTITY_CUTOUT)                 .withLocation(Main.id("entity_cutout_instanced"))               .withVertexFormat(/*INSTANCED_FORMAT*/DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES).withShaderDefine("NO_OVERLAY").withVertexShader(Main.id("core/entity_instanced")).withUniform("InstanceBuffer", UniformType.TEXEL_BUFFER, TextureFormat.RED8I).build();
    public static final RenderPipeline ENTITY_TRANSLUCENT_CULL_INSTANCED_PIPELINE   = RenderPipelineHelper.asBuilder(RenderPipelines.ENTITY_TRANSLUCENT_CULL)       .withLocation(Main.id("entity_transluent_cull_instanced"))      .withVertexFormat(/*INSTANCED_FORMAT*/DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES).withShaderDefine("NO_OVERLAY").withVertexShader(Main.id("core/entity_instanced")).withUniform("InstanceBuffer", UniformType.TEXEL_BUFFER, TextureFormat.RGBA8).build();
    public static final RenderPipeline ENTITY_EMISSIVE_INSTANCED                    = RenderPipelineHelper.asBuilder(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE)   .withLocation(Main.id("entity_transluent_emissive_instanced"))  .withVertexFormat(/*INSTANCED_FORMAT*/DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES).withShaderDefine("NO_OVERLAY").withVertexShader(Main.id("core/entity_instanced")).withUniform("InstanceBuffer", UniformType.TEXEL_BUFFER, TextureFormat.RGBA8).build();

    public static void registerPipelines(Consumer<RenderPipeline> register) {
        register.accept(ENTITY_CUTOUT_INSTANCED_PIPELINE);
        register.accept(ENTITY_TRANSLUCENT_CULL_INSTANCED_PIPELINE);
        register.accept(ENTITY_EMISSIVE_INSTANCED);
    }

//    public final Map<String, ShaderInstance> shaders = new HashMap<>();

    public boolean isReady() {
//        return this.shaders.size() > 0;
        return true;
    }

    public RenderPass setupShaderBatchPass(MaterialProp materialProp, ShaderProp shaderProp, BatchType batchType) {
        RenderPipeline      renderPipeline;
        TextureTransform    textureTransform;
        OutputTarget        outputTarget;
        boolean             useOverlay;
        boolean             useLightmap;

        if (batchType == BatchType.INSTANCED) {
            renderPipeline = switch (materialProp.shaderName) {
                case "rendertype_entity_cutout"             -> ENTITY_CUTOUT_INSTANCED_PIPELINE;
                case "rendertype_entity_translucent_cull"   -> ENTITY_TRANSLUCENT_CULL_INSTANCED_PIPELINE;
                case "rendertype_beacon_beam"               -> ENTITY_EMISSIVE_INSTANCED;
                default                                     -> throw new IllegalStateException("Cannot get shader: " + materialProp.shaderName);
            };

            textureTransform    = TextureTransform  .DEFAULT_TEXTURING;
            outputTarget        = OutputTarget      .MAIN_TARGET;
            useOverlay          = false;
            useLightmap         = true;
        } else {
            RenderType  renderType  = materialProp  .getBlazeRenderType();
            RenderSetup renderSetup = renderType    .state;

            renderPipeline      = renderSetup.pipeline;
            textureTransform    = renderSetup.textureTransform;
            outputTarget        = renderSetup.outputTarget;
            useOverlay          = renderSetup.useOverlay;
            useLightmap         = renderSetup.useLightmap;
        }

        Matrix4f modelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix()).copy();

        if (shaderProp.viewMatrix != null) {
            modelViewMatrix.multiply(shaderProp.viewMatrix);
        }

        if (materialProp.billboard) {
            AttrUtil.zeroRotation(modelViewMatrix);
        }

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                modelViewMatrix.asMoj(),
                new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                new Vector3f(),
                textureTransform.getMatrix()
        );

        RenderTarget    renderTarget    = outputTarget.getRenderTarget  ();
        AbstractTexture texture         = materialProp.getTexture       ();

        GpuTextureView colorTexture =                           RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : renderTarget.getColorTextureView();
        GpuTextureView depthTexture = renderTarget.useDepth ? ( RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : renderTarget.getDepthTextureView()) : null;

        RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "MTR Immediate draw for " + materialProp.shaderName,
                colorTexture, OptionalInt   .empty(),
                depthTexture, OptionalDouble.empty()
        );

        renderPass.setPipeline(renderPipeline);

        ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();

        if (scissorState.enabled()) {
            renderPass.enableScissor(
                    scissorState.x      (),
                    scissorState.y      (),
                    scissorState.width  (),
                    scissorState.height ()
            );
        }

        renderPass.setUniform("DynamicTransforms", dynamicTransforms);

        renderPass.bindTexture(
                "Sampler0",
                texture.getTextureView  (),
                texture.getSampler      ()
        );

        RenderSystem.bindDefaultUniforms(renderPass);

        if (useOverlay)     renderPass.bindTexture("Sampler1", Minecraft.getInstance().gameRenderer.overlayTexture  ().getTextureView(),    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        if (useLightmap)    renderPass.bindTexture("Sampler2", Minecraft.getInstance().gameRenderer.lightmap        (),                     RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

        return renderPass;
    }

    /*public void reloadShaders(ResourceManager resourceManager) throws IOException {
        this.shaders.values().forEach(ShaderInstance::close);
        this.shaders.clear();
        PatchingResourceProvider provider = new PatchingResourceProvider(resourceManager);

        loadShader(provider, "rendertype_entity_cutout");
        loadShader(provider, "rendertype_entity_translucent_cull");
        loadShader(provider, "rendertype_beacon_beam");
    }

    private void loadShader(ResourceProvider resourceManager, String name) throws IOException {
        ShaderInstance shader = new ShaderInstance(resourceManager, name, MC_FORMAT_ENTITY_MAT);
        shaders.put(name, shader);
    }*/

    /*public void setupShaderBatchState(MaterialProp materialProp, ShaderProp shaderProp) {
        final boolean useCustomShader = ShadersModHandler.canUseCustomShader();
        ShaderInstance shaderInstance;

        if (useCustomShader) {
            shaderInstance = shaders.get(materialProp.shaderName);
            materialProp.setupCompositeState();
        } else {
            RenderType renderType = materialProp.getBlazeRenderType();
            renderType.setupRenderState();
            shaderInstance = RenderSystem.getShader();
        }

        if (shaderInstance == null) {
            throw new IllegalArgumentException("Cannot get shader: " + materialProp.shaderName
                    + (useCustomShader ? "_modelmat" : ""));
        }

        Matrix4f mvMatrix = new Matrix4f(RenderSystem.getModelViewMatrix()).copy();
        if (shaderProp.viewMatrix != null) mvMatrix.multiply(shaderProp.viewMatrix);
        if (materialProp.billboard) AttrUtil.zeroRotation(mvMatrix);
        shaderProp.renderSystemViewMatrix = mvMatrix;

        shaderInstance.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, mvMatrix.asMoj(),
                RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shaderInstance.apply();

        if (shaderInstance.programId != ShaderInstance.lastProgramId) {
            ProgramManager.glUseProgram(shaderInstance.programId);
            ShaderInstance.lastProgramId = shaderInstance.programId;
        }
    }*/

    /*public void cleanupShaderBatchState(MaterialProp materialProp, ShaderProp shaderProp) {
        final boolean useCustomShader = ShadersModHandler.canUseCustomShader();
        if (!useCustomShader) {
            ShaderInstance shaderInstance = RenderSystem.getShader();
            if (shaderInstance != null && shaderInstance.MODEL_VIEW_MATRIX != null) {
                // ModelViewMatrix might have got set in VertAttrState, reset it
                shaderInstance.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
                if (ShadersModHandler.canUseCustomShader()) {
                    shaderInstance.MODEL_VIEW_MATRIX.upload();
                } else {
                    shaderInstance.apply();
                }
            }
        }
    }*/
}
