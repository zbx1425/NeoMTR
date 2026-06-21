package cn.zbx1425.mtrsteamloco.gui;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.render.scripting.AbstractScriptContext;
import cn.zbx1425.mtrsteamloco.render.scripting.ScriptContextManager;
import cn.zbx1425.mtrsteamloco.render.scripting.ScriptHolder;
import cn.zbx1425.mtrsteamloco.render.scripting.util.GraphicsTexture;
import com.google.common.base.Splitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptDebugOverlay {

    public static void render(GuiGraphicsExtractor vdStuff) {
        Matrix3x2fStack matrices = vdStuff.pose();
        if (!ClientConfig.enableScriptDebugOverlay) return;
        if (Minecraft.getInstance().screen != null) return;

        matrices.pushMatrix();
        matrices.translate(10, 10);

        Map<ScriptHolder, List<AbstractScriptContext>> contexts = new HashMap<>();
        for (Map.Entry<AbstractScriptContext, ScriptHolder> entry : ScriptContextManager.livingContexts.entrySet()) {
            contexts.computeIfAbsent(entry.getValue(), k -> new java.util.ArrayList<>()).add(entry.getKey());
        }

        int y = 0;
        Font font = Minecraft.getInstance().font;
        int lineHeight = Mth.ceil(font.lineHeight * 1.2f);
        for (Map.Entry<ScriptHolder, List<AbstractScriptContext>> entry : contexts.entrySet()) {
            ScriptHolder holder = entry.getKey();
            synchronized (holder) {
                if (holder.failTime > 0) {
                    drawText(vdStuff, font, holder.name + " FAILED", 0, y, 0xFFFF0000);
                    y += lineHeight;
                    for (String msgLine : Splitter.fixedLength(60).split(holder.failException.getMessage())) {
                        drawText(vdStuff, font, msgLine, 5, y, 0xFFFF8888);
                        y += lineHeight;
                    }
                } else {
                    drawText(vdStuff, font, holder.name, 0, y, 0xFFAAAAFF);
                    y += lineHeight;
                }
            }
            for (AbstractScriptContext context : entry.getValue()) {
                drawText(vdStuff, font,
                        String.format("#%08X (%.2f ms)", context.hashCode(), context.lastExecuteDurationMovingAverage / 1000000.0),
                        10, y, 0xFFCCCCFF);
                y += lineHeight;
                for (Map.Entry<String, Object> debugInfo : context.debugInfo.entrySet()) {
                    Object value = debugInfo.getValue();
                    if (value instanceof GraphicsTexture texture) {
                        float scale = (Minecraft.getInstance().getWindow().getGuiScaledWidth() - 40) / (float) texture.width;
                        blit(vdStuff, texture.identifier, 20, y, (int)(texture.width * scale), (int)(texture.height * scale));
                        drawText(vdStuff, font, debugInfo.getKey() + ": GraphicsTexture", 20, y, 0xFFFFFFFF);
                        y += (int)(texture.height * scale) + lineHeight / 2;
                    } else {
                        drawText(vdStuff, font, debugInfo.getKey() + ": " + debugInfo.getValue(), 20, y, 0xFFFFFFFF);
                        y += lineHeight;
                    }
                }
            }
        }

        matrices.popMatrix();
    }
    
    private static void drawText(GuiGraphicsExtractor guiGraphics, Font font, String text, int x, int y, int color) {
        guiGraphics.text(font, text, x, y, color);
    }
    private static void blit(GuiGraphicsExtractor guiGraphics, Identifier texture, int x, int y, int width, int height) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, width, height, 0, 0, 1, 1, 1, 1);
    }
}
