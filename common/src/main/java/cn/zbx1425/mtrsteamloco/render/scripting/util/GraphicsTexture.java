package cn.zbx1425.mtrsteamloco.render.scripting.util;

import cn.zbx1425.mtrsteamloco.Main;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.image.*;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.UUID;

@SuppressWarnings("unused")
public class GraphicsTexture implements Closeable {

    private final DynamicTexture dynamicTexture;
    public final Identifier identifier;

    public BufferedImage bufferedImage;
    public Graphics2D graphics;

    public final int width, height;

    public GraphicsTexture(int width, int height) {
        this.width = width;
        this.height = height;

        NativeImage backingNativeImage = new NativeImage(width, height, false);

        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        identifier = Identifier.fromNamespaceAndPath(Main.MOD_ID, String.format("dynamic/graphics/%s", UUID.randomUUID()));
        graphics = bufferedImage.createGraphics();

        dynamicTexture = new DynamicTexture(() -> "MTR-NTE GraphicsTexture (" + this.identifier.toString() + ")", backingNativeImage);
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().getTextureManager().register(identifier, dynamicTexture);
        });
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static BufferedImage createArgbBufferedImage(BufferedImage src) {
        BufferedImage newImage = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = newImage.createGraphics();
        graphics.drawImage(src, 0, 0, null);
        graphics.dispose();
        return newImage;
    }

    public void upload() {
        IntBuffer imgData = IntBuffer.wrap(((DataBufferInt)bufferedImage.getRaster().getDataBuffer()).getData());
        long pixelAddr = dynamicTexture.getPixels().getPointer();
        ByteBuffer target = MemoryUtil.memByteBuffer(pixelAddr, width * height * 4);
        for (int i = 0; i < width * height; i++) {
            // ARGB to RGBA
            int pixel = imgData.get();
            target.put((byte)((pixel >> 16) & 0xFF));
            target.put((byte)((pixel >> 8) & 0xFF));
            target.put((byte)(pixel & 0xFF));
            target.put((byte)((pixel >> 24) & 0xFF));
        }

        Minecraft.getInstance().execute(dynamicTexture::upload);
    }

    @Override
    public void close() {
        Minecraft.getInstance().execute(() -> {
            graphics.dispose();
            Minecraft.getInstance().getTextureManager().release(identifier);
        });
    }
}
