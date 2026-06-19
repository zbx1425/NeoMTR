package cn.zbx1425.mtrsteamloco.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.mappings.Text;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

public class WidgetScrollList extends AbstractScrollWidget {

    public final ArrayList<AbstractWidget> children = new ArrayList<>();
    private AbstractWidget focusedChild = null;

    public WidgetScrollList(int x, int y, int w, int h) {
        super(x, y, w, h, Text.literal(""));
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.translate(this.getX(), this.getY());
        for (AbstractWidget widget : children) {
            widget.extractRenderState(guiGraphics, mouseX - this.getX(), (int) (mouseY + getOffset()) - this.getY(), partialTick);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (isMouseInside(mouseX, mouseY)) {
            double cx = mouseX - this.getX();
            double cy = mouseY + getOffset() - this.getY();
            for (AbstractWidget widget : new ArrayList<>(children)) {
                if (widget.mouseClicked(new MouseButtonEvent(cx, cy, event.buttonInfo()), isDoubleClick)) {
                    focusedChild = widget;
                    return true;
                }
            }
            focusedChild = null;
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (super.mouseDragged(event, deltaX, deltaY)) return true;
        double cx = event.x() - this.getX();
        double cy = event.y() + getOffset() - this.getY();
        for (AbstractWidget widget : new ArrayList<>(children)) {
            if (widget.mouseDragged(new MouseButtonEvent(cx, cy, event.buttonInfo()), deltaX, deltaY)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double cx = event.x() - this.getX();
        double cy = event.y() + getOffset() - this.getY();
        for (AbstractWidget widget : new ArrayList<>(children)) {
            if (widget.mouseReleased(new MouseButtonEvent(cx, cy, event.buttonInfo()))) return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseInside(mouseX, mouseY)) {
            double cx = mouseX - this.getX();
            double cy = mouseY + getOffset() - this.getY();
            for (AbstractWidget widget : new ArrayList<>(children)) {
                if (widget.mouseScrolled(cx, cy, scrollX, scrollY)) return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isMouseInside(mouseX, mouseY)) {
            setFocused(true);
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (focusedChild != null && focusedChild.keyPressed(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (focusedChild != null && focusedChild.charTyped(event)) return true;
        return super.charTyped(event);
    }

    @Override
    protected int getContentHeight() {
        AbstractWidget lastChild = children.isEmpty() ? null : children.get(children.size() - 1);
        if (lastChild == null) return 0;
        return lastChild.getY() + lastChild.getHeight();
    }

    @Override
    protected boolean getScrollBarVisible() {
        return getContentHeight() > height;
    }

    @Override
    protected double getScrollInterval() {
        AbstractWidget lastChild = children.isEmpty() ? null : children.get(children.size() - 1);
        if (lastChild == null) return 0;
        return lastChild.getHeight();
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
}
