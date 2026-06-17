package cn.zbx1425.mtrsteamloco.gui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class WidgetLabel extends AbstractWidget {

    public boolean alignR = false;

    private final Runnable onClick;

    public WidgetLabel(int x, int y, int width, Component text) {
        super(x, y, width, 10, text);
        this.onClick = null;
    }

    public WidgetLabel(int x, int y, int width, Component text, Runnable onClick) {
        super(x, y, width, 10, text);
        this.onClick = onClick;
    }

    @Override
    public void renderWidget(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        String[] lines = this.getMessage().getString().split("\n");
        this.height = lines.length * 10;
        for (int i = 0; i < lines.length; ++i) {
            int textWidth = Minecraft.getInstance().font.width(lines[i]);
            int x = alignR ? this.getX() + this.getWidth() - textWidth : this.getX();
            int y = this.getY() + 10 * i;
            if (textWidth > this.width) {
                int offset = (int)(System.currentTimeMillis() / 25 % (textWidth + 40));
                AbstractScrollWidget.vcEnableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
                guiGraphics.text(Minecraft.getInstance().font, lines[i], x - offset, y, -1);
                guiGraphics.text(Minecraft.getInstance().font, lines[i], x + textWidth + 40 - offset, y, -1);
                RenderSystem.disableScissor();
            } else {
                guiGraphics.text(Minecraft.getInstance().font, lines[i], x, y, -1);
            }
            if (!isActive()) {
                guiGraphics.text(Minecraft.getInstance().font, "▶", x - 8, y, 0xffff0000);
            }
        }
    }

    @Override
    public void onClick(double d, double e) {
        super.onClick(d, e);
        if (onClick != null) onClick.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
}
