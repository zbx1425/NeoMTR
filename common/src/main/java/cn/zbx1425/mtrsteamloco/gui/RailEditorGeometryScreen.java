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
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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

    private int currentTab = 0; // 0 = node pose, 1 = vertical curve

    // Node pose state
    private boolean editingStartNode = true;
    private float currentAngle = 0;
    private boolean isUndetermined = true;
    private TransportMode transportMode = TransportMode.TRAIN;
    private boolean nodeAngleChanged = false;

    private boolean updatingFromCode = false;
    private WidgetBetterTextField textFieldAngle;
    private AngleSlider slider;

    private static final int COMPASS_RADIUS = 75;
    private static final int DIR_RING_RADIUS = 45;
    private static final int STEP_RING_RADIUS = 70;
    private static final int LINE_LENGTH = 80;
    private static final int DIR_BTN_SIZE = 18;
    private static final int STEP_BTN_SIZE = 18;
    private static final int TEXT_FIELD_WIDTH = 50;
    private static final int SLIDER_WIDTH = 130;
    private static final int LINE_THICKNESS = 2;
    private static final int[] STEP_DELTAS = {-5, -1, 1, 5};
    private static final String[] STEP_LABELS = {"-5", "-1", "+1", "+5"};
    private static final float[] STEP_ANGULAR_OFFSETS = {-30, -13, 13, 30};
    private static final int LINE_COLOR = 0xFFFF4444;
    private static final int RING_COLOR = 0xFF999999;

    private final Button[] directionButtons = new Button[16];
    private final Button[] stepButtons = new Button[4];

    public RailEditorGeometryScreen() {
        super(Text.translatable("gui.mtrsteamloco.rail_editor_geometry.title"));
        if (pickedRail == null) acquirePickInfoWhenUse();
        loadNodeState();
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
        IDrawing.setPositionAndWidth(addRenderableWidget(tabVerticalCurve), 0, SQUARE_SIZE * 2, LEFT_PANEL_WIDTH);

        // Right panel content
        switch (currentTab) {
            case 0 -> loadNodePoseTab();
            case 1 -> loadVerticalCurveTab();
        }
    }

    // ==================== Node Pose Tab ====================

    private void loadNodeState() {
        BlockPos nodePos = editingStartNode ? pickedPosStart : pickedPosEnd;
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
                currentAngle = tile.getAngleDegrees();
            }
        } else {
            BlockState state = world.getBlockState(nodePos);
            if (state.getBlock() instanceof BlockNode) {
                isUndetermined = false;
                currentAngle = BlockNode.getAngle(state);
            }
        }
    }

    private boolean isNodeFree(BlockPos pos) {
        Level world = Minecraft.getInstance().level;
        if (world == null) return false;
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof BlockFreeNode;
    }

    private void loadNodePoseTab() {
        int rightPanelWidth = Math.min(width - LEFT_PANEL_WIDTH - SQUARE_SIZE * 2, 380);
        int rightPanelX = LEFT_PANEL_WIDTH + (width - LEFT_PANEL_WIDTH - rightPanelWidth) / 2;
        int y = SQUARE_SIZE;

        // Node selector buttons
        int halfW = rightPanelWidth / 2 - 2;

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
        IDrawing.setPositionAndWidth(addRenderableWidget(btnThisNode), rightPanelX, y, halfW);

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
        IDrawing.setPositionAndWidth(addRenderableWidget(btnOtherNode), rightPanelX + halfW + 4, y, halfW);
        y += SQUARE_SIZE + 2;

        // Show coordinates
        BlockPos currentNodePos = editingStartNode ? pickedPosStart : pickedPosEnd;
        String coordLabel = String.format("(%d, %d, %d)", currentNodePos.getX(), currentNodePos.getY(), currentNodePos.getZ());
        addRenderableWidget(new WidgetLabel(rightPanelX, y + 4, rightPanelWidth, Text.literal(coordLabel)));
        y += SQUARE_SIZE;

        boolean isFree = isNodeFree(currentNodePos);
        if (!isFree) {
            addRenderableWidget(new WidgetLabel(rightPanelX, y + 4, rightPanelWidth,
                    Text.translatable("gui.mtrsteamloco.rail_editor_geometry.not_free_node")));
            return;
        }

        // Compass area
        int compassCx = rightPanelX + rightPanelWidth / 2;
        int compassCy = y + COMPASS_RADIUS + 10;

        // Angle text field at center of compass
        textFieldAngle = new WidgetBetterTextField("0.00", 10);
        textFieldAngle.setResponder(this::onTextFieldChanged);
        IDrawing.setPositionAndWidth(addRenderableWidget(textFieldAngle), compassCx - TEXT_FIELD_WIDTH / 2, compassCy - SQUARE_SIZE / 2, TEXT_FIELD_WIDTH);

        // 16 direction buttons around compass
        for (int i = 0; i < 16; i++) {
            float angle = -180 + i * 22.5f;
            double rad = Math.toRadians(angle);
            int bx = compassCx + (int) (DIR_RING_RADIUS * Math.cos(rad)) - DIR_BTN_SIZE / 2;
            int by = compassCy + (int) (DIR_RING_RADIUS * Math.sin(rad)) - DIR_BTN_SIZE / 2;
            final float finalAngle = angle;
            Button btn = UtilitiesClient.newButton(DIR_BTN_SIZE, Text.literal(""), b -> setAngleInternal(finalAngle, false));
            IDrawing.setPositionAndWidth(addRenderableWidget(btn), bx, by, DIR_BTN_SIZE);
            directionButtons[i] = btn;
        }

        // Step buttons
        for (int i = 0; i < 4; i++) {
            int delta = STEP_DELTAS[i];
            Button btn = UtilitiesClient.newButton(STEP_BTN_SIZE, Text.literal(STEP_LABELS[i]), b -> setAngleInternal(currentAngle + delta, false));
            double stepRad = Math.toRadians(currentAngle + STEP_ANGULAR_OFFSETS[i]);
            int bx = compassCx + (int) (STEP_RING_RADIUS * Math.cos(stepRad)) - STEP_BTN_SIZE / 2;
            int by = compassCy + (int) (STEP_RING_RADIUS * Math.sin(stepRad)) - STEP_BTN_SIZE / 2;
            IDrawing.setPositionAndWidth(addRenderableWidget(btn), bx, by, STEP_BTN_SIZE);
            btn.active = !isUndetermined;
            stepButtons[i] = btn;
        }

        y = compassCy + COMPASS_RADIUS + 16;

        // Slider
        slider = new AngleSlider(rightPanelX, y, SLIDER_WIDTH, SQUARE_SIZE, isUndetermined ? 0 : currentAngle);
        slider.active = !isUndetermined;
        addRenderableWidget(slider);

        // Reset button
        Button resetButton = UtilitiesClient.newButton(
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.reset_undetermined"),
                b -> {
                    isUndetermined = true;
                    currentAngle = 0;
                    nodeAngleChanged = true;
                    syncAllWidgets();
                }
        );
        Map<BlockPos, Rail> neighborMap = ClientData.RAILS.get(currentNodePos);
        boolean hasConnections = neighborMap != null && !neighborMap.isEmpty();
        resetButton.active = !hasConnections;
        IDrawing.setPositionAndWidth(addRenderableWidget(resetButton), rightPanelX + SLIDER_WIDTH + 4, y, rightPanelWidth - SLIDER_WIDTH - 4);
        y += SQUARE_SIZE + 6;

        // Derive button
        BlockPos otherPos = editingStartNode ? pickedPosEnd : pickedPosStart;
        Button deriveButton = UtilitiesClient.newButton(
                Text.translatable("gui.mtrsteamloco.rail_editor_geometry.derive_angle"),
                b -> deriveAngleFromNeighbor(currentNodePos, otherPos)
        );
        IDrawing.setPositionAndWidth(addRenderableWidget(deriveButton), rightPanelX, y, rightPanelWidth);

        syncAllWidgets();
    }

    private void setAngleInternal(float degrees, boolean fromTextField) {
        if (updatingFromCode) return;
        updatingFromCode = true;
        currentAngle = RailAngle.quantizeAngle(degrees);
        isUndetermined = false;
        nodeAngleChanged = true;
        if (!fromTextField && textFieldAngle != null) {
            textFieldAngle.setValue(formatAngle(currentAngle));
        }
        if (slider != null) {
            slider.setAngle(currentAngle);
            slider.active = true;
        }
        for (Button btn : stepButtons) {
            if (btn != null) btn.active = true;
        }
        updatingFromCode = false;
    }

    private void syncAllWidgets() {
        updatingFromCode = true;
        if (isUndetermined) {
            if (textFieldAngle != null) {
                textFieldAngle.setValue("");
                textFieldAngle.setEditable(false);
            }
            if (slider != null) {
                slider.setAngle(0);
                slider.active = false;
            }
            for (Button btn : stepButtons) {
                if (btn != null) btn.active = false;
            }
        } else {
            if (textFieldAngle != null) {
                textFieldAngle.setValue(formatAngle(currentAngle));
                textFieldAngle.setEditable(true);
            }
            if (slider != null) {
                slider.setAngle(currentAngle);
                slider.active = true;
            }
            for (Button btn : stepButtons) {
                if (btn != null) btn.active = true;
            }
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
        BlockPos nodePos = editingStartNode ? pickedPosStart : pickedPosEnd;
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
                    radiusInput.setTextColor(0xE0E0E0);
                } else {
                    radiusInput.setTextColor(0xEEEE00);
                }
                extra.setVerticalCurveRadius(newRadius);
                sendRailUpdate();
                btnSaveToTool.active = batchEnabled && toolRadius != newRadius;
            } catch (NumberFormatException e) {
                radiusInput.setTextColor(0xFF0000);
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
        return toolItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private void updateToolTag(java.util.function.Consumer<CompoundTag> modifier) {
        if (Minecraft.getInstance().player == null) return;
        ItemStack toolItem = Minecraft.getInstance().player.getMainHandItem();
        if (!toolItem.is(Main.RAIL_EDITOR_GEOMETRY.get())) return;
        CompoundTag tag = toolItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        modifier.accept(tag);
        toolItem.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        PacketUpdateHoldingItem.sendUpdateC2S();
    }

    private void sendRailUpdate() {
        if (pickedRail == null) return;
        PacketUpdateRail.sendUpdateC2S(pickedRail, pickedPosStart, pickedPosEnd);
    }

    // ==================== Batch Apply (called from mixin) ====================

    public static void acquirePickInfoWhenUse() {
        pickedRail = RailPicker.pickedRail;
        pickedPosStart = RailPicker.pickedPosStart;
        pickedPosEnd = RailPicker.pickedPosEnd;
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

        if (currentTab == 0 && !isUndetermined && isNodeFree(editingStartNode ? pickedPosStart : pickedPosEnd)) {
            int rightPanelWidth = Math.min(width - LEFT_PANEL_WIDTH - SQUARE_SIZE * 2, 380);
            int rightPanelX = LEFT_PANEL_WIDTH + (width - LEFT_PANEL_WIDTH - rightPanelWidth) / 2;
            int compassCx = rightPanelX + rightPanelWidth / 2;
            int compassCy = SQUARE_SIZE + SQUARE_SIZE + 2 + SQUARE_SIZE + COMPASS_RADIUS + 10;

            drawCompassRing(guiGraphics, compassCx, compassCy, COMPASS_RADIUS, RING_COLOR);
            for (float i = 0; i < 360; i += 22.5f) {
                drawTick(guiGraphics, compassCx, compassCy, i, COMPASS_RADIUS - 4, COMPASS_RADIUS, 1, RING_COLOR);
            }
            drawLine(guiGraphics, compassCx, compassCy, currentAngle, 12, LINE_LENGTH, LINE_THICKNESS, LINE_COLOR);
            drawLine(guiGraphics, compassCx, compassCy, currentAngle + 180, 12, LINE_LENGTH, LINE_THICKNESS, LINE_COLOR);
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
        // TODO: Check if this is correct
        guiGraphics.pose().rotate(angleDeg);
//        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(angleDeg));
        guiGraphics.fill(rStart, -thickness / 2, rEnd, -thickness / 2 + thickness, color);
        guiGraphics.pose().popMatrix();
    }

    private static void drawSegment(GuiGraphicsExtractor guiGraphics, float x1, float y1, float x2, float y2, int thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001F) return;
        float angleDeg = (float) Math.toDegrees(Math.atan2(dy, dx));
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x1, y1);
        guiGraphics.pose().rotate(angleDeg);
        // TODO: Check if this is correct
//        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(angleDeg));
        guiGraphics.fill(0, -thickness / 2, (int) Math.ceil(len), -thickness / 2 + thickness, color);
        guiGraphics.pose().popMatrix();
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
            return (float) (value * 360.0 - 180.0);
        }

        private static double angleToSlider(float degrees) {
            return (degrees + 180.0) / 360.0;
        }
    }
}
