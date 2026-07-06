package cn.zbx1425.sowcer.batch;

import cn.zbx1425.sowcer.shader.BlazeRenderType;
//import cn.zbx1425.sowcer.vertex.VertAttrState;
import cn.zbx1425.sowcer.vertex.HackGlState;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
//import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Properties regarding material. Set during model loading. Affects batching. */
public class MaterialProp {

    /** Name of the shader program. Must be loaded in ShaderManager. */
    public String shaderName;
    /** The texture to use. Null disables texture. */
    public Identifier texture;

    /** The vertex attribute values to use for those specified with VertAttrSrc MATERIAL. */
    // public VertAttrState attrState = new VertAttrState();
    public HackGlState hackGlState = new HackGlState();

    /** If blending should be set up. True for entity_translucent_* and beacon_beam when translucent is true. */
    public boolean translucent = false;
    /** If depth buffer should be written to. False for beacon_beam when translucent is true, true for everything else. */
    public boolean writeDepthBuf = true;
    /** If the renderer should remove rotation components from model and view matrices.
     *  Results in faces on the XY plane always facing the camera. */
    public boolean billboard = false;

    public boolean cutoutHack = false;

    /** Count of rolling sign sub-textures horizontally. */
    public int sheetElementsU = 0;
    /** Count of rolling sign sub-textures vertically. */
    public int sheetElementsV = 0;

    public MaterialProp() {

    }
    public MaterialProp(String shaderName) {
        this.shaderName = shaderName;
    }

    public MaterialProp(DataInputStream dis) throws IOException {
        int len = dis.readInt();
        String content = new String(dis.readNBytes(len), StandardCharsets.UTF_8);
        JsonObject mtlObj = (JsonObject)new JsonParser().parse(content);
        this.shaderName = mtlObj.get("shaderName").getAsString();
        this.texture = mtlObj.get("texture").isJsonNull() ? null : Identifier.parse(mtlObj.get("texture").getAsString());
        // this.attrState.color = mtlObj.get("color").isJsonNull() ? null : mtlObj.get("color").getAsInt();
        // this.attrState.lightmapUV = mtlObj.get("lightmapUV").isJsonNull() ? null : mtlObj.get("lightmapUV").getAsInt();
        this.hackGlState.color = mtlObj.get("color").isJsonNull() ? null : mtlObj.get("color").getAsInt();
        this.hackGlState.lightmap = mtlObj.get("lightmapUV").isJsonNull() ? null : mtlObj.get("lightmapUV").getAsInt();
        this.translucent = mtlObj.has("translucent") && mtlObj.get("translucent").getAsBoolean();
        this.writeDepthBuf = mtlObj.has("writeDepthBuf") && mtlObj.get("writeDepthBuf").getAsBoolean();
        this.billboard = mtlObj.has("billboard") && mtlObj.get("billboard").getAsBoolean();
        this.cutoutHack = mtlObj.has("cutoutHack") && mtlObj.get("cutoutHack").getAsBoolean();
    }

    public static final Identifier WHITE_TEXTURE_LOCATION = Identifier.parse("minecraft:textures/misc/white.png");

    public AbstractTexture getTexture() {
        return Minecraft.getInstance().getTextureManager().getTexture(texture == null ? WHITE_TEXTURE_LOCATION : texture);
    }

    /*public void setupCompositeState() {
        if (texture != null) {
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            textureManager.getTexture(texture).setFilter(false, false);
            RenderSystem.setShaderTexture(0, texture);
        } else {
            RenderSystem.setShaderTexture(0, WHITE_TEXTURE_LOCATION);
        }

        // HACK: To make cutout transparency on beacon_beam work
        if (translucent || cutoutHack) {
            RenderSystem.enableBlend(); // TransparentState
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        } else {
            RenderSystem.disableBlend();
        }
        RenderSystem.enableDepthTest(); // DepthTestState
        RenderSystem.depthFunc(GL33.GL_LEQUAL);
        RenderSystem.enableCull();
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer(); // LightmapState
        Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor(); // OverlayState
        RenderSystem.depthMask(writeDepthBuf); // WriteMaskState
    }*/

    public RenderType getBlazeRenderType() {
        RenderType result;
        Identifier textureToUse = texture == null ? WHITE_TEXTURE_LOCATION : texture;
        result = switch (shaderName) {
            case "rendertype_entity_cutout" -> BlazeRenderType.entityCutout(textureToUse);
            case "rendertype_entity_translucent_cull" -> BlazeRenderType.entityTranslucentCull(textureToUse);
            case "rendertype_beacon_beam" -> /*BlazeRenderType.beaconBeam(textureToUse, translucent)*/ BlazeRenderType.entityTranslucentEmissive(textureToUse);
            default -> BlazeRenderType.entityCutout(textureToUse);
        };
        return result;
    }

    public MaterialProp copy() {
        MaterialProp result = new MaterialProp();
        result.copyFrom(this);
        return result;
    }

    public void copyFrom(MaterialProp other) {
        this.shaderName = other.shaderName;
        this.texture = other.texture;
        // this.attrState = other.attrState.copy();
        this.hackGlState = other.hackGlState;
        this.translucent = other.translucent;
        this.writeDepthBuf = other.writeDepthBuf;
        this.billboard = other.billboard;
        this.sheetElementsU = other.sheetElementsU;
        this.sheetElementsV = other.sheetElementsV;
    }

    public void serializeTo(DataOutputStream dos) throws IOException {
        JsonObject mtlObj = new JsonObject();
        mtlObj.addProperty("version", 2);
        mtlObj.addProperty("shaderName", shaderName);
        if (texture == null) {
            mtlObj.add("texture", new JsonNull());
        } else {
            mtlObj.addProperty("texture", texture.toString());
        }
        // if (this.attrState.color == null) {
        if (this.hackGlState.color == null) {
            mtlObj.add("color", new JsonNull());
        } else {
            // mtlObj.addProperty("color", this.attrState.color);
            mtlObj.addProperty("color", this.hackGlState.color);
        }
        // if (this.attrState.lightmapUV == null) {
        if (this.hackGlState.lightmap == null) {
            mtlObj.add("lightmapUV", new JsonNull());
        } else {
            //mtlObj.addProperty("lightmapUV", this.attrState.lightmapUV);
            mtlObj.addProperty("lightmapUV", this.hackGlState.lightmap);
        }
        mtlObj.addProperty("translucent", this.translucent);
        mtlObj.addProperty("writeDepthBuf", this.writeDepthBuf);
        mtlObj.addProperty("billboard", this.billboard);
        mtlObj.addProperty("cutoutHack", this.cutoutHack);
        String content = mtlObj.toString();
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(contentBytes.length);
        dos.write(contentBytes);
    }

    @Override
    public String toString() {
        return String.format("{%s: %s%s}",
                texture == null ? "null" : texture.toString(), translucent ? " T-" : "", shaderName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaterialProp that = (MaterialProp) o;
        return translucent == that.translucent && writeDepthBuf == that.writeDepthBuf
                && billboard == that.billboard && cutoutHack == that.cutoutHack
                && sheetElementsU == that.sheetElementsU && sheetElementsV == that.sheetElementsV
                && Objects.equals(shaderName, that.shaderName) && Objects.equals(texture, that.texture)
                && Objects.equals(/*attrState*/hackGlState, that./*attrState*/hackGlState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shaderName, texture, /*attrState*/hackGlState, translucent, writeDepthBuf, billboard,
                cutoutHack, sheetElementsU, sheetElementsV);
    }
}
