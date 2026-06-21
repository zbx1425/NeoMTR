package cn.zbx1425.mtrsteamloco.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;

public abstract class AbstractScrollWidget extends AbstractWidget {
    private double offset;
    private boolean holdingScrollBar;

    public AbstractScrollWidget(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (!this.visible) return false;
        boolean clickInside = this.isMouseInside(event.x(), event.y());
        boolean clickBar = this.getScrollBarVisible() && event.x() >= (double)(this.getX() + this.width) && event.x() <= (double)(this.getX() + this.width + 8) && event.y() >= (double)this.getY() && event.y() < (double)(this.getY() + this.height);
        this.setFocused(clickInside || clickBar);
        if (clickBar && event.button() == 0) {
            this.holdingScrollBar = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.holdingScrollBar = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!(this.visible && this.isFocused() && this.holdingScrollBar)) return false;
        if (event.y() < (double)this.getY()) {
            this.setOffset(0.0);
        } else if (event.y() > (double)(this.getY() + this.height)) {
            this.setOffset(this.getMaxOffset());
        } else {
            int i = this.getScrollBarHeight();
            double d = Math.max(1, this.getMaxOffset() / (this.height - i));
            this.setOffset(this.offset + deltaY * d);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
        if (!this.visible || !this.isFocused()) return false;
        this.setOffset(this.offset - delta * this.getScrollInterval());
        return true;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        this.renderBackground(guiGraphics);
        guiGraphics.enableScissor(getX(), getY(), getX() + this.width, getY() + this.height);
        poseStack.translate(0, (float)-this.offset);
        this.renderContents(guiGraphics, mouseX, mouseY, partialTick);
        poseStack.popMatrix();
        guiGraphics.disableScissor();
        if (this.getScrollBarVisible()) {
            this.renderScrollBar(guiGraphics);
        }
    }

    private int getScrollBarHeight() {
        return Mth.clamp((int)((float)(this.height * this.height) / (float)this.getContentHeight()), 32, this.height);
    }

    protected double getOffset() {
        return this.offset;
    }

    protected void setOffset(double offset) {
        this.offset = Mth.clamp(offset, 0.0, this.getMaxOffset());
    }

    protected int getMaxOffset() {
        return Math.max(0, this.getContentHeight() - this.height);
    }

    protected void renderBackground(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.fill(this.getX(), this.getY() + 1, this.getX() + this.width, this.getY() + this.height - 1, this.isFocused() ? 0xffffffff : 0xffa0a0a0);
        guiGraphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, 0xff555555);
    }

    private void renderScrollBar(GuiGraphicsExtractor guiGraphics) {
        int h = this.getScrollBarHeight();
        int x1 = this.getX() + this.width;
        int x2 = this.getX() + this.width + 8;
        int y1 = Math.max(this.getY(), (int)this.offset * (this.height - h) / this.getMaxOffset() + this.getY());
        int y2 = y1 + h;

        // TODO:
//        RenderSystem.setShader(GameRenderer::getPositionColorShader);
//        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
//        Tesselator tesselator = Tesselator.getInstance();
//        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
//        bufferBuilder.addVertex(x1, y2, 0).setColor(128, 128, 128, 255);
//        bufferBuilder.addVertex(x2, y2, 0).setColor(128, 128, 128, 255);
//        bufferBuilder.addVertex(x2, y1, 0).setColor(128, 128, 128, 255);
//        bufferBuilder.addVertex(x1, y1, 0).setColor(128, 128, 128, 255);
//        bufferBuilder.addVertex(x1, y2 - 1, 0).setColor(192, 192, 192, 255);
//        bufferBuilder.addVertex(x2 - 1, y2 - 1, 0).setColor(192, 192, 192, 255);
//        bufferBuilder.addVertex(x2 - 1, y1, 0).setColor(192, 192, 192, 255);
//        bufferBuilder.addVertex(x1, y1, 0).setColor(192, 192, 192, 255);

        // Shadow
        guiGraphics.fill(x1, y1, x2, y2, ARGB.color(255, 128, 128, 128));
        // Scroll bar
        guiGraphics.fill(x1, y1, x2-1, y2-1, ARGB.color(255, 192, 192, 192));

//        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    protected boolean isMouseInside(double x, double y) {
        return x >= (double)this.getX() && x < (double)(this.getX() + this.width) && y >= (double)this.getY() && y < (double)(this.getY() + this.height);
    }

    protected abstract int getContentHeight();

    protected abstract boolean getScrollBarVisible();

    protected abstract double getScrollInterval();

    protected abstract void renderContents(GuiGraphicsExtractor var1, int var2, int var3, float var4);
}

