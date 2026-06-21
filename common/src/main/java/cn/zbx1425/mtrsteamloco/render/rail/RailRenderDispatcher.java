package cn.zbx1425.mtrsteamloco.render.rail;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import cn.zbx1425.mtrsteamloco.data.RailModelRegistry;
import cn.zbx1425.mtrsteamloco.gui.SelectListScreen;
import cn.zbx1425.mtrsteamloco.mixin.LevelRendererAccessor;
import cn.zbx1425.sowcer.batch.BatchManager;
import cn.zbx1425.sowcer.batch.ShaderProp;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcerext.reuse.DrawScheduler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mtr.MTRClient;
import mtr.client.ClientData;
import mtr.data.Rail;
import mtr.data.RailType;
import mtr.data.TransportMode;
import mtr.util.Utilities;
import mtr.util.UtilitiesClient;
import mtr.render.RenderTrains;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class RailRenderDispatcher {

    private final HashMap<Rail, BakedRail> railRefMap = new HashMap<>();
    private final HashMap<ModelRef, HashMap<Long, RailChunkBase>> railChunkMap = new HashMap<>();
    private final List<RailChunkBase> railChunkList = new LinkedList<>();
    private boolean isInstanced;

    private final HashSet<Rail> currentFrameRails = new HashSet<>();

    public static boolean isHoldingMtrRailRelated = false;
    public static boolean isHoldingBrush = false;
    public static boolean isHoldingRailEditorVisual = false;
    public static boolean isHoldingRailEditorGeometry = false;
    public static boolean isHoldingNteRailRelated = false;
    public static boolean isHoldingRailRelated = false;
    public static boolean isPreviewingModel = false;

    private BlockPos[] findRailPositions(Rail rail) {
        for (Map.Entry<BlockPos, Map<BlockPos, Rail>> outer : ClientData.RAILS.entrySet()) {
            for (Map.Entry<BlockPos, Rail> inner : outer.getValue().entrySet()) {
                if (inner.getValue() == rail) {
                    return new BlockPos[]{outer.getKey(), inner.getKey()};
                }
            }
        }
        return null;
    }

    private void addRail(Rail rail) {
        if (railRefMap.containsKey(rail)) return;
        BlockPos[] positions = findRailPositions(rail);
        if (positions == null) return;

        BakedRail bakedRail = new BakedRail(rail, positions[0], positions[1]);
        railRefMap.put(rail, bakedRail);

        for (Map.Entry<ModelRef, HashMap<Long, ArrayList<Matrix4f>>> modelEntry : bakedRail.interiorModelsByChunks.entrySet()) {
            registerChunks(bakedRail, modelEntry.getKey(), modelEntry.getValue().keySet());
        }
        for (Map.Entry<ModelRef, HashMap<Long, ArrayList<BakedRail.TransformOnBoundary>>> modelEntry : bakedRail.boundaryModelsByChunks.entrySet()) {
            registerChunks(bakedRail, modelEntry.getKey(), modelEntry.getValue().keySet());
        }
    }

    private void registerChunks(BakedRail bakedRail, ModelRef modelRef, java.util.Set<Long> chunkIds) {
        HashMap<Long, RailChunkBase> chunkMap = railChunkMap.computeIfAbsent(modelRef, k -> new HashMap<>());
        for (long chunkId : chunkIds) {
            if (chunkMap.containsKey(chunkId)) {
                chunkMap.get(chunkId).addRail(bakedRail, modelRef);
            } else {
                RailChunkBase newChunk;
                if (isInstanced) {
                    newChunk = new InstancedRailChunk(chunkId, modelRef);
                } else {
                    newChunk = new MeshBuildingRailChunk(chunkId, modelRef);
                }
                newChunk.addRail(bakedRail, modelRef);
                chunkMap.put(chunkId, newChunk);
                railChunkList.add(newChunk);
            }
        }
    }

    private void removeRail(Rail rail) {
        if (!railRefMap.containsKey(rail)) return;
        BakedRail bakedRail = railRefMap.remove(rail);

        for (Map.Entry<ModelRef, HashMap<Long, ArrayList<Matrix4f>>> modelEntry : bakedRail.interiorModelsByChunks.entrySet()) {
            unregisterChunks(bakedRail, modelEntry.getKey(), modelEntry.getValue().keySet());
        }
        for (Map.Entry<ModelRef, HashMap<Long, ArrayList<BakedRail.TransformOnBoundary>>> modelEntry : bakedRail.boundaryModelsByChunks.entrySet()) {
            unregisterChunks(bakedRail, modelEntry.getKey(), modelEntry.getValue().keySet());
        }
    }

    private void unregisterChunks(BakedRail bakedRail, ModelRef modelRef, java.util.Set<Long> chunkIds) {
        HashMap<Long, RailChunkBase> chunkMap = railChunkMap.get(modelRef);
        if (chunkMap == null) return;
        for (long chunkId : chunkIds) {
            RailChunkBase chunk = chunkMap.get(chunkId);
            if (chunk != null) {
                chunk.removeRail(bakedRail, modelRef);
            }
        }
    }

    public boolean registerRail(Rail rail) {
        if (rail.railType == RailType.NONE || needsVanillaMTRRendering(rail)) return false;
        currentFrameRails.add(rail);
        return true;
    }

    public void clearRail() {
        currentFrameRails.clear();
        railRefMap.clear();
        for (HashMap<Long, RailChunkBase> chunkMap : railChunkMap.values()) {
            for (RailChunkBase chunk : chunkMap.values()) {
                chunk.close();
            }
            chunkMap.clear();
        }
        railChunkMap.clear();
        railChunkList.clear();
    }

    public void registerLightUpdate(int x, int yMin, int yMax, int z) {
        long chunkId = BakedRail.chunkIdFromSectPos(x, z);
        for (HashMap<Long, RailChunkBase> chunkMap : railChunkMap.values()) {
            RailChunkBase chunk = chunkMap.get(chunkId);
            if (chunk != null && !chunk.isDirty && chunk.containsYSection(yMin, yMax)) {
                chunk.isDirty = true;
            }
        }
    }

    public void prepareDraw() {
        if (Minecraft.getInstance().player == null) return;
        Screen currentScreen = Minecraft.getInstance().screen;
        isPreviewingModel = currentScreen instanceof SelectListScreen && ((SelectListScreen)currentScreen).isSelecting();
        if (!isPreviewingModel) {
            isHoldingMtrRailRelated = RenderTrains.isHoldingRailRelated(Minecraft.getInstance().player);
            isHoldingBrush = Utilities.isHolding(Minecraft.getInstance().player, (item) -> item.equals(mtr.Items.BRUSH.get()));
            isHoldingRailEditorVisual = Utilities.isHolding(Minecraft.getInstance().player, (item) -> item.equals(Main.RAIL_EDITOR_VISUAL.get()));
            isHoldingRailEditorGeometry = Utilities.isHolding(Minecraft.getInstance().player, (item) -> item.equals(Main.RAIL_EDITOR_GEOMETRY.get()));
            isHoldingNteRailRelated = isHoldingBrush || isHoldingRailEditorVisual || isHoldingRailEditorGeometry;
            isHoldingRailRelated = isHoldingMtrRailRelated || isHoldingNteRailRelated;
        } else {
            isHoldingMtrRailRelated = false;
            isHoldingBrush = false;
            isHoldingRailEditorVisual = false;
            isHoldingRailEditorGeometry = false;
            isHoldingNteRailRelated = false;
            isHoldingRailRelated = false;
        }
    }

    public void drawRails(Level level, BatchManager batchManager, Matrix4f viewMatrix) {
        boolean shouldBeInstanced = ClientConfig.getRailRenderLevel() == 3;
        if (isInstanced != shouldBeInstanced) clearRail();
        isInstanced = shouldBeInstanced;

        HashSet<Rail> railsToAdd = new HashSet<>(currentFrameRails);
        railsToAdd.removeAll(railRefMap.keySet());
        for (Rail rail : railsToAdd) addRail(rail);
        HashSet<Rail> railsToRemove = new HashSet<>(railRefMap.keySet());
        railsToRemove.removeAll(currentFrameRails);
        for (Rail rail : railsToRemove) removeRail(rail);
        currentFrameRails.clear();

        Vec3 cameraBlockPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        railChunkList.sort(Comparator.comparingDouble(chunk -> chunk.getCameraDistManhattanXZ(cameraBlockPos)));

        int buffersRebuilt = 0;
        Frustum cullingFrustum = ((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).getCullingFrustum();
        ShaderProp shaderProp = new ShaderProp().setViewMatrix(viewMatrix);

        int maxRailDistance = MTRClient.isReplayMod() ? 64 * 16 : (UtilitiesClient.getRenderDistance() + 1) * 16;
        boolean isOutsideRenderDistance = false;
        for (Iterator<RailChunkBase> it = railChunkList.iterator(); it.hasNext(); ) {
            RailChunkBase chunk = it.next();
            if (chunk.containingRails.isEmpty()) {
                chunk.close();
                it.remove();
                HashMap<Long, RailChunkBase> chunkMap = railChunkMap.get(chunk.modelRef);
                if (chunkMap != null) chunkMap.remove(chunk.chunkId);
                continue;
            }
            if (isOutsideRenderDistance) continue;
            if (chunk.cameraDistManhattanXZ > maxRailDistance) {
                isOutsideRenderDistance = true;
                continue;
            }
            if (chunk.isDirty || !chunk.bufferBuilt) {
#if DEBUG
                    chunk.rebuildBuffer(level);
//                    RenderUtil.displayStatusMessage("Rebuilt: " + chunk.getChunkPos().toString());
#else
                if (MTRClient.isReplayMod() || buffersRebuilt < 1) chunk.rebuildBuffer(level); // One per frame
#endif
                buffersRebuilt++;
            }
            if (chunk.bufferBuilt && cullingFrustum.isVisible(chunk.boundingBox)) {
                chunk.enqueue(batchManager, shaderProp);
            }
        }
    }

    public void drawRailNodes(Level level, DrawScheduler drawScheduler, Matrix4f viewMatrix) {
        if (isHoldingRailRelated) {
            HashSet<BlockPos> drawnNodes = new HashSet<>();
            for (Map.Entry<BlockPos, Map<BlockPos, Rail>> entryStart : ClientData.RAILS.entrySet()) {
                for (Map.Entry<BlockPos, Rail> entryEnd : entryStart.getValue().entrySet()) {
                    if (drawnNodes.add(entryStart.getKey())) {
                        Matrix4f nodePose = viewMatrix.copy();
                        nodePose.translate(entryStart.getKey().getX() + 0.5f,
                                entryStart.getKey().getY(), entryStart.getKey().getZ() + 0.5f);
                        nodePose.rotateY(-(float) entryEnd.getValue().facingStart.angleRadians + (float) Math.PI / 2);
                        final int light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, entryStart.getKey()),
                                level.getBrightness(LightLayer.SKY, entryStart.getKey()));
                        drawScheduler.enqueue(RailModelRegistry.railNodeModel, nodePose, light);
                    }
                    if (drawnNodes.add(entryEnd.getKey())) {
                        Matrix4f nodePose = viewMatrix.copy();
                        nodePose.translate(entryEnd.getKey().getX() + 0.5f,
                                entryEnd.getKey().getY(), entryEnd.getKey().getZ() + 0.5f);
                        nodePose.rotateY(-(float) entryEnd.getValue().facingEnd.angleRadians + (float) Math.PI / 2);
                        final int light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, entryEnd.getKey()),
                                level.getBrightness(LightLayer.SKY, entryEnd.getKey()));
                        drawScheduler.enqueue(RailModelRegistry.railNodeModel, nodePose, light);
                    }
                }
            }
        }
    }

    // "null": hidden, "": use MTR's default pipeline
    public static String getModelKeyForRender(Rail rail, String customModelKey) {
        if (customModelKey.equals("") || !RailModelRegistry.elements.containsKey(customModelKey)) {
            if (rail == null) return "";
            if (rail.transportMode == TransportMode.TRAIN) {
                if (rail.railType == RailType.SIDING) {
                    return "nte_builtin_depot";
                } else {
                    return "nte_builtin_concrete_sleeper";
                }
            } else {
                return "";
            }
        } else {
            if (customModelKey.equals("null")) {
                return isHoldingMtrRailRelated ? "" : "null";
            } else {
                return customModelKey;
            }
        }
    }

    public boolean needsVanillaMTRRendering(Rail rail) {
        RailExtraSupplier railExtra = (RailExtraSupplier) rail;
        if (railExtra.getRepeaters().isEmpty()) {
            return isHoldingMtrRailRelated;
        } else if (railExtra.getRepeaters().size() == 1) {
            if (railExtra.getRepeaters().getFirst().getPrimaryModelTypeKey().isEmpty()) {
                return rail.transportMode != TransportMode.TRAIN;
            }
        }
        return false;
    }

    public void drawBoundingBoxes(PoseStack matrixStack, VertexConsumer buffer) {
#if DEBUG
        for (RailChunkBase chunk : railChunkList) {
            boolean isChunkEven = chunk.isEven();
            LevelRenderer.renderLineBox(matrixStack, buffer, chunk.boundingBox,
                    1.0f, isChunkEven ? 1.0f : 0.0f, isChunkEven ? 0.0f : 1.0f, 1.0f);
        }
#endif
    }
}
