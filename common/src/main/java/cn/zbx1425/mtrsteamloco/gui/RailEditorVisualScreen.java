package cn.zbx1425.mtrsteamloco.gui;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.*;
import cn.zbx1425.mtrsteamloco.network.PacketUpdateHoldingItem;
import cn.zbx1425.mtrsteamloco.network.PacketUpdateRail;
import cn.zbx1425.mtrsteamloco.render.RailPicker;
import cn.zbx1425.mtrsteamloco.render.rail.RailRenderDispatcher;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import mtr.RegistryClient;
import mtr.client.IDrawing;
import mtr.client.ClientData;
import mtr.data.Rail;
import mtr.mappings.Text;
import mtr.util.UtilitiesClient;
import mtr.packet.IPacket;
import mtr.screen.WidgetBetterTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class RailEditorVisualScreen extends SelectListScreen {

    private enum ModelSelectTarget { NONE, ATTACHMENT_BASE, ATTACHMENT_INSTANCE }
    private ModelSelectTarget modelSelectTarget = ModelSelectTarget.NONE;

    private static Rail pickedRail = null;
    private static BlockPos pickedPosStart = BlockPos.ZERO;
    private static BlockPos pickedPosEnd = BlockPos.ZERO;

    private static String lastEditedRepeaterId = "";

    private static BlockPos lastTerminalNode = null;
    private static float lastExitOffset = 0;
    private static String lastPropagateRepeaterId = "";
    private static float lastPropagateInterval = 0;
    private static int[] lastExitFMI = new int[0];

    private int selectedLayerIndex = 0;
    private int selectedBaseAttIndex = 0;
    private int selectedOvAttIndex = 0;

    private final WidgetScrollList layerScrollList = new WidgetScrollList(0, 0, 100, 100);
    private final WidgetScrollPanel rightScrollPanel = new WidgetScrollPanel(0, 0, 100, 100);

    private int savedBarSelectedIndex = -1;
    private int savedBarZoomIndex = 0;
    private float savedBarViewCenter = -1;

    public RailEditorVisualScreen() {
        super(Text.translatable("gui.mtr.rail_editor_visual.title"));
        if (pickedRail == null) acquirePickInfoWhenUse();
        autoSelectLayer();
    }

    @Override
    protected void init() {
        super.init();
        loadPage();
    }

    @Override
    protected void loadPage() {
        clearWidgets();

        if (modelSelectTarget != ModelSelectTarget.NONE) {
            String currentModelKey = "";
            RailModelRepeater sel = getSelectedRepeater();
            if (sel != null) {
                RepeaterAttachment att = getTargetAttachment(sel);
                if (att != null) currentModelKey = att.modelTypeKey;
            }
            String finalKey = currentModelKey;
            scrollList.visible = true;
            loadSelectPage(key -> !key.equals(finalKey));
            return;
        }

        scrollList.visible = false;
        loadEditorPage();
    }

    private RepeaterAttachment getTargetAttachment(RailModelRepeater repeater) {
        if (modelSelectTarget == ModelSelectTarget.ATTACHMENT_BASE) {
            if (selectedBaseAttIndex >= 0 && selectedBaseAttIndex < repeater.attachments.size()) {
                return repeater.attachments.get(selectedBaseAttIndex);
            }
        } else if (modelSelectTarget == ModelSelectTarget.ATTACHMENT_INSTANCE && currentBar != null) {
            int posIdx = currentBar.getSelectedIndex();
            RailModelInstanceOverride ov = repeater.instanceOverrides.get(
                    displayIndexToCanonIndex(posIdx, getPositionCount(repeater)));
            if (ov != null && ov.attachments != null
                    && selectedOvAttIndex >= 0 && selectedOvAttIndex < ov.attachments.size()) {
                return ov.attachments.get(selectedOvAttIndex);
            }
        }
        return null;
    }

    private List<RailModelRepeater> getRepeaters() {
        if (pickedRail == null) return new ArrayList<>();
        return ((RailExtraSupplier) pickedRail).getRepeaters();
    }

    private void autoSelectLayer() {
        List<RailModelRepeater> repeaters = getRepeaters();
        if (repeaters.isEmpty()) return;
        if (lastEditedRepeaterId.isEmpty()) {
            selectedLayerIndex = repeaters.size() - 1;
            return;
        }
        int lastMatch = -1;
        for (int i = 0; i < repeaters.size(); i++) {
            if (repeaters.get(i).getId().equals(lastEditedRepeaterId)) {
                lastMatch = i;
            }
        }
        selectedLayerIndex = lastMatch >= 0 ? lastMatch : repeaters.size() - 1;
    }

    private boolean userIsAtCanonStart() {
        return pickedPosStart.asLong() <= pickedPosEnd.asLong();
    }

    private RailModelRepeater getSelectedRepeater() {
        List<RailModelRepeater> repeaters = getRepeaters();
        if (selectedLayerIndex >= 0 && selectedLayerIndex < repeaters.size()) {
            return repeaters.get(selectedLayerIndex);
        }
        return null;
    }

    private void loadEditorPage() {
        if (currentBar != null) {
            savedBarSelectedIndex = currentBar.getSelectedIndex();
            savedBarZoomIndex = currentBar.getZoomIndex();
            savedBarViewCenter = currentBar.getViewCenter();
        }

        List<RailModelRepeater> repeaters = getRepeaters();

        int leftPanelWidth = Mth.clamp(width / 3, 140, 220);
        int rightPanelWidth = Math.min(width - (leftPanelWidth + SQUARE_SIZE) - SQUARE_SIZE, 380);
        int rightPanelX = leftPanelWidth + (width - leftPanelWidth - rightPanelWidth) / 2;

        // -- Left panel: layer list --
        layerScrollList.children.clear();
        IDrawing.setPositionAndWidth(layerScrollList, 0, SQUARE_SIZE, leftPanelWidth);
        layerScrollList.setHeight(height - SQUARE_SIZE * 3);

        for (int i = 0; i < repeaters.size(); i++) {
            RailModelRepeater p = repeaters.get(i);
            String displayId = p.getId();
            RailModelProperties props = RailModelRegistry.elements.get(displayId);
            String modelLabel = (props != null && !props.name.getString().isEmpty())
                    ? props.name.getString() : (displayId.isEmpty() ? "---" : displayId);
            String modeLabel = switch (p.repeaterMode) {
                case STRETCH_INTERVAL -> "S";
                case FIXED_INTERVAL -> "F";
                case MANUAL -> "M";
            };
            String btnText = String.format("%s [%s]", modelLabel, modeLabel);

            final int layerIdx = i;
            int btnWidth = leftPanelWidth - SQUARE_SIZE;

            Button layerBtn = UtilitiesClient.newButton(
                    Text.literal(btnText),
                    sender -> {
                        selectedLayerIndex = layerIdx;
                        selectedBaseAttIndex = 0;
                        selectedOvAttIndex = 0;
                        Minecraft.getInstance().execute(this::loadPage);
                    }
            );
            layerBtn.active = (i != selectedLayerIndex);
            IDrawing.setPositionAndWidth(layerBtn, 0, i * SQUARE_SIZE, btnWidth);
            layerScrollList.children.add(layerBtn);

            Button deleteBtn = UtilitiesClient.newButton(
                    Text.literal("x"),
                    sender -> {
                        getRepeaters().remove(layerIdx);
                        if (selectedLayerIndex >= getRepeaters().size()) {
                            selectedLayerIndex = Math.max(0, getRepeaters().size() - 1);
                        }
                        sendUpdate();
                        Minecraft.getInstance().execute(this::loadPage);
                    }
            );
            IDrawing.setPositionAndWidth(deleteBtn, btnWidth, i * SQUARE_SIZE, SQUARE_SIZE);
            layerScrollList.children.add(deleteBtn);
        }

        addRenderableWidget(layerScrollList);

        IDrawing.setPositionAndWidth(addRenderableWidget(UtilitiesClient.newButton(
                Text.translatable("gui.mtr.rail_editor_visual.add_layer"),
                sender -> {
                    getRepeaters().add(new RailModelRepeater());
                    selectedLayerIndex = getRepeaters().size() - 1;
                    selectedBaseAttIndex = 0;
                    selectedOvAttIndex = 0;
                    sendUpdate();
                    Minecraft.getInstance().execute(this::loadPage);
                }
        )), 0, height - SQUARE_SIZE * 2, leftPanelWidth);

        // -- Right panel --
        RailModelRepeater selected = getSelectedRepeater();
        if (selected == null) {
            addRenderableWidget(new WidgetLabel(rightPanelX, SQUARE_SIZE * 2, rightPanelWidth,
                    Text.translatable("gui.mtr.rail_editor_visual.no_layers")));
            return;
        }

        rightScrollPanel.children.clear();
        IDrawing.setPositionAndWidth(rightScrollPanel, rightPanelX, SQUARE_SIZE, rightPanelWidth);
        rightScrollPanel.setHeight(height - SQUARE_SIZE * 2);

        int w = rightPanelWidth - 10;
        int halfW = w / 2 - 2;
        int y = 0;

        // -- Interval override --
        rightScrollPanel.children.add(new WidgetLabel(0, y + 6, halfW,
                Text.translatable("gui.mtr.rail_editor_visual.interval")));

        WidgetBetterTextField intervalField = new WidgetBetterTextField("0", 8);
        IDrawing.setPositionAndWidth(intervalField, halfW + 4, y, halfW);
        intervalField.setValue(selected.intervalOverride > 0 ? String.format("%.2f", selected.intervalOverride) : "");
        intervalField.setResponder(text -> {
            try {
                selected.intervalOverride = text.isEmpty() ? 0 : Float.parseFloat(text);
                intervalField.setTextColor(0xE0E0E0);
                sendUpdate();
            } catch (NumberFormatException e) {
                intervalField.setTextColor(0xFF0000);
            }
        });
        intervalField.active = selected.repeaterMode != RepeaterMode.MANUAL;
        rightScrollPanel.children.add(intervalField);
        y += SQUARE_SIZE + 4;

        // -- Mode selector --
        int modeButtonWidth = w / 3;
        Button btnStretch = UtilitiesClient.newButton(
                Text.translatable("gui.mtr.rail_editor_visual.stretch_interval"),
                sender -> { selected.repeaterMode = RepeaterMode.STRETCH_INTERVAL; sendUpdate(); Minecraft.getInstance().execute(this::loadPage); }
        );
        btnStretch.active = selected.repeaterMode != RepeaterMode.STRETCH_INTERVAL;
        IDrawing.setPositionAndWidth(btnStretch, 0, y, modeButtonWidth);
        rightScrollPanel.children.add(btnStretch);

        Button btnFixed = UtilitiesClient.newButton(
                Text.translatable("gui.mtr.rail_editor_visual.fixed_interval"),
                sender -> {
                    if (selected.repeaterMode != RepeaterMode.FIXED_INTERVAL) {
                        selected.offsetFromStart = userIsAtCanonStart();
                    }
                    selected.repeaterMode = RepeaterMode.FIXED_INTERVAL;
                    sendUpdate();
                    Minecraft.getInstance().execute(this::loadPage);
                }
        );
        btnFixed.active = selected.repeaterMode != RepeaterMode.FIXED_INTERVAL;
        IDrawing.setPositionAndWidth(btnFixed, modeButtonWidth, y, modeButtonWidth);
        rightScrollPanel.children.add(btnFixed);

        Button btnManual = UtilitiesClient.newButton(
                Text.translatable("gui.mtr.rail_editor_visual.manual"),
                sender -> {
                    if (selected.repeaterMode != RepeaterMode.MANUAL) {
                        selected.manualPositions = computePositionsForCurrentMode(selected);
                    }
                    selected.repeaterMode = RepeaterMode.MANUAL;
                    sendUpdate();
                    Minecraft.getInstance().execute(this::loadPage);
                }
        );
        btnManual.active = selected.repeaterMode != RepeaterMode.MANUAL;
        IDrawing.setPositionAndWidth(btnManual, modeButtonWidth * 2, y, w - modeButtonWidth * 2);
        rightScrollPanel.children.add(btnManual);
        y += SQUARE_SIZE + 4;

        // -- Mode-specific settings --
        if (selected.repeaterMode == RepeaterMode.FIXED_INTERVAL) {
            y = loadFixedPanel(selected, y, w);
        }
        y += 20;

        // -- Attachments sub-list (vertical) --
        y = loadAttachmentPanel(selected, y, w);
        y += 20;

        // -- Position bar (all modes) --
        y = loadPositionBar(selected, y, w);
//        y += 20;

        // -- Instance override panel --
        loadInstanceOverridePanel(selected, y, w);

        addRenderableWidget(rightScrollPanel);
    }

    private int loadFixedPanel(RailModelRepeater repeater, int y, int w) {
        boolean fromThisNode = repeater.offsetFromStart == userIsAtCanonStart();
        int halfW = w / 2 - 2;

        rightScrollPanel.children.add(new WidgetLabel(0, y + 6, halfW,
                Text.translatable("gui.mtr.rail_editor_visual.offset_direction")));

        Button btnFromThis = UtilitiesClient.newButton(
                Text.translatable("gui.mtr.rail_editor_visual.from_this_node"),
                sender -> { repeater.offsetFromStart = userIsAtCanonStart(); sendUpdate(); Minecraft.getInstance().execute(this::loadPage); }
        );
        btnFromThis.active = !fromThisNode;
        IDrawing.setPositionAndWidth(btnFromThis, halfW + 4, y, (w - halfW - 4) / 2 - 1);
        rightScrollPanel.children.add(btnFromThis);

        Button btnFromOther = UtilitiesClient.newButton(
                Text.translatable("gui.mtr.rail_editor_visual.from_other_node"),
                sender -> { repeater.offsetFromStart = !userIsAtCanonStart(); sendUpdate(); Minecraft.getInstance().execute(this::loadPage); }
        );
        btnFromOther.active = fromThisNode;
        IDrawing.setPositionAndWidth(btnFromOther, halfW + 4 + (w - halfW - 4) / 2 + 1, y, (w - halfW - 4) / 2 - 1);
        rightScrollPanel.children.add(btnFromOther);
        y += SQUARE_SIZE + 2;

        rightScrollPanel.children.add(new WidgetLabel(0, y + 6, halfW,
                Text.translatable("gui.mtr.rail_editor_visual.offset")));

        WidgetBetterTextField offsetField = new WidgetBetterTextField("0", 8);
        IDrawing.setPositionAndWidth(offsetField, halfW + 4, y, halfW);
        offsetField.setValue(String.format("%.3f", repeater.offset));
        offsetField.setResponder(text -> {
            try {
                repeater.offset = text.isEmpty() ? 0 : Float.parseFloat(text);
                offsetField.setTextColor(0xE0E0E0);
                sendUpdate();
            } catch (NumberFormatException e) {
                offsetField.setTextColor(0xFF0000);
            }
        });
        rightScrollPanel.children.add(offsetField);
        y += SQUARE_SIZE + 2;

        int btnW = w / 3 - 2;
        Button btnPropagate = UtilitiesClient.newButton(
                Text.translatable("gui.mtr.rail_editor_visual.propagate"),
                sender -> sendPropagate(repeater, false)
        );
        IDrawing.setPositionAndWidth(btnPropagate, 0, y, btnW);
        rightScrollPanel.children.add(btnPropagate);

        Button btnUndo = UtilitiesClient.newButton(
                Text.translatable("gui.mtr.rail_editor_visual.undo_propagate"),
                sender -> sendPropagate(repeater, true)
        );
        IDrawing.setPositionAndWidth(btnUndo, btnW + 2, y, btnW);
        rightScrollPanel.children.add(btnUndo);

        boolean canContinue = lastTerminalNode != null
                && lastTerminalNode.equals(pickedPosStart)
                && repeater.getId().equals(lastPropagateRepeaterId);
        Button btnContinue = UtilitiesClient.newButton(
                Text.translatable("gui.mtr.rail_editor_visual.continue_propagate"),
                sender -> {
                    repeater.offset = lastExitOffset;
                    repeater.offsetFromStart = userIsAtCanonStart();
                    repeater.intervalOverride = lastPropagateInterval;
                    applyExitFMI(repeater, lastExitFMI);
                    sendUpdate();
                    Minecraft.getInstance().execute(() -> sendPropagate(repeater, false));
                }
        );
        btnContinue.active = canContinue;
        IDrawing.setPositionAndWidth(btnContinue, btnW * 2 + 4, y, w - btnW * 2 - 4);
        rightScrollPanel.children.add(btnContinue);
        y += SQUARE_SIZE + 4;

        return y;
    }

    private void applyExitFMI(RailModelRepeater repeater, int[] exitFMI) {
        for (int i = 0; i < Math.min(exitFMI.length, repeater.attachments.size()); i++) {
            repeater.attachments.get(i).firstModelIndex = exitFMI[i];
        }
    }

    // ==================== Attachment List Editor (reusable) ====================

    private int getSelectedAttIndex(ModelSelectTarget target) {
        return target == ModelSelectTarget.ATTACHMENT_BASE ? selectedBaseAttIndex : selectedOvAttIndex;
    }

    private void setSelectedAttIndex(ModelSelectTarget target, int idx) {
        if (target == ModelSelectTarget.ATTACHMENT_BASE) selectedBaseAttIndex = idx;
        else selectedOvAttIndex = idx;
    }

    private int loadAttachmentListEditor(List<RepeaterAttachment> attList, int y, int w,
                                         int leftMargin, ModelSelectTarget selectTarget) {
        int innerW = w - leftMargin;
        int selIdx = getSelectedAttIndex(selectTarget);

        int attBtnWidth = innerW - SQUARE_SIZE * 2;

        for (int i = 0; i < attList.size(); i++) {
            RepeaterAttachment att = attList.get(i);
            String attLabel = getAttachmentLabel(att);
            final int attIdx = i;

            Button attBtn = UtilitiesClient.newButton(
                    Text.literal(attLabel),
                    sender -> {
                        setSelectedAttIndex(selectTarget, attIdx);
                        Minecraft.getInstance().execute(this::loadPage);
                    }
            );
            attBtn.active = (i != selIdx);
            IDrawing.setPositionAndWidth(attBtn, leftMargin, y, attBtnWidth);
            rightScrollPanel.children.add(attBtn);

            Button delAttBtn = UtilitiesClient.newButton(
                    Text.literal("x"),
                    sender -> {
                        if (attList.size() > 1) {
                            attList.remove(attIdx);
                            int cur = getSelectedAttIndex(selectTarget);
                            if (cur >= attList.size()) {
                                setSelectedAttIndex(selectTarget, attList.size() - 1);
                            }
                            sendUpdate();
                            Minecraft.getInstance().execute(this::loadPage);
                        }
                    }
            );
            delAttBtn.active = attList.size() > 1;
            IDrawing.setPositionAndWidth(delAttBtn, leftMargin + attBtnWidth, y, SQUARE_SIZE);
            rightScrollPanel.children.add(delAttBtn);

            y += SQUARE_SIZE;
        }

        y -= SQUARE_SIZE;
        Button addAttBtn = UtilitiesClient.newButton(
                Text.literal("+"),
                sender -> {
                    attList.add(new RepeaterAttachment());
                    setSelectedAttIndex(selectTarget, attList.size() - 1);
                    sendUpdate();
                    Minecraft.getInstance().execute(this::loadPage);
                }
        );
        IDrawing.setPositionAndWidth(addAttBtn, leftMargin + innerW - SQUARE_SIZE, y, SQUARE_SIZE);
        rightScrollPanel.children.add(addAttBtn);
        y += SQUARE_SIZE + 4;

        if (selIdx >= 0 && selIdx < attList.size()) {
            y = loadAttachmentDetail(attList.get(selIdx), y, w, leftMargin + 20, selectTarget);
        }

        return y;
    }

    private int loadAttachmentPanel(RailModelRepeater repeater, int y, int w) {
        rightScrollPanel.children.add(new WidgetLabel(0, y + 6, w,
                Text.translatable("gui.mtr.rail_editor_visual.attachments")));
        y += SQUARE_SIZE - 2;

        y = loadAttachmentListEditor(repeater.attachments, y, w, 0, ModelSelectTarget.ATTACHMENT_BASE);
        return y;
    }

    private int loadAttachmentDetail(RepeaterAttachment att, int y, int w,
                                     int leftMargin, ModelSelectTarget selectTarget) {
        int innerW = w - leftMargin;
        int halfW = innerW / 2 - 2;

        // -- Model Type selector --
        String modelLabel = getAttachmentLabel(att);
        Button modelBtn = UtilitiesClient.newButton(
                Text.literal(modelLabel),
                sender -> {
                    modelSelectTarget = selectTarget;
                    Minecraft.getInstance().execute(this::loadPage);
                }
        );
        IDrawing.setPositionAndWidth(modelBtn, leftMargin, y, innerW);
        rightScrollPanel.children.add(modelBtn);
        y += SQUARE_SIZE + 2;

        // -- Direction --
        rightScrollPanel.children.add(new WidgetLabel(leftMargin, y + 6, halfW,
                Text.translatable("gui.mtr.rail_editor_visual.direction")));

        Button btnNormal = UtilitiesClient.newButton(
                Text.translatable("gui.mtr.rail_editor_visual.facing_away"),
                sender -> { att.reversed = false; sendUpdate(); Minecraft.getInstance().execute(this::loadPage); }
        );
        btnNormal.active = att.reversed;
        IDrawing.setPositionAndWidth(btnNormal, leftMargin + halfW + 4, y, (innerW - halfW - 4) / 2 - 1);
        rightScrollPanel.children.add(btnNormal);

        Button btnReversed = UtilitiesClient.newButton(
                Text.translatable("gui.mtr.rail_editor_visual.facing_here"),
                sender -> { att.reversed = true; sendUpdate(); Minecraft.getInstance().execute(this::loadPage); }
        );
        btnReversed.active = !att.reversed;
        IDrawing.setPositionAndWidth(btnReversed, leftMargin + halfW + 4 + (innerW - halfW - 4) / 2 + 1, y, (innerW - halfW - 4) / 2 - 1);
        rightScrollPanel.children.add(btnReversed);
        y += SQUARE_SIZE + 2;

        // -- XYZ offset --
        int thirdW = innerW / 3 - 2;
        rightScrollPanel.children.add(new WidgetLabel(leftMargin, y + 6, thirdW,
                Text.translatable("gui.mtr.rail_editor_visual.offset_x")));
        rightScrollPanel.children.add(new WidgetLabel(leftMargin + thirdW + 2, y + 6, thirdW,
                Text.translatable("gui.mtr.rail_editor_visual.offset_y")));
        rightScrollPanel.children.add(new WidgetLabel(leftMargin + thirdW * 2 + 4, y + 6, thirdW,
                Text.translatable("gui.mtr.rail_editor_visual.offset_z")));
        y += SQUARE_SIZE - 4;

        WidgetBetterTextField fieldX = new WidgetBetterTextField("0", 8);
        IDrawing.setPositionAndWidth(fieldX, leftMargin, y, thirdW);
        fieldX.setValue(String.format("%.3f", att.offsetX));
        fieldX.setResponder(text -> {
            try { att.offsetX = text.isEmpty() ? 0 : Float.parseFloat(text); fieldX.setTextColor(0xE0E0E0); sendUpdate(); }
            catch (NumberFormatException e) { fieldX.setTextColor(0xFF0000); }
        });
        rightScrollPanel.children.add(fieldX);

        WidgetBetterTextField fieldY = new WidgetBetterTextField("0", 8);
        IDrawing.setPositionAndWidth(fieldY, leftMargin + thirdW + 2, y, thirdW);
        fieldY.setValue(String.format("%.3f", att.offsetY));
        fieldY.setResponder(text -> {
            try { att.offsetY = text.isEmpty() ? 0 : Float.parseFloat(text); fieldY.setTextColor(0xE0E0E0); sendUpdate(); }
            catch (NumberFormatException e) { fieldY.setTextColor(0xFF0000); }
        });
        rightScrollPanel.children.add(fieldY);

        WidgetBetterTextField fieldZ = new WidgetBetterTextField("0", 8);
        IDrawing.setPositionAndWidth(fieldZ, leftMargin + thirdW * 2 + 4, y, thirdW);
        fieldZ.setValue(String.format("%.3f", att.offsetZ));
        fieldZ.setResponder(text -> {
            try { att.offsetZ = text.isEmpty() ? 0 : Float.parseFloat(text); fieldZ.setTextColor(0xE0E0E0); sendUpdate(); }
            catch (NumberFormatException e) { fieldZ.setTextColor(0xFF0000); }
        });
        rightScrollPanel.children.add(fieldZ);
        y += SQUARE_SIZE + 2;

        // -- First Model Index (only if multi-model type) --
        RailModelProperties props = RailModelRegistry.elements.get(att.modelTypeKey);
        int modelCount = (props != null) ? props.getModelCount() : 0;
        if (modelCount > 1) {
            rightScrollPanel.children.add(new WidgetLabel(leftMargin, y + 6, halfW,
                    Text.translatable("gui.mtr.rail_editor_visual.first_model_index")));

            Button btnDecFMI = UtilitiesClient.newButton(Text.literal("<"), sender -> {
                att.firstModelIndex = (att.firstModelIndex - 1 + modelCount) % modelCount;
                sendUpdate(); Minecraft.getInstance().execute(this::loadPage);
            });
            IDrawing.setPositionAndWidth(btnDecFMI, leftMargin + halfW + 4, y, SQUARE_SIZE);
            rightScrollPanel.children.add(btnDecFMI);

            rightScrollPanel.children.add(new WidgetLabel(leftMargin + halfW + 4 + SQUARE_SIZE, y + 6,
                    innerW - halfW - 4 - SQUARE_SIZE * 2,
                    Text.literal(String.format("%d / %d", att.firstModelIndex % modelCount, modelCount))));

            Button btnIncFMI = UtilitiesClient.newButton(Text.literal(">"), sender -> {
                att.firstModelIndex = (att.firstModelIndex + 1) % modelCount;
                sendUpdate(); Minecraft.getInstance().execute(this::loadPage);
            });
            IDrawing.setPositionAndWidth(btnIncFMI, leftMargin + innerW - SQUARE_SIZE, y, SQUARE_SIZE);
            rightScrollPanel.children.add(btnIncFMI);
            y += SQUARE_SIZE + 2;
        }

        y += 4;
        return y;
    }

    private String getAttachmentLabel(RepeaterAttachment att) {
        if (att.modelTypeKey.isEmpty()) {
            return Text.translatable("gui.mtr.rail_editor_visual.default_model").getString();
        }
        RailModelProperties props = RailModelRegistry.elements.get(att.modelTypeKey);
        if (props != null && !props.name.getString().isEmpty()) {
            return props.name.getString();
        }
        return att.modelTypeKey;
    }

    // ==================== Position Bar ====================

    private WidgetManualPositionBar currentBar;
    private WidgetBetterTextField positionInputField;

    private int loadPositionBar(RailModelRepeater repeater, int y, int w) {
        float railLength = pickedRail != null ? (float) pickedRail.getLength() : 100f;
        boolean flipForDisplay = !userIsAtCanonStart();
        boolean isManual = repeater.repeaterMode == RepeaterMode.MANUAL;

        List<Float> computedPositions;
        if (isManual) {
            computedPositions = repeater.manualPositions;
        } else {
            computedPositions = computePositionsForCurrentMode(repeater);
        }

        List<Float> displayPositions;
        if (flipForDisplay) {
            displayPositions = new ArrayList<>(computedPositions.size());
            for (float pos : computedPositions) {
                displayPositions.add(railLength - pos);
            }
            Collections.reverse(displayPositions);
        } else {
            displayPositions = new ArrayList<>(computedPositions);
        }

        rightScrollPanel.children.add(new WidgetLabel(0, y + 6, w,
                Text.translatable("gui.mtr.rail_editor_visual.position_bar_info", displayPositions.size())));
        y += SQUARE_SIZE + 2;

        currentBar = new WidgetManualPositionBar(0, y, w, positions -> {
            if (!isManual) return;
            if (flipForDisplay) {
                List<Float> canonPositions = new ArrayList<>(positions.size());
                for (float pos : positions) {
                    canonPositions.add(quantizePosition(railLength - pos));
                }
                Collections.reverse(canonPositions);
                repeater.manualPositions = canonPositions;
            } else {
                repeater.manualPositions = new ArrayList<>(positions);
            }
            sendUpdate();
        });
        currentBar.setRailLength(railLength);
        currentBar.setEditable(isManual);
        currentBar.setPositions(displayPositions);
        if (savedBarSelectedIndex >= 0 && savedBarSelectedIndex < displayPositions.size()) {
            currentBar.setSelectedIndex(savedBarSelectedIndex);
        }
        if (savedBarZoomIndex > 0) {
            currentBar.setZoomIndex(savedBarZoomIndex);
        }
        if (savedBarViewCenter >= 0) {
            currentBar.setViewCenter(savedBarViewCenter);
        }

        Set<Integer> ovIndices = new HashSet<>();
        int posCount = computedPositions.size();
        for (int canonIdx : repeater.instanceOverrides.keySet()) {
            int displayIdx = flipForDisplay ? (posCount - 1 - canonIdx) : canonIdx;
            if (displayIdx >= 0 && displayIdx < posCount) {
                ovIndices.add(displayIdx);
            }
        }
        currentBar.setOverrideIndices(ovIndices);

        float playerProgress = computePlayerProgress();
        if (playerProgress >= 0) {
            float displayProgress = flipForDisplay ? (railLength - playerProgress) : playerProgress;
            currentBar.setPlayerProgress(displayProgress);
        }

        currentBar.setOnSelectionChange(() -> Minecraft.getInstance().execute(this::loadPage));
        rightScrollPanel.children.add(currentBar);
        y += currentBar.getHeight() + 4;

        if (isManual) {
            int halfW = w / 2 - 2;
            rightScrollPanel.children.add(new WidgetLabel(0, y + 6, halfW,
                    Text.translatable("gui.mtr.rail_editor_visual.selected_offset")));
            positionInputField = new WidgetBetterTextField("", 10);
            IDrawing.setPositionAndWidth(positionInputField, halfW + 4, y, halfW);
            rightScrollPanel.children.add(positionInputField);
            updatePositionInputField(true);
            positionInputField.setResponder(text -> {
                if (currentBar == null) return;
                int sel = currentBar.getSelectedIndex();
                if (sel < 0) return;
                try {
                    float val = Float.parseFloat(text);
                    val = quantizePosition(val);
                    currentBar.setPositionAt(sel, val);
                    positionInputField.setTextColor(0xE0E0E0);
                } catch (NumberFormatException e) {
                    positionInputField.setTextColor(0xFF0000);
                }
            });
            y += SQUARE_SIZE + 4;
        } else {
            positionInputField = null;
        }

        return y;
    }

    // ==================== Instance Override Panel ====================

    private void loadInstanceOverridePanel(RailModelRepeater repeater, int y, int w) {
        if (currentBar == null) return;
        int sel = currentBar.getSelectedIndex();
        if (sel < 0) return;

        int posCount = getPositionCount(repeater);
        int canonIdx = displayIndexToCanonIndex(sel, posCount);
        if (canonIdx < 0 || canonIdx >= posCount) return;

        RailModelInstanceOverride override = repeater.instanceOverrides.get(canonIdx);
        boolean hasOverride = override != null && !override.isEmpty();

        rightScrollPanel.children.add(new WidgetLabel(0, y + 6, w,
                Text.translatable("gui.mtr.rail_editor_visual.instance_override")));
        y += SQUARE_SIZE - 2;

        final int cIdx = canonIdx;
        if (!hasOverride) {
            Button btnAddOverride = UtilitiesClient.newButton(
                    Text.translatable("gui.mtr.rail_editor_visual.override_model"),
                    sender -> {
                        RailModelInstanceOverride newOv = new RailModelInstanceOverride();
                        newOv.attachments = new ArrayList<>(repeater.attachments.size());
                        for (RepeaterAttachment att : repeater.attachments) {
                            newOv.attachments.add(att.copy());
                        }
                        repeater.instanceOverrides.put(cIdx, newOv);
                        selectedOvAttIndex = 0;
                        sendUpdate();
                        Minecraft.getInstance().execute(this::loadPage);
                    }
            );
            IDrawing.setPositionAndWidth(btnAddOverride, 0, y, w);
            rightScrollPanel.children.add(btnAddOverride);
        } else {
            Button btnRemoveOverride = UtilitiesClient.newButton(
                    Text.translatable("gui.mtr.rail_editor_visual.cancel_override_model"),
                    sender -> {
                        repeater.instanceOverrides.remove(cIdx);
                        sendUpdate();
                        Minecraft.getInstance().execute(this::loadPage);
                    }
            );
            IDrawing.setPositionAndWidth(btnRemoveOverride, 0, y, w);
            rightScrollPanel.children.add(btnRemoveOverride);
            y += SQUARE_SIZE + 4;

            y = loadAttachmentListEditor(override.attachments, y, w,
                    20, ModelSelectTarget.ATTACHMENT_INSTANCE);
        }
    }

    private int getPositionCount(RailModelRepeater repeater) {
        if (repeater.repeaterMode == RepeaterMode.MANUAL) {
            return repeater.manualPositions.size();
        }
        return computePositionsForCurrentMode(repeater).size();
    }

    private int displayIndexToCanonIndex(int displayIdx, int posCount) {
        boolean flipForDisplay = !userIsAtCanonStart();
        return flipForDisplay ? (posCount - 1 - displayIdx) : displayIdx;
    }

    private void updatePositionInputField(boolean editable) {
        if (positionInputField == null || currentBar == null) return;
        int sel = currentBar.getSelectedIndex();
        if (sel >= 0) {
            positionInputField.active = editable;
            positionInputField.setValue(String.format("%.3f", currentBar.getSelectedPosition()));
            positionInputField.setTextColor(0xE0E0E0);
        } else {
            positionInputField.active = false;
            positionInputField.setValue("");
        }
    }

    private float computePlayerProgress() {
        if (pickedRail == null || Minecraft.getInstance().player == null) return -1;
        Vec3 playerPos = Minecraft.getInstance().player.position();
        double railLength = pickedRail.getLength();

        double bestT = 0;
        double bestDistSq = Double.MAX_VALUE;
        double step = Math.max(0.5, railLength / 200);
        for (double t = 0; t <= railLength; t += step) {
            Vec3 pos = pickedRail.getPosition(t);
            double dSq = pos.distanceToSqr(playerPos);
            if (dSq < bestDistSq) {
                bestDistSq = dSq;
                bestT = t;
            }
        }

        double lo = Math.max(0, bestT - step);
        double hi = Math.min(railLength, bestT + step);
        for (int i = 0; i < 16; i++) {
            double m1 = lo + (hi - lo) / 3;
            double m2 = hi - (hi - lo) / 3;
            double d1 = pickedRail.getPosition(m1).distanceToSqr(playerPos);
            double d2 = pickedRail.getPosition(m2).distanceToSqr(playerPos);
            if (d1 < d2) hi = m2;
            else lo = m1;
        }
        return (float) ((lo + hi) / 2);
    }

    private static float quantizePosition(float value) {
        return Math.round(value * 1000f) / 1000f;
    }

    private float resolveInterval(RailModelRepeater p) {
        if (p.intervalOverride > 0) return p.intervalOverride;
        String primaryKey = p.getPrimaryModelTypeKey();
        String resolvedKey = pickedRail != null
                ? RailRenderDispatcher.getModelKeyForRender(pickedRail, primaryKey)
                : primaryKey;
        RailModelProperties props = RailModelRegistry.elements.get(resolvedKey);
        if (props != null && props.repeatInterval > 0) return props.repeatInterval;
        props = RailModelRegistry.elements.get(primaryKey);
        if (props != null && props.repeatInterval > 0) return props.repeatInterval;
        return 1.0f;
    }

    private List<Float> computePositionsForCurrentMode(RailModelRepeater p) {
        double L = pickedRail != null ? pickedRail.getLength() : 100.0;
        double I = resolveInterval(p);

        List<Float> result = new ArrayList<>();
        switch (p.repeaterMode) {
            case STRETCH_INTERVAL: {
                if (L < I * 0.5) {
                    result.add(quantizePosition((float) (L / 2)));
                    break;
                }
                int N = Math.max(2, Math.round((float) (L / I)) + 1);
                double actualI = L / (N - 1);
                for (int k = 0; k < N; k++) {
                    result.add(quantizePosition((float) (k * actualI)));
                }
                break;
            }
            case FIXED_INTERVAL: {
                if (p.offsetFromStart) {
                    for (double t = p.offset; t < L - 0.001; t += I) {
                        result.add(quantizePosition((float) t));
                    }
                } else {
                    List<Float> tmp = new ArrayList<>();
                    for (double t = L - p.offset; t > 0.001; t -= I) {
                        tmp.add(quantizePosition((float) t));
                    }
                    Collections.reverse(tmp);
                    result.addAll(tmp);
                }
                break;
            }
            default:
                result.addAll(p.manualPositions);
                break;
        }
        return result;
    }

    // ==================== Propagation ====================

    private void sendPropagate(RailModelRepeater repeater, boolean undo) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        if (undo) {
            packet.writeByte(2);
        } else {
            packet.writeByte(0);
            packet.writeBlockPos(pickedPosStart);
            packet.writeBlockPos(pickedPosEnd);
            packet.writeVarInt(selectedLayerIndex);
            packet.writeUtf(repeater.getId());
            packet.writeFloat(resolveInterval(repeater));
            packet.writeFloat(repeater.offset);
            packet.writeVarInt(repeater.attachments.size());
            for (RepeaterAttachment att : repeater.attachments) {
                RailModelProperties props = RailModelRegistry.elements.get(att.modelTypeKey);
                int modelCount = (props != null) ? props.getModelCount() : 1;
                packet.writeVarInt(modelCount);
                packet.writeVarInt(att.firstModelIndex);
            }
        }
        RegistryClient.sendToServer(IPacket.PACKET_PROPAGATE_REPEATER_OFFSET, packet);
        onClose();
    }

    @Override
    protected void onBtnClick(String btnKey) {
        RailModelRepeater sel = getSelectedRepeater();
        if (sel == null) return;

        RepeaterAttachment targetAtt = getTargetAttachment(sel);
        if (targetAtt != null) {
            targetAtt.modelTypeKey = btnKey;
        }
        sendUpdate();
    }

    @Override
    protected List<Pair<String, String>> getRegistryEntries() {
        return RailModelRegistry.elements.entrySet().stream()
                .filter(e -> !e.getValue().name.getString().isEmpty())
                .map(e -> new Pair<>(e.getKey(), e.getValue().name.getString()))
                .toList();
    }

    private void sendUpdate() {
        if (pickedRail == null) return;
        PacketUpdateRail.sendUpdateC2S(pickedRail, pickedPosStart, pickedPosEnd);
        saveToToolNbt();
    }

    private void saveToToolNbt() {
        if (Minecraft.getInstance().player == null) return;
        ItemStack toolItem = Minecraft.getInstance().player.getMainHandItem();
        if (!toolItem.is(Main.RAIL_EDITOR_VISUAL.get())) return;
        CompoundTag tag = toolItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        writeRepeatersToNbt(tag, getRepeaters());
        toolItem.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        PacketUpdateHoldingItem.sendUpdateC2S();
    }

    public static void receivePropagationResult(FriendlyByteBuf packet) {
        lastTerminalNode = packet.readBlockPos();
        lastExitOffset = packet.readFloat();
        lastPropagateRepeaterId = packet.readUtf();
        lastPropagateInterval = packet.readFloat();
        int attCount = packet.readVarInt();
        lastExitFMI = new int[attCount];
        for (int i = 0; i < attCount; i++) {
            lastExitFMI[i] = packet.readVarInt();
        }
    }

    public static void acquirePickInfoWhenUse() {
        pickedRail = RailPicker.pickedRail;
        pickedPosStart = RailPicker.pickedPosStart;
        pickedPosEnd = RailPicker.pickedPosEnd;
    }

    public static boolean hasValidLastPick() {
        if (pickedRail == null) return false;
        Map<BlockPos, Rail> connections = ClientData.RAILS.get(pickedPosStart);
        if (connections == null) return false;
        return connections.containsKey(pickedPosEnd);
    }

    public static void openLastPickedScreen() {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new RailEditorVisualScreen()));
    }

    public static void batchApplyBrushTemplate(CompoundTag toolTag) {
        if (toolTag == null || pickedRail == null) return;
        RailExtraSupplier extra = (RailExtraSupplier) pickedRail;

        List<RailModelRepeater> template = readRepeatersFromNbt(toolTag);
        if (template.isEmpty()) {
            extra.setIsSecondaryDir(!extra.getIsSecondaryDir());
        } else {
            List<RailModelRepeater> current = extra.getRepeaters();
            boolean allMatch = templateMatchesCurrent(current, template);
            if (allMatch) {
                extra.setIsSecondaryDir(!extra.getIsSecondaryDir());
            } else {
                for (RailModelRepeater tp : template) {
                    RailModelRepeater existing = findById(current, tp.getId());
                    if (existing != null) {
                        if (tp.repeaterMode == RepeaterMode.MANUAL) continue;
                        existing.repeaterMode = tp.repeaterMode;
                        existing.intervalOverride = tp.intervalOverride;
                        existing.attachments = new ArrayList<>();
                        for (RepeaterAttachment att : tp.attachments) {
                            existing.attachments.add(att.copy());
                        }
                    } else {
                        current.add(tp.copy());
                    }
                }
            }
        }
        PacketUpdateRail.sendUpdateC2S(pickedRail, pickedPosStart, pickedPosEnd);
    }

    private static RailModelRepeater findById(List<RailModelRepeater> list, String id) {
        for (RailModelRepeater p : list) {
            if (p.getId().equals(id)) return p;
        }
        return null;
    }

    private static boolean templateMatchesCurrent(List<RailModelRepeater> current, List<RailModelRepeater> template) {
        for (RailModelRepeater tp : template) {
            RailModelRepeater cp = findById(current, tp.getId());
            if (cp == null) return false;
            if (cp.repeaterMode != tp.repeaterMode
                    || cp.intervalOverride != tp.intervalOverride) return false;
        }
        return true;
    }

    static void writeRepeatersToNbt(CompoundTag tag, List<RailModelRepeater> repeaters) {
        tag.putInt("RepeaterCount", repeaters.size());
        for (int i = 0; i < repeaters.size(); i++) {
            RailModelRepeater p = repeaters.get(i);
            CompoundTag layerTag = new CompoundTag();
            layerTag.putString("Id", p.id);
            layerTag.putInt("Mode", p.repeaterMode.ordinal());
            layerTag.putFloat("IntervalOverride", p.intervalOverride);
            layerTag.putInt("AttachmentCount", p.attachments.size());
            for (int j = 0; j < p.attachments.size(); j++) {
                RepeaterAttachment att = p.attachments.get(j);
                CompoundTag attTag = new CompoundTag();
                attTag.putString("ModelTypeKey", att.modelTypeKey);
                attTag.putBoolean("Reversed", att.reversed);
                attTag.putFloat("OffsetX", att.offsetX);
                attTag.putFloat("OffsetY", att.offsetY);
                attTag.putFloat("OffsetZ", att.offsetZ);
                attTag.putInt("FirstModelIndex", att.firstModelIndex);
                layerTag.put("Att_" + j, attTag);
            }
            tag.put("Repeater_" + i, layerTag);
        }
    }

    static List<RailModelRepeater> readRepeatersFromNbt(CompoundTag tag) {
        int count = tag.getIntOr("RepeaterCount", 0);
        List<RailModelRepeater> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CompoundTag layerTag = tag.getCompoundOrEmpty("Repeater_" + i);
            if (layerTag.isEmpty()) continue;
            RailModelRepeater p = new RailModelRepeater();
            p.id = layerTag.getStringOr("Id", "");
            p.repeaterMode = RepeaterMode.fromIndex(layerTag.getIntOr("Mode", 0));
            p.intervalOverride = layerTag.getFloatOr("IntervalOverride", 0);

            int attCount = layerTag.getIntOr("AttachmentCount", 0);
            if (attCount > 0) {
                p.attachments.clear();
                for (int j = 0; j < attCount; j++) {
                    CompoundTag attTag = layerTag.getCompoundOrEmpty("Att_" + j);
                    RepeaterAttachment att = new RepeaterAttachment();
                    att.modelTypeKey = attTag.getStringOr("ModelTypeKey", "");
                    att.reversed = attTag.getBooleanOr("Reversed", false);
                    att.offsetX = attTag.getFloatOr("OffsetX", 0);
                    att.offsetY = attTag.getFloatOr("OffsetY", 0);
                    att.offsetZ = attTag.getFloatOr("OffsetZ", 0);
                    att.firstModelIndex = attTag.getIntOr("FirstModelIndex", 0);
                    p.attachments.add(att);
                }
            } else if (layerTag.contains("ModelKey")) {
                // Legacy NBT compat
                p.attachments.clear();
                RepeaterAttachment att = new RepeaterAttachment();
                att.modelTypeKey = layerTag.getStringOr("ModelKey", "");
                att.reversed = layerTag.getBooleanOr("Reversed", false);
                p.attachments.add(att);
            }
            result.add(p);
        }
        return result;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        if (modelSelectTarget != ModelSelectTarget.NONE) {
            renderSelectPage(guiGraphics);
        }
    }

    @Override
    public void onClose() {
        if (modelSelectTarget != ModelSelectTarget.NONE) {
            modelSelectTarget = ModelSelectTarget.NONE;
            Minecraft.getInstance().execute(this::loadPage);
        } else {
            RailModelRepeater sel = getSelectedRepeater();
            if (sel != null) {
                lastEditedRepeaterId = sel.getId();
                pruneOverrides(sel);
            }
            this.minecraft.setScreen(null);
        }
    }

    private void pruneOverrides(RailModelRepeater repeater) {
        int posCount = getPositionCount(repeater);
        repeater.pruneOverrides(posCount);
        if (pickedRail != null) sendUpdate();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
