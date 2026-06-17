package cn.zbx1425.mtrsteamloco.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.mappings.Text;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;

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
        PoseStack poseStack = guiGraphics.pose();
        poseStack.translate(this.getX(), this.getY(), 0.0);
        for (AbstractWidget widget : children) {
            widget.render(guiGraphics, mouseX - this.getX(), (int) (mouseY + getOffset()) - this.getY(), partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseInside(mouseX, mouseY)) {
            double cx = mouseX - this.getX();
            double cy = mouseY + getOffset() - this.getY();
            for (AbstractWidget widget : new ArrayList<>(children)) {
                if (widget.mouseClicked(cx, cy, button)) {
                    focusedChild = widget;
                    return true;
                }
            }
            focusedChild = null;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        double cx = mouseX - this.getX();
        double cy = mouseY + getOffset() - this.getY();
        for (AbstractWidget widget : new ArrayList<>(children)) {
            if (widget.mouseDragged(cx, cy, button, dragX, dragY)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double cx = mouseX - this.getX();
        double cy = mouseY + getOffset() - this.getY();
        for (AbstractWidget widget : new ArrayList<>(children)) {
            if (widget.mouseReleased(cx, cy, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedChild != null && focusedChild.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char ch, int modifiers) {
        if (focusedChild != null && focusedChild.charTyped(ch, modifiers)) return true;
        return super.charTyped(ch, modifiers);
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
