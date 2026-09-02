package cn.zbx1425.mtrsteamloco.gui;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import cn.zbx1425.mtrsteamloco.network.PacketUpdateHoldingItem;
import cn.zbx1425.mtrsteamloco.network.PacketUpdateRail;
import cn.zbx1425.mtrsteamloco.render.RailPicker;
import mtr.block.BlockFreeNode;
import mtr.block.BlockNode;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.*;
import mtr.screen.base.MTRScreen;
import mtr.mappings.Text;
import mtr.util.UtilitiesClient;
import mtr.packet.PacketTrainDataGuiClient;
import mtr.screen.WidgetBetterTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class RailEditorGeometryScreen extends MTRScreen {

    private static final int SQUARE_SIZE = 20;
    private static final int LEFT_PANEL_WIDTH = 140;

    private static Rail pickedRail = null;
    private static BlockPos pickedPosStart = BlockPos.ZERO;
    private static BlockPos pickedPosEnd = BlockPos.ZERO;
    private static BlockPos clickedNodePos = BlockPos.ZERO;

    private enum EditContext { CONNECTED_ENDPOINT, SINGLE_FREE_NODE }

    private int currentTab = 0; // 0 = node pose, 1 = vertical curve
    private EditContext editContext = EditContext.SINGLE_FREE_NODE;

    // Node pose state
    private boolean editingStartNode = true;
    private float currentAngle = 0;
    private boolean isUndetermined = true;
    private TransportMode transportMode = TransportMode.TRAIN;
    private boolean nodeAngleChanged = false;

    private boolean updatingFromCode = false;
    private WidgetBetterTextField textFieldAngle;
    private AngleSlider slider;

    private static final int PREVIEW_RADIUS = 55;
    private static final int PREVIEW_COL_WIDTH = PREVIEW_RADIUS * 2 + 16;
    private static final int LINE_LENGTH = 48;
    private static final int LINE_THICKNESS = 2;
    private static final int LINE_COLOR = 0xFFFF4444;
    private static final int RING_COLOR = 0xFFAAAAAA;
    private static final int TICK_COLOR_MAJOR = 0xFFAAAAAA;
    private static final int TICK_COLOR_MINOR = 0xFF666666;
    private static final int PREVIEW_BG_COLOR = 0xFF222222;
    private static final int SLIDER_HEIGHT = 20;
    private static final int TEXT_FIELD_WIDTH = 55;
    private static final int CONTENT_GAP = 10;

    private static final float[] PRESET_ANGLES = {-90, -45, 0, 45};
    private static final String[] PRESET_LABELS = {"-90°", "-45°", "0°", "45°"};
    private static final int[] STEP_DELTAS = {-5, -1, 1, 5};
    private static final String[] STEP_LABELS = {"-5", "-1", "+1", "+5"};

    public RailEditorGeometryScreen() {
        super(Text.translatable("gui.mtrsteamloco.rail_editor_geometry.title"));
        if (pickedRail == null && clickedNodePos.equals(BlockPos.ZERO)) {
            acquirePickInfoWhenUse(null);
        }
        detectContext();
        loadNodeState();
    }

    private void detectContext() {
        if (pickedRail != null) {
            editContext = EditContext.CONNECTED_ENDPOINT;
        } else {
            editContext = EditContext.SINGLE_FREE_NODE;
        }
    }

    @Override
    protected void init() {
        super.init();
        loadPage();
    }

    private void loadPage() {
        clearWidgets();

        // Left panel: tab buttons
        Button tabNodePose = UtilitiesClient.newButton(
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.tab_node_pose"),
                sender -> { currentTab = 0; Minecraft.getInstance().execute(this::loadPage); }
        );
        tabNodePose.active = (currentTab != 0);
        IDrawing.setPositionAndWidth(addRenderableWidget(tabNodePose), 0, SQUARE_SIZE, LEFT_PANEL_WIDTH);

        Button tabVerticalCurve = UtilitiesClient.newButton(
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.tab_vertical_curve"),
                sender -> { currentTab = 1; Minecraft.getInstance().execute(this::loadPage); }
        );
        tabVerticalCurve.active = (currentTab != 1);
        if (editContext == EditContext.SINGLE_FREE_NODE) tabVerticalCurve.active = false;
        IDrawing.setPositionAndWidth(addRenderableWidget(tabVerticalCurve), 0, SQUARE_SIZE * 2, LEFT_PANEL_WIDTH);

        switch (currentTab) {
            case 0 -> loadNodePoseTab();
            case 1 -> loadVerticalCurveTab();
        }
    }

    // ==================== Node Pose Tab ====================

    private void loadNodeState() {
        BlockPos nodePos = getEditingNodePos();
        Level world = Minecraft.getInstance().level;
        if (world == null) return;

        BlockEntity entity = world.getBlockEntity(nodePos);
        if (entity instanceof BlockFreeNode.TileEntityFreeNode tile) {
            transportMode = tile.getTransportMode();
            if (tile.isUndetermined()) {
                isUndetermined = true;
                currentAngle = 0;
            } else {
                isUndetermined = false;
                currentAngle = normalizeLineAngle(tile.getAngleDegrees());
            }
        } else {
            BlockState state = world.getBlockState(nodePos);
            if (state.getBlock() instanceof BlockNode) {
                isUndetermined = false;
                currentAngle = normalizeLineAngle(BlockNode.getAngle(state));
            }
        }
    }

    private BlockPos getEditingNodePos() {
        if (editContext == EditContext.SINGLE_FREE_NODE) {
            return clickedNodePos;
        }
        return editingStartNode ? pickedPosStart : pickedPosEnd;
    }

    private boolean isNodeFree(BlockPos pos) {
        Level world = Minecraft.getInstance().level;
        if (world == null) return false;
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof BlockFreeNode;
    }

    private void loadNodePoseTab() {
        int contentWidth = Math.min(width - LEFT_PANEL_WIDTH - 20, 500);
        int contentX = LEFT_PANEL_WIDTH + (width - LEFT_PANEL_WIDTH - contentWidth) / 2;
        int controlWidth = contentWidth - PREVIEW_COL_WIDTH - CONTENT_GAP;
        int controlX = contentX;
        int y = SQUARE_SIZE;

        if (editContext == EditContext.CONNECTED_ENDPOINT) {
            int halfW = controlWidth / 2 - 2;

            Button btnThisNode = UtilitiesClient.newButton(
                    Text.translatable("gui.mtrsteamloco.rail_editor_geometry.this_node"),
                    sender -> {
                        if (!editingStartNode) {
                            saveNodeAngleIfChanged();
                            editingStartNode = true;
                            loadNodeState();
                            Minecraft.getInstance().execute(this::loadPage);
                        }
                    }
            );
            btnThisNode.active = !editingStartNode;
            if (!isNodeFree(pickedPosStart)) btnThisNode.active = false;
            IDrawing.setPositionAndWidth(addRenderableWidget(btnThisNode), controlX, y, halfW);

            Button btnOtherNode = UtilitiesClient.newButton(
                    Text.translatable("gui.mtrsteamloco.rail_editor_geometry.other_node"),
                    sender -> {
                        if (editingStartNode) {
                            saveNodeAngleIfChanged();
                            editingStartNode = false;
                            loadNodeState();
                            Minecraft.getInstance().execute(this::loadPage);
                        }
                    }
            );
            btnOtherNode.active = editingStartNode;
            if (!isNodeFree(pickedPosEnd)) btnOtherNode.active = false;
            IDrawing.setPositionAndWidth(addRenderableWidget(btnOtherNode), controlX + halfW + 4, y, halfW);
            y += SQUARE_SIZE + 2;
        }

        // Coordinates
        BlockPos currentNodePos = getEditingNodePos();
        String coordLabel = String.format("(%d, %d, %d)", currentNodePos.getX(), currentNodePos.getY(), currentNodePos.getZ());
        addRenderableWidget(new WidgetLabel(controlX, y + 4, controlWidth, Text.literal(coordLabel)));
        y += SQUARE_SIZE;

        boolean isFree = isNodeFree(currentNodePos);
        if (!isFree) {
            addRenderableWidget(new WidgetLabel(controlX, y + 4, controlWidth,
                    Text.translatable("gui.mtrsteamloco.rail_editor_geometry.not_free_node")));
            return;
        }

        // Slider (full control width)
        slider = new AngleSlider(controlX, y, controlWidth, SLIDER_HEIGHT, isUndetermined ? 0 : currentAngle);
        addRenderableWidget(slider);
        y += SLIDER_HEIGHT + 4;

        // Row: text field + step buttons
        textFieldAngle = new WidgetBetterTextField("0.00", 10);
        textFieldAngle.setResponder(this::onTextFieldChanged);
        IDrawing.setPositionAndWidth(addRenderableWidget(textFieldAngle), controlX, y, TEXT_FIELD_WIDTH);

        int stepBtnWidth = 28;
        int stepStartX = controlX + TEXT_FIELD_WIDTH + 8;
        for (int i = 0; i < 4; i++) {
            int delta = STEP_DELTAS[i];
            Button btn = UtilitiesClient.newButton(SQUARE_SIZE, Text.literal(STEP_LABELS[i]),
                    b -> setAngleInternal(currentAngle + delta, false));
            IDrawing.setPositionAndWidth(addRenderableWidget(btn), stepStartX + i * (stepBtnWidth + 2), y, stepBtnWidth);
        }
        y += SQUARE_SIZE + 4;

        // Row: preset angle buttons
        int presetBtnWidth = (controlWidth - 12) / 4;
        for (int i = 0; i < PRESET_ANGLES.length; i++) {
            float angle = PRESET_ANGLES[i];
            Button btn = UtilitiesClient.newButton(SQUARE_SIZE, Text.literal(PRESET_LABELS[i]),
                    b -> setAngleInternal(angle, false));
            IDrawing.setPositionAndWidth(addRenderableWidget(btn), controlX + i * (presetBtnWidth + 4), y, presetBtnWidth);
        }
        y += SQUARE_SIZE + 6;

        // Single action button: auto_max_radius OR reset_nan
        Map<BlockPos, Rail> neighborMap = ClientData.RAILS.get(currentNodePos);
        boolean hasConnections = neighborMap != null && !neighborMap.isEmpty();

        if (hasConnections && editContext == EditContext.CONNECTED_ENDPOINT) {
            BlockPos otherPos = editingStartNode ? pickedPosEnd : pickedPosStart;
            Button actionButton = UtilitiesClient.newButton(
                    Text.translatable("gui.mtrsteamloco.rail_editor_geometry.auto_max_radius"),
                    b -> deriveAngleFromNeighbor(currentNodePos, otherPos)
            );
            float neighborAngle = readRawAngle(Minecraft.getInstance().level, otherPos);
            actionButton.active = !Float.isNaN(neighborAngle);
            IDrawing.setPositionAndWidth(addRenderableWidget(actionButton), controlX, y, controlWidth);
        } else {
            Button actionButton = UtilitiesClient.newButton(
                    Text.translatable("gui.mtrsteamloco.rail_editor_geometry.reset_nan"),
                    b -> {
                        isUndetermined = true;
                        currentAngle = 0;
                        nodeAngleChanged = true;
                        syncAllWidgets();
                    }
            );
            actionButton.active = !hasConnections;
            IDrawing.setPositionAndWidth(addRenderableWidget(actionButton), controlX, y, controlWidth);
        }

        syncAllWidgets();
    }

    private void setAngleInternal(float degrees, boolean fromTextField) {
        if (updatingFromCode) return;
        updatingFromCode = true;
        currentAngle = normalizeLineAngle(RailAngle.quantizeAngle(degrees));
        isUndetermined = false;
        nodeAngleChanged = true;
        if (!fromTextField && textFieldAngle != null) {
            textFieldAngle.setValue(formatAngle(currentAngle));
        }
        if (slider != null) {
            slider.setAngle(currentAngle);
        }
        updatingFromCode = false;
    }

    private void syncAllWidgets() {
        updatingFromCode = true;
        if (textFieldAngle != null) {
            textFieldAngle.setValue(isUndetermined ? "" : formatAngle(currentAngle));
            textFieldAngle.setEditable(true);
        }
        if (slider != null) {
            slider.setAngle(isUndetermined ? 0 : currentAngle);
        }
        updatingFromCode = false;
    }

    private void onTextFieldChanged(String text) {
        if (updatingFromCode) return;
        try {
            float parsed = Float.parseFloat(text.trim());
            if (Float.isFinite(parsed)) {
                setAngleInternal(parsed, true);
            }
        } catch (NumberFormatException ignored) {}
    }

    private void deriveAngleFromNeighbor(BlockPos nodePos, BlockPos neighborPos) {
        Level world = Minecraft.getInstance().level;
        if (world == null) return;

        float neighborRawAngle = readRawAngle(world, neighborPos);
        if (Float.isNaN(neighborRawAngle)) return;

        Double derivedDeg = RailCalculator.calculateMaxRadiusAngle(
                neighborPos.getX(), neighborPos.getZ(),
                nodePos.getX(), nodePos.getZ(),
                Math.toRadians(neighborRawAngle)
        );
        if (derivedDeg != null) {
            setAngleInternal(derivedDeg.floatValue(), false);
        }
    }

    private static float readRawAngle(Level world, BlockPos nodePos) {
        if (world == null) return Float.NaN;
        BlockState state = world.getBlockState(nodePos);
        if (state.getBlock() instanceof BlockFreeNode) {
            BlockEntity entity = world.getBlockEntity(nodePos);
            if (entity instanceof BlockFreeNode.TileEntityFreeNode tile) {
                return tile.getAngleDegrees();
            }
            return Float.NaN;
        }
        if (state.getBlock() instanceof BlockNode) {
            return BlockNode.getAngle(state);
        }
        return Float.NaN;
    }

    private void saveNodeAngleIfChanged() {
        if (!nodeAngleChanged) return;
        BlockPos nodePos = getEditingNodePos();
        if (!isNodeFree(nodePos)) return;
        float angle = isUndetermined ? 0 : currentAngle;
        PacketTrainDataGuiClient.sendFreeNodeC2S(nodePos, isUndetermined, angle, transportMode);
        nodeAngleChanged = false;
    }

    // ==================== Vertical Curve Tab ====================

    private void loadVerticalCurveTab() {
        if (pickedRail == null) return;
        RailExtraSupplier extra = (RailExtraSupplier) pickedRail;

        int rightPanelWidth = Math.min(width - LEFT_PANEL_WIDTH - SQUARE_SIZE * 2, 380);
        int rightPanelX = LEFT_PANEL_WIDTH + (width - LEFT_PANEL_WIDTH - rightPanelWidth) / 2;
        int y = SQUARE_SIZE;

        // Title
        addRenderableWidget(new WidgetLabel(rightPanelX, y + 4, rightPanelWidth,
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.vertical_curve_radius")));
        y += SQUARE_SIZE;

        // Radius input
        int halfW = rightPanelWidth / 2 - 2;
        WidgetBetterTextField radiusInput = new WidgetBetterTextField("", 8);
        float currentRadius = extra.getVerticalCurveRadius();
        if (currentRadius > 0) {
            radiusInput.setValue(Integer.toString((int) currentRadius));
        } else {
            radiusInput.setValue("");
        }
        IDrawing.setPositionAndWidth(addRenderableWidget(radiusInput), rightPanelX, y, halfW);

        // Max (Default) button
        Button btnMax = UtilitiesClient.newButton(
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.radius_set_max"),
                sender -> {
                    extra.setVerticalCurveRadius(0);
                    sendRailUpdate();
                    Minecraft.getInstance().execute(this::loadPage);
                }
        );
        btnMax.active = currentRadius != 0;
        int btnW = (rightPanelWidth - halfW - 4) / 2 - 1;
        IDrawing.setPositionAndWidth(addRenderableWidget(btnMax), rightPanelX + halfW + 4, y, btnW);

        // No Curve button
        Button btnNone = UtilitiesClient.newButton(
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.radius_set_none"),
                sender -> {
                    extra.setVerticalCurveRadius(-1);
                    sendRailUpdate();
                    Minecraft.getInstance().execute(this::loadPage);
                }
        );
        btnNone.active = currentRadius >= 0;
        IDrawing.setPositionAndWidth(addRenderableWidget(btnNone), rightPanelX + halfW + 4 + btnW + 2, y, btnW);
        y += SQUARE_SIZE + 4;

        // Reference values label
        String valuesText = getVerticalValueText(currentRadius);
        addRenderableWidget(new WidgetLabel(rightPanelX, y + 4, rightPanelWidth, Text.literal(valuesText)));
        y += SQUARE_SIZE + 4;

        addRenderableWidget(new WidgetLabel(rightPanelX, y + 4, rightPanelWidth,
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.radius_ref")));
        y += SQUARE_SIZE * 2;

        // ---- Batch Apply Section ----
        addRenderableWidget(new WidgetLabel(rightPanelX, y + 4, rightPanelWidth,
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.batch_apply_section")));
        y += SQUARE_SIZE;

        CompoundTag toolTag = getToolTag();
        boolean batchEnabled = toolTag != null && toolTag.getBooleanOr("BatchApplyVerticalCurve", false);
        float toolRadius = toolTag != null ? toolTag.getFloatOr("VerticalCurveRadius", 0) : 0;

        Button btnBatchOn = UtilitiesClient.newButton(
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.batch_apply_on"),
                sender -> {
                    updateToolTag(tag -> {
                        tag.putBoolean("BatchApplyVerticalCurve", true);
                        tag.putFloat("VerticalCurveRadius", extra.getVerticalCurveRadius());
                    });
                    Minecraft.getInstance().execute(this::loadPage);
                }
        );
        btnBatchOn.active = !batchEnabled;
        IDrawing.setPositionAndWidth(addRenderableWidget(btnBatchOn), rightPanelX, y, halfW);

        Button btnBatchOff = UtilitiesClient.newButton(
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.batch_apply_off"),
                sender -> {
                    updateToolTag(tag -> tag.putBoolean("BatchApplyVerticalCurve", false));
                    Minecraft.getInstance().execute(this::loadPage);
                }
        );
        btnBatchOff.active = batchEnabled;
        IDrawing.setPositionAndWidth(addRenderableWidget(btnBatchOff), rightPanelX + halfW + 4, y, halfW);
        y += SQUARE_SIZE + 4;

        // "Save to Tool" button + tool value label
        String toolValueStr = formatRadiusForDisplay(toolRadius);
        addRenderableWidget(new WidgetLabel(rightPanelX, y + 4, halfW,
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.tool_value", toolValueStr)));

        Button btnSaveToTool = UtilitiesClient.newButton(
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.save_to_tool"),
                sender -> {
                    updateToolTag(tag -> tag.putFloat("VerticalCurveRadius", extra.getVerticalCurveRadius()));
                    Minecraft.getInstance().execute(this::loadPage);
                }
        );
        btnSaveToTool.active = batchEnabled && toolRadius != currentRadius;
        IDrawing.setPositionAndWidth(addRenderableWidget(btnSaveToTool), rightPanelX + halfW + 4, y, halfW);

        // Wire up radius input responder (after btnSaveToTool is created)
        radiusInput.setResponder(text -> {
            if (text.isEmpty()) return;
            try {
                float newRadius = Float.parseFloat(text);
                int H = Math.abs(extra.getHeight());
                double L = pickedRail.getLength();
                double maxRadius = (H == 0) ? 0 : (H * H + L * L) / (H * 4.0);
                if (newRadius < maxRadius) {
                    radiusInput.setTextColor(0xFFE0E0E0);
                } else {
                    radiusInput.setTextColor(0xFFEEEE00);
                }
                extra.setVerticalCurveRadius(newRadius);
                sendRailUpdate();
                btnSaveToTool.active = batchEnabled && toolRadius != newRadius;
            } catch (NumberFormatException e) {
                radiusInput.setTextColor(0xFFFF0000);
            }
        });
    }

    private String getVerticalValueText(float verticalRadius) {
        if (pickedRail == null) return "";
        RailExtraSupplier extra = (RailExtraSupplier) pickedRail;
        int H = Math.abs(extra.getHeight());
        double L = pickedRail.getLength();
        double maxRadius = (H == 0) ? 0 : (H * H + L * L) / (H * 4.0);
        double gradient;
        if (verticalRadius < 0) {
            gradient = H / L * 1000;
        } else if (verticalRadius == 0 || verticalRadius > maxRadius) {
            gradient = Math.tan(RailExtraSupplier.getVTheta(pickedRail, maxRadius)) * 1000;
        } else {
            gradient = Math.tan(RailExtraSupplier.getVTheta(pickedRail, verticalRadius)) * 1000;
        }
        return Text.translatable("gui.mtrsteamloco.rail_editor_geometry.radius_values",
                String.format("%.1f", maxRadius), String.format("%.1f", gradient)
        ).getString();
    }

    private static String formatRadiusForDisplay(float radius) {
        if (radius < 0) return "No Curve";
        if (radius == 0) return "Max";
        return Integer.toString((int) radius);
    }

    // ==================== Tool NBT Helpers ====================

    private CompoundTag getToolTag() {
        if (Minecraft.getInstance().player == null) return null;
        ItemStack toolItem = Minecraft.getInstance().player.getMainHandItem();
        if (!toolItem.is(Main.RAIL_EDITOR_GEOMETRY.get())) return null;
        return toolItem.getOrDefault(Main.TOOL_TAG.get(), new CompoundTag()).copy();
    }

    private void updateToolTag(java.util.function.Consumer<CompoundTag> modifier) {
        if (Minecraft.getInstance().player == null) return;
        ItemStack toolItem = Minecraft.getInstance().player.getMainHandItem();
        if (!toolItem.is(Main.RAIL_EDITOR_GEOMETRY.get())) return;
        CompoundTag tag = toolItem.getOrDefault(Main.TOOL_TAG.get(), new CompoundTag()).copy();
        modifier.accept(tag);
        toolItem.set(Main.TOOL_TAG.get(), tag);
        PacketUpdateHoldingItem.sendUpdateC2S();
    }

    private void sendRailUpdate() {
        if (pickedRail == null) return;
        PacketUpdateRail.sendUpdateC2S(pickedRail, pickedPosStart, pickedPosEnd);
    }

    // ==================== Entry & Batch Apply ====================

    public static void acquirePickInfoWhenUse(BlockPos clickedPos) {
        pickedRail = RailPicker.pickedRail;
        pickedPosStart = RailPicker.pickedPosStart;
        pickedPosEnd = RailPicker.pickedPosEnd;
        clickedNodePos = clickedPos != null ? clickedPos :
                (pickedPosStart != null ? pickedPosStart : BlockPos.ZERO);
    }

    public static void batchApply(CompoundTag toolTag) {
        if (toolTag == null || pickedRail == null) return;
        RailExtraSupplier extra = (RailExtraSupplier) pickedRail;
        boolean propertyUpdated = false;

        if (toolTag.getBooleanOr("BatchApplyVerticalCurve", false)) {
            float toolRadius = toolTag.getFloatOr("VerticalCurveRadius", 0);
            if (toolRadius != extra.getVerticalCurveRadius()) {
                extra.setVerticalCurveRadius(toolRadius);
                propertyUpdated = true;
            }
        }

        if (!propertyUpdated) {
            extra.setIsSecondaryDir(!extra.getIsSecondaryDir());
        }
        PacketUpdateRail.sendUpdateC2S(pickedRail, pickedPosStart, pickedPosEnd);
    }

    // ==================== Rendering ====================

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);

        if (currentTab == 0 && isNodeFree(getEditingNodePos())) {
            int contentWidth = Math.min(width - LEFT_PANEL_WIDTH - 20, 500);
            int contentX = LEFT_PANEL_WIDTH + (width - LEFT_PANEL_WIDTH - contentWidth) / 2;
            int controlWidth = contentWidth - PREVIEW_COL_WIDTH - CONTENT_GAP;
            int previewX = contentX + controlWidth + CONTENT_GAP;

            int previewTop = SQUARE_SIZE;
            int compassCx = previewX + PREVIEW_COL_WIDTH / 2;
            int compassCy = previewTop + PREVIEW_COL_WIDTH / 2;

            // Dark gray background for preview area
            guiGraphics.fill(previewX, previewTop, previewX + PREVIEW_COL_WIDTH, previewTop + PREVIEW_COL_WIDTH, PREVIEW_BG_COLOR);

            // Draw ring
            drawCompassRing(guiGraphics, compassCx, compassCy, PREVIEW_RADIUS, RING_COLOR);

            // Draw tick marks: 45° = large, 22.5° = small
            for (int i = 0; i < 16; i++) {
                float tickAngle = i * 22.5f;
                boolean isMajor = (i % 2 == 0);
                int innerR = isMajor ? PREVIEW_RADIUS - 7 : PREVIEW_RADIUS - 4;
                int thickness = isMajor ? 2 : 1;
                int color = isMajor ? TICK_COLOR_MAJOR : TICK_COLOR_MINOR;
                drawTick(guiGraphics, compassCx, compassCy, tickAngle, innerR, PREVIEW_RADIUS, thickness, color);
            }

            // Draw direction line (both sides)
            if (!isUndetermined) {
                drawLine(guiGraphics, compassCx, compassCy, currentAngle, 8, LINE_LENGTH, LINE_THICKNESS, LINE_COLOR);
                drawLine(guiGraphics, compassCx, compassCy, currentAngle + 180, 8, LINE_LENGTH, LINE_THICKNESS, LINE_COLOR);
                drawLine(guiGraphics, compassCx, compassCy, currentAngle + 90, 0, 20, LINE_THICKNESS, TICK_COLOR_MAJOR);
                drawLine(guiGraphics, compassCx, compassCy, currentAngle - 90, 0, 20, LINE_THICKNESS, TICK_COLOR_MAJOR);
            }
        }
    }

    @Override
    public void onClose() {
        saveNodeAngleIfChanged();
        this.minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ==================== Drawing Helpers ====================

    private static void drawCompassRing(GuiGraphicsExtractor guiGraphics, int cx, int cy, int radius, int color) {
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            float a1 = 360F * i / segments;
            float a2 = 360F * (i + 1) / segments;
            double r1 = Math.toRadians(a1);
            double r2 = Math.toRadians(a2);
            float x1 = cx + (float) (radius * Math.cos(r1));
            float y1 = cy + (float) (radius * Math.sin(r1));
            float x2 = cx + (float) (radius * Math.cos(r2));
            float y2 = cy + (float) (radius * Math.sin(r2));
            drawSegment(guiGraphics, x1, y1, x2, y2, 1, color);
        }
    }

    private static void drawTick(GuiGraphicsExtractor guiGraphics, int cx, int cy, float angleDeg, int rInner, int rOuter, int thickness, int color) {
        double rad = Math.toRadians(angleDeg);
        float x1 = cx + (float) (rInner * Math.cos(rad));
        float y1 = cy + (float) (rInner * Math.sin(rad));
        float x2 = cx + (float) (rOuter * Math.cos(rad));
        float y2 = cy + (float) (rOuter * Math.sin(rad));
        drawSegment(guiGraphics, x1, y1, x2, y2, thickness, color);
    }

    private static void drawLine(GuiGraphicsExtractor guiGraphics, int cx, int cy, float angleDeg, int rStart, int rEnd, int thickness, int color) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(cx, cy);
        guiGraphics.pose().rotate((float)Math.toRadians(angleDeg));
        guiGraphics.fill(rStart, -thickness / 2, rEnd, -thickness / 2 + thickness, color);
        guiGraphics.pose().popMatrix();
    }

    private static void drawSegment(GuiGraphicsExtractor guiGraphics, float x1, float y1, float x2, float y2, int thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001F) return;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x1, y1);
        guiGraphics.pose().rotate((float)Math.atan2(dy, dx));
        guiGraphics.fill(0, -thickness / 2, (int) Math.ceil(len), -thickness / 2 + thickness, color);
        guiGraphics.pose().popMatrix();
    }

    // ==================== Angle Utilities ====================

    /**
     * Normalize an angle to [-90, 90) representing a line direction (180° equivalent).
     */
    private static float normalizeLineAngle(float degrees) {
        float n = RailAngle.normalizeAngle(degrees);
        if (n >= 90) return n - 180;
        if (n < -90) return n + 180;
        return n;
    }

    private static String formatAngle(float angle) {
        if (angle == (int) angle) return String.valueOf((int) angle);
        String s = String.format("%.2f", angle);
        if (s.contains(".")) {
            while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    // ==================== Angle Slider ====================

    private class AngleSlider extends AbstractSliderButton {

        AngleSlider(int x, int y, int w, int h, float initialAngle) {
            super(x, y, w, h, Text.literal(formatAngle(initialAngle) + "\u00B0"), angleToSlider(initialAngle));
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(formatAngle(sliderToAngle()) + "\u00B0"));
        }

        @Override
        protected void applyValue() {
            setAngleInternal(sliderToAngle(), false);
        }

        void setAngle(float degrees) {
            value = angleToSlider(degrees);
            updateMessage();
        }

        private float sliderToAngle() {
            return (float) (value * 180.0 - 90.0);
        }

        private static double angleToSlider(float degrees) {
            return (degrees + 90.0) / 180.0;
        }
    }
}
