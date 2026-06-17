package cn.zbx1425.mtrsteamloco.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

public class WidgetManualPositionBar extends AbstractWidget {

    private float railLength = 100f;
    private final List<Float> positions = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean isDragging = false;
    private final Consumer<List<Float>> onChange;
    private Runnable onSelectionChange;

    private boolean editable = true;
    private Set<Integer> overrideIndices = Collections.emptySet();
    private float playerProgress = -1;

    private float viewCenter = 50f;
    private int zoomIndex = 0;
    private static final float[] ZOOM_LEVELS = {1, 2, 4, 8, 16};

    private static final int HANDLE_HALF_W = 3;
    private static final int OVERVIEW_H = 12;
    private static final int OV_TICK_AREA = 12;
    private static final int GAP = 4;
    private static final int DETAIL_H = 22;
    private static final int DT_TICK_AREA = 12;
    private static final int MARGIN = 10;
    private static final int TOTAL_H = OVERVIEW_H + OV_TICK_AREA + GAP + DETAIL_H + DT_TICK_AREA;
    private static final int TICK_LEN = 4;
    private static final float QUANTIZE_STEP = 0.001f;

    private boolean isDraggingOverview = false;

    public WidgetManualPositionBar(int x, int y, int width, Consumer<List<Float>> onChange) {
        super(x, y, width, TOTAL_H, Component.empty());
        this.onChange = onChange;
    }

    public void setOnSelectionChange(Runnable callback) {
        this.onSelectionChange = callback;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setOverrideIndices(Set<Integer> indices) {
        this.overrideIndices = indices != null ? indices : Collections.emptySet();
    }

    public void setPlayerProgress(float progress) {
        this.playerProgress = progress;
    }

    public void setRailLength(float railLength) {
        this.railLength = Math.max(0.1f, railLength);
        this.viewCenter = this.railLength / 2;
        clampViewport();
    }

    public void setPositions(List<Float> newPositions) {
        this.positions.clear();
        this.positions.addAll(newPositions);
        Collections.sort(this.positions);
        if (selectedIndex >= positions.size()) {
            selectedIndex = positions.isEmpty() ? -1 : positions.size() - 1;
        }
    }

    public List<Float> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = (index >= 0 && index < positions.size()) ? index : -1;
    }

    public float getSelectedPosition() {
        return (selectedIndex >= 0 && selectedIndex < positions.size()) ? positions.get(selectedIndex) : -1;
    }

    public void setPositionAt(int index, float value) {
        if (index >= 0 && index < positions.size()) {
            positions.set(index, quantize(Math.max(0, Math.min(railLength, value))));
            notifyChange();
        }
    }

    public void setZoomIndex(int idx) {
        this.zoomIndex = Math.max(0, Math.min(ZOOM_LEVELS.length - 1, idx));
        clampViewport();
    }

    public int getZoomIndex() {
        return zoomIndex;
    }

    public float getViewCenter() {
        return viewCenter;
    }

    public void setViewCenter(float center) {
        this.viewCenter = center;
        clampViewport();
    }

    private float getZoom() {
        return ZOOM_LEVELS[zoomIndex];
    }

    private static float quantize(float value) {
        return Math.round(value / QUANTIZE_STEP) * QUANTIZE_STEP;
    }

    private void clampViewport() {
        float halfView = railLength / (2 * getZoom());
        if (halfView >= railLength / 2) {
            viewCenter = railLength / 2;
        } else {
            viewCenter = Math.max(halfView, Math.min(railLength - halfView, viewCenter));
        }
    }

    // --- Overview geometry (full range) ---
    private int ovLeft() { return getX() + MARGIN; }
    private int ovRight() { return getX() + width - MARGIN; }
    private int ovWidth() { return ovRight() - ovLeft(); }
    private int ovTop() { return getY(); }

    private int ovPosToPixel(float pos) {
        return ovLeft() + Math.round((pos / railLength) * ovWidth());
    }

    private float ovPixelToPos(double px) {
        return Math.max(0, Math.min(railLength, (float) ((px - ovLeft()) / ovWidth()) * railLength));
    }

    // --- Detail geometry (viewport range) ---
    private int dtTop() { return getY() + OVERVIEW_H + OV_TICK_AREA + GAP; }
    private int dtLeft() { return getX() + MARGIN; }
    private int dtRight() { return getX() + width - MARGIN; }
    private int dtWidth() { return dtRight() - dtLeft(); }

    private float viewStart() { return Math.max(0, viewCenter - railLength / (2 * getZoom())); }
    private float viewEnd() { return Math.min(railLength, viewCenter + railLength / (2 * getZoom())); }

    private int dtPosToPixel(float pos) {
        float vs = viewStart(), ve = viewEnd();
        if (ve <= vs) return dtLeft();
        return dtLeft() + Math.round(((pos - vs) / (ve - vs)) * dtWidth());
    }

    private float dtPixelToPos(double px) {
        float vs = viewStart(), ve = viewEnd();
        float pos = vs + (float) ((px - dtLeft()) / dtWidth()) * (ve - vs);
        return Math.max(0, Math.min(railLength, pos));
    }

    // --- Tick computation ---
    private static float niceStep(float range, int maxTicks) {
        if (range <= 0 || maxTicks <= 0) return 1;
        float roughStep = range / maxTicks;
        float magnitude = (float) Math.pow(10, Math.floor(Math.log10(roughStep)));
        float residual = roughStep / magnitude;
        float nice;
        if (residual <= 1.5f) nice = 1;
        else if (residual <= 3.5f) nice = 2;
        else if (residual <= 7.5f) nice = 5;
        else nice = 10;
        return nice * magnitude;
    }

    private static String formatTickLabel(float value, float step) {
        if (step >= 1) return String.format("%.0f", value);
        if (step >= 0.1f) return String.format("%.1f", value);
        if (step >= 0.01f) return String.format("%.2f", value);
        return String.format("%.3f", value);
    }

    // --- Rendering ---
    @Override
    public void renderWidget(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        Font font = Minecraft.getInstance().font;

        renderOverview(g, font);
        renderDetail(g, font);
    }

    private void renderOverview(GuiGraphicsExtractor g, Font font) {
        int ot = ovTop();
        int ocy = ot + OVERVIEW_H / 2;
        int ol = ovLeft(), or_ = ovRight();

        dfill(g, ol, ocy - 1, or_, ocy + 1, 0xFF505050);
        dfill(g, ol, ocy - 3, ol + 1, ocy + 3, 0xFFAAAAAA);
        dfill(g, or_ - 1, ocy - 3, or_, ocy + 3, 0xFFAAAAAA);

        int vpL = ovPosToPixel(viewStart());
        int vpR = ovPosToPixel(viewEnd());
        dfill(g, vpL, ot, vpR, ot + OVERVIEW_H, 0x30FFFFFF);
        dfill(g, vpL, ot, vpL + 1, ot + OVERVIEW_H, 0xFF4488FF);
        dfill(g, vpR - 1, ot, vpR, ot + OVERVIEW_H, 0xFF4488FF);

        for (int i = 0; i < positions.size(); i++) {
            int px = ovPosToPixel(positions.get(i));
            int color;
            if (i == selectedIndex) color = 0xFFFFFF00;
            else if (overrideIndices.contains(i)) color = 0xFFFF8800;
            else color = 0xFF00CC00;
            dfill(g, px, ot + 1, px + 1, ot + OVERVIEW_H - 1, color);
        }

        if (playerProgress >= 0 && playerProgress <= railLength) {
            int ppx = ovPosToPixel(playerProgress);
            dfill(g, ppx, ot, ppx + 1, ot + OVERVIEW_H, 0xFFFF4444);
        }

        int tickY = ot + OVERVIEW_H;
        drawTicks(g, font, ol, or_, 0, railLength, tickY);
    }

    private void renderDetail(GuiGraphicsExtractor g, Font font) {
        int dt = dtTop();
        int dcy = dt + DETAIL_H / 2;
        int dl = dtLeft(), dr = dtRight();
        float vs = viewStart(), ve = viewEnd();

        dfill(g, dl, dt, dr, dt + DETAIL_H, 0xFF1A1A1A);
        dfill(g, dl, dcy - 1, dr, dcy + 1, 0xFF606060);

        int hh = DETAIL_H / 2 - 2;
        for (int i = 0; i < positions.size(); i++) {
            float p = positions.get(i);
            if (p < vs - 1 || p > ve + 1) continue;
            int px = dtPosToPixel(p);
            if (px < dl - HANDLE_HALF_W || px > dr + HANDLE_HALF_W) continue;
            int color;
            if (i == selectedIndex) color = 0xFFFFFF00;
            else if (overrideIndices.contains(i)) color = 0xFFFF8800;
            else color = 0xFF00FF00;
            dfill(g, px - HANDLE_HALF_W, dcy - hh, px + HANDLE_HALF_W, dcy + hh, color);
        }

        if (playerProgress >= 0 && playerProgress <= railLength
                && playerProgress >= vs - 1 && playerProgress <= ve + 1) {
            int ppx = dtPosToPixel(playerProgress);
            if (ppx >= dl && ppx <= dr) {
                dfill(g, ppx, dt, ppx + 1, dt + DETAIL_H, 0xAAFF4444);
                dfill(g, ppx - 2, dt, ppx + 3, dt + 2, 0xFFFF4444);
            }
        }

        drawTicks(g, font, dl, dr, vs, ve, dt + DETAIL_H);
    }

    private void drawTicks(GuiGraphicsExtractor g, Font font, int pxL, int pxR, float posStart, float posEnd, int y) {
        float range = posEnd - posStart;
        if (range <= 0) return;
        int maxTicks = Math.max(2, (pxR - pxL) / 50);
        float step = niceStep(range, maxTicks);
        float first = (float) (Math.ceil(posStart / step) * step);
        for (float t = first; t <= posEnd + step * 0.001f; t += step) {
            int px = pxL + Math.round(((t - posStart) / range) * (pxR - pxL));
            if (px < pxL - 1 || px > pxR + 1) continue;
            dfill(g, px, y, px + 1, y + TICK_LEN, 0xFF606060);
            String label = formatTickLabel(t, step);
            g.drawString(font, label, px - font.width(label) / 2, y + TICK_LEN + 1, 0xFF888888);
        }
    }

    private static void dfill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color);
    }

    // --- Mouse interaction ---
    private boolean isInOverview(double mx, double my) {
        return mx >= ovLeft() - 2 && mx <= ovRight() + 2
                && my >= ovTop() && my <= ovTop() + OVERVIEW_H;
    }

    private boolean isInDetail(double mx, double my) {
        int dt = dtTop();
        return mx >= dtLeft() - HANDLE_HALF_W && mx <= dtRight() + HANDLE_HALF_W
                && my >= dt - 2 && my <= dt + DETAIL_H + 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;

        if (isInOverview(mouseX, mouseY) && button == 0) {
            viewCenter = ovPixelToPos(mouseX);
            clampViewport();
            isDraggingOverview = true;
            return true;
        }

        if (isInDetail(mouseX, mouseY)) {
            if (button == 0) {
                int closestIdx = findClosestDetailHandle(mouseX);
                if (closestIdx >= 0) {
                    selectedIndex = closestIdx;
                    if (editable) isDragging = true;
                } else if (editable) {
                    float newPos = quantize(dtPixelToPos(mouseX));
                    positions.add(newPos);
                    Collections.sort(positions);
                    selectedIndex = positions.indexOf(newPos);
                    isDragging = true;
                    notifyChange();
                }
                notifySelectionChange();
                return true;
            } else if (button == 1 && editable) {
                int closestIdx = findClosestDetailHandle(mouseX);
                if (closestIdx >= 0) {
                    positions.remove(closestIdx);
                    if (selectedIndex >= positions.size()) {
                        selectedIndex = positions.isEmpty() ? -1 : positions.size() - 1;
                    }
                    isDragging = false;
                    notifyChange();
                    notifySelectionChange();
                    return true;
                }
            }
        }

        return false;
    }

    private int findClosestDetailHandle(double mouseX) {
        int closestIdx = -1;
        double closestDist = Double.MAX_VALUE;
        float vs = viewStart(), ve = viewEnd();
        for (int i = 0; i < positions.size(); i++) {
            float p = positions.get(i);
            if (p < vs - 1 || p > ve + 1) continue;
            double dist = Math.abs(dtPosToPixel(p) - mouseX);
            if (dist < closestDist && dist <= HANDLE_HALF_W + 4) {
                closestDist = dist;
                closestIdx = i;
            }
        }
        return closestIdx;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingOverview && button == 0) {
            viewCenter = ovPixelToPos(mouseX);
            clampViewport();
            return true;
        }
        if (editable && isDragging && selectedIndex >= 0 && selectedIndex < positions.size() && button == 0) {
            float newPos = quantize(dtPixelToPos(mouseX));
            positions.set(selectedIndex, newPos);
            notifyChange();
            notifySelectionChange();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingOverview && button == 0) {
            isDraggingOverview = false;
            return true;
        }
        if (isDragging && button == 0) {
            isDragging = false;
            float draggedValue = (selectedIndex >= 0 && selectedIndex < positions.size())
                    ? positions.get(selectedIndex) : -1;
            Collections.sort(positions);
            selectedIndex = draggedValue >= 0 ? positions.indexOf(draggedValue) : -1;
            notifyChange();
            notifySelectionChange();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!visible || !active) return false;
        if (mouseX < getX() || mouseX > getX() + width || mouseY < getY() || mouseY > getY() + TOTAL_H) {
            return false;
        }

        float cursorPos = dtPixelToPos(mouseX);
        float pixelFrac = dtWidth() > 0 ? (float) ((mouseX - dtLeft()) / dtWidth()) : 0.5f;

        if (scrollY > 0) {
            zoomIndex = Math.min(ZOOM_LEVELS.length - 1, zoomIndex + 1);
        } else if (scrollY < 0) {
            zoomIndex = Math.max(0, zoomIndex - 1);
        } else {
            return false;
        }

        float newHalfView = railLength / (2 * getZoom());
        viewCenter = cursorPos - (2 * pixelFrac - 1) * newHalfView;
        clampViewport();
        return true;
    }

    private void notifyChange() {
        onChange.accept(Collections.unmodifiableList(positions));
    }

    private void notifySelectionChange() {
        if (onSelectionChange != null) onSelectionChange.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
}
