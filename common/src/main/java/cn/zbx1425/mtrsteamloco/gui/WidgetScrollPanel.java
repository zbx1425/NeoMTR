package cn.zbx1425.mtrsteamloco.gui;

import mtr.mappings.Text;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

public class WidgetScrollPanel extends AbstractScrollWidget {

    public final ArrayList<AbstractWidget> children = new ArrayList<>();
    private AbstractWidget focusedChild = null;

    public WidgetScrollPanel(int x, int y, int w, int h) {
        super(x, y, w, h, Text.literal(""));
    }

    @Override
    protected void renderBackground(GuiGraphicsExtractor guiGraphics) { }

    @Override
    protected void renderContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int offsetY = (int) getOffset();
        for (AbstractWidget widget : children) {
            int origX = widget.getX();
            int origY = widget.getY();
            widget.setX(origX + this.getX());
            widget.setY(origY + this.getY() - offsetY);
            widget.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            widget.setX(origX);
            widget.setY(origY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (isMouseInside(event.x(), event.y())) {
            double cx = event.x() - this.getX();
            double cy = event.y() + getOffset() - this.getY();
            for (AbstractWidget widget : new ArrayList<>(children)) {
                if (widget.mouseClicked(new MouseButtonEvent(cx, cy, event.buttonInfo()), isDoubleClick)) {
                    setChildFocus(widget);
                    this.setFocused(true);
                    return true;
                }
            }
            setChildFocus(null);
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    private void setChildFocus(AbstractWidget child) {
        if (focusedChild != null && focusedChild != child) {
            focusedChild.setFocused(false);
        }
        focusedChild = child;
        if (focusedChild != null) {
            focusedChild.setFocused(true);
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        double cx = event.x() - this.getX();
        double cy = event.y() + getOffset() - this.getY();
        for (AbstractWidget widget : new ArrayList<>(children)) {
            if (widget.mouseDragged(new MouseButtonEvent(cx, cy, event.buttonInfo()), deltaX, deltaY)) return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
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
    public boolean preeditUpdated(@Nullable PreeditEvent event) {
        if (focusedChild != null && focusedChild.preeditUpdated(event)) return true;
        return super.preeditUpdated(event);
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
