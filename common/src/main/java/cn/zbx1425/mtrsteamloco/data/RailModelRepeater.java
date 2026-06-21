package cn.zbx1425.mtrsteamloco.data;

import io.netty.buffer.Unpooled;
import mtr.Registry;
import mtr.data.Rail;
import mtr.data.RailwayData;
import mtr.packet.IPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.Value;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.MapValue;

import java.io.IOException;
import java.util.*;

public class RailModelRepeater {

    public String id;
    public RepeaterMode repeaterMode;
    public float offset;
    public boolean offsetFromStart;
    public float intervalOverride;
    public List<Float> manualPositions;
    public List<RepeaterAttachment> attachments;
    public Map<Integer, RailModelInstanceOverride> instanceOverrides;

    public RailModelRepeater() {
        this.id = "";
        this.repeaterMode = RepeaterMode.STRETCH_INTERVAL;
        this.offset = 0;
        this.offsetFromStart = true;
        this.intervalOverride = 0;
        this.manualPositions = Collections.emptyList();
        this.attachments = new ArrayList<>();
        this.attachments.add(new RepeaterAttachment());
        this.instanceOverrides = new HashMap<>();
    }

    public RailModelRepeater(String modelTypeKey, boolean reversed) {
        this();
        this.attachments.get(0).modelTypeKey = modelTypeKey;
        this.attachments.get(0).reversed = reversed;
    }

    /** Effective id for matching in propagation and batch operations. */
    public String getId() {
        if (!id.isEmpty()) return id;
        if (!attachments.isEmpty()) return attachments.get(0).modelTypeKey;
        return "";
    }

    /** Primary model type key (from first attachment). Used for display and legacy compat. */
    public String getPrimaryModelTypeKey() {
        return attachments.isEmpty() ? "" : attachments.get(0).modelTypeKey;
    }

    public RailModelRepeater copy() {
        RailModelRepeater copy = new RailModelRepeater();
        copy.id = this.id;
        copy.repeaterMode = this.repeaterMode;
        copy.offset = this.offset;
        copy.offsetFromStart = this.offsetFromStart;
        copy.intervalOverride = this.intervalOverride;
        copy.manualPositions = new ArrayList<>(this.manualPositions);
        copy.attachments = new ArrayList<>(this.attachments.size());
        for (RepeaterAttachment att : this.attachments) {
            copy.attachments.add(att.copy());
        }
        copy.instanceOverrides = new HashMap<>();
        for (Map.Entry<Integer, RailModelInstanceOverride> e : this.instanceOverrides.entrySet()) {
            copy.instanceOverrides.put(e.getKey(), e.getValue().copy());
        }
        return copy;
    }

    public boolean isLegacyCompatible() {
        return repeaterMode == RepeaterMode.STRETCH_INTERVAL
                && id.isEmpty()
                && offset == 0
                && offsetFromStart
                && intervalOverride == 0
                && manualPositions.isEmpty()
                && instanceOverrides.isEmpty()
                && attachments.size() == 1
                && attachments.get(0).offsetX == 0
                && attachments.get(0).offsetY == 0
                && attachments.get(0).offsetZ == 0
                && !attachments.get(0).reversed
                && attachments.get(0).firstModelIndex == 0;
    }

    public void pruneOverrides(int positionCount) {
        instanceOverrides.entrySet().removeIf(e -> e.getKey() < 0 || e.getKey() >= positionCount || e.getValue().isEmpty());
    }

    public float resolveInterval(RailModelProperties properties) {
        return intervalOverride > 0 ? intervalOverride : properties.repeatInterval;
    }

    // ==================== Serialization: MessagePack ====================

    public void toMessagePack(MessagePacker packer) throws IOException {
        Map<Integer, RailModelInstanceOverride> nonDefaultOv = new HashMap<>();
        for (Map.Entry<Integer, RailModelInstanceOverride> e : instanceOverrides.entrySet()) {
            if (!e.getValue().isEmpty()) {
                nonDefaultOv.put(e.getKey(), e.getValue());
            }
        }

        int fieldCount = 6;
        if (!id.isEmpty()) fieldCount++;
        if (!nonDefaultOv.isEmpty()) fieldCount++;

        packer.packMapHeader(fieldCount);
        if (!id.isEmpty()) {
            packer.packString("id").packString(id);
        }
        packer.packString("mode").packInt(repeaterMode.ordinal());
        packer.packString("offset").packFloat(offset);
        packer.packString("offset_from_start").packBoolean(offsetFromStart);
        packer.packString("interval_override").packFloat(intervalOverride);
        packer.packString("manual_positions").packArrayHeader(manualPositions.size());
        for (float pos : manualPositions) {
            packer.packFloat(pos);
        }
        packer.packString("attachments").packArrayHeader(attachments.size());
        for (RepeaterAttachment att : attachments) {
            att.toMessagePack(packer);
        }
        if (!nonDefaultOv.isEmpty()) {
            packer.packString("instance_overrides").packMapHeader(nonDefaultOv.size());
            for (Map.Entry<Integer, RailModelInstanceOverride> e : nonDefaultOv.entrySet()) {
                packer.packInt(e.getKey());
                e.getValue().toMessagePack(packer);
            }
        }
    }

    public static RailModelRepeater fromMessagePack(MapValue mapValue) {
        RailModelRepeater repeater = new RailModelRepeater();
        repeater.attachments.clear();
        Map<Value, Value> map = mapValue.map();

        // Detect legacy format (has "model_key" field)
        boolean isLegacy = false;
        for (Map.Entry<Value, Value> entry : map.entrySet()) {
            if (entry.getKey().asStringValue().asString().equals("model_key")) {
                isLegacy = true;
                break;
            }
        }

        if (isLegacy) {
            return fromMessagePackLegacy(map);
        }

        for (Map.Entry<Value, Value> entry : map.entrySet()) {
            String key = entry.getKey().asStringValue().asString();
            Value val = entry.getValue();
            switch (key) {
                case "id":
                    repeater.id = val.asStringValue().asString();
                    break;
                case "mode":
                    repeater.repeaterMode = RepeaterMode.fromIndex(val.asIntegerValue().asInt());
                    break;
                case "offset":
                    repeater.offset = val.asFloatValue().toFloat();
                    break;
                case "offset_from_start":
                    repeater.offsetFromStart = val.asBooleanValue().getBoolean();
                    break;
                case "interval_override":
                    repeater.intervalOverride = val.asFloatValue().toFloat();
                    break;
                case "manual_positions":
                    ArrayValue arr = val.asArrayValue();
                    List<Float> positions = new ArrayList<>(arr.size());
                    for (Value v : arr) {
                        positions.add(v.asFloatValue().toFloat());
                    }
                    repeater.manualPositions = positions;
                    break;
                case "attachments":
                    ArrayValue attArr = val.asArrayValue();
                    for (Value v : attArr) {
                        repeater.attachments.add(RepeaterAttachment.fromMessagePack(v.asMapValue()));
                    }
                    break;
                case "instance_overrides":
                    MapValue ovMap = val.asMapValue();
                    for (Map.Entry<Value, Value> ovEntry : ovMap.entrySet()) {
                        int idx = ovEntry.getKey().asIntegerValue().asInt();
                        RailModelInstanceOverride ov = RailModelInstanceOverride.fromMessagePack(ovEntry.getValue().asMapValue());
                        if (!ov.isEmpty()) {
                            repeater.instanceOverrides.put(idx, ov);
                        }
                    }
                    break;
            }
        }

        if (repeater.attachments.isEmpty()) {
            repeater.attachments.add(new RepeaterAttachment());
        }
        return repeater;
    }

    /** Parse legacy format (pre-attachment architecture) and convert. */
    private static RailModelRepeater fromMessagePackLegacy(Map<Value, Value> map) {
        RailModelRepeater repeater = new RailModelRepeater();
        repeater.attachments.clear();

        String modelKey = "";
        boolean reversed = false;

        for (Map.Entry<Value, Value> entry : map.entrySet()) {
            String key = entry.getKey().asStringValue().asString();
            Value val = entry.getValue();
            switch (key) {
                case "model_key":
                    modelKey = val.asStringValue().asString();
                    break;
                case "mode":
                    repeater.repeaterMode = RepeaterMode.fromIndex(val.asIntegerValue().asInt());
                    break;
                case "offset":
                    repeater.offset = val.asFloatValue().toFloat();
                    break;
                case "offset_from_start":
                    repeater.offsetFromStart = val.asBooleanValue().getBoolean();
                    break;
                case "reversed":
                    reversed = val.asBooleanValue().getBoolean();
                    break;
                case "interval_override":
                    repeater.intervalOverride = val.asFloatValue().toFloat();
                    break;
                case "manual_positions":
                    ArrayValue arr = val.asArrayValue();
                    List<Float> positions = new ArrayList<>(arr.size());
                    for (Value v : arr) {
                        positions.add(v.asFloatValue().toFloat());
                    }
                    repeater.manualPositions = positions;
                    break;
                case "instance_overrides":
                    MapValue ovMap = val.asMapValue();
                    for (Map.Entry<Value, Value> ovEntry : ovMap.entrySet()) {
                        int idx = ovEntry.getKey().asIntegerValue().asInt();
                        MapValue ovData = ovEntry.getValue().asMapValue();
                        RepeaterAttachment att = new RepeaterAttachment();
                        for (Map.Entry<Value, Value> field : ovData.entrySet()) {
                            switch (field.getKey().asStringValue().asString()) {
                                case "mk": att.modelTypeKey = field.getValue().asStringValue().asString(); break;
                                case "ox": att.offsetX = field.getValue().asFloatValue().toFloat(); break;
                                case "oy": att.offsetY = field.getValue().asFloatValue().toFloat(); break;
                                case "oz": att.offsetZ = field.getValue().asFloatValue().toFloat(); break;
                                case "rv": att.reversed = field.getValue().asBooleanValue().getBoolean(); break;
                            }
                        }
                        if (!att.isDefault()) {
                            RailModelInstanceOverride ov = new RailModelInstanceOverride();
                            ov.attachments = new ArrayList<>(Collections.singletonList(att));
                            repeater.instanceOverrides.put(idx, ov);
                        }
                    }
                    break;
            }
        }

        repeater.attachments.add(new RepeaterAttachment(modelKey, reversed));
        return repeater;
    }

    // ==================== Serialization: Packet ====================

    public void writePacket(FriendlyByteBuf packet) {
        packet.writeUtf(id);
        packet.writeByte(repeaterMode.ordinal());
        packet.writeFloat(offset);
        packet.writeBoolean(offsetFromStart);
        packet.writeFloat(intervalOverride);
        packet.writeVarInt(manualPositions.size());
        for (float pos : manualPositions) {
            packet.writeFloat(pos);
        }
        packet.writeVarInt(attachments.size());
        for (RepeaterAttachment att : attachments) {
            att.writePacket(packet);
        }
        Map<Integer, RailModelInstanceOverride> nonDefaultOv = new HashMap<>();
        for (Map.Entry<Integer, RailModelInstanceOverride> e : instanceOverrides.entrySet()) {
            if (!e.getValue().isEmpty()) {
                nonDefaultOv.put(e.getKey(), e.getValue());
            }
        }
        packet.writeVarInt(nonDefaultOv.size());
        for (Map.Entry<Integer, RailModelInstanceOverride> e : nonDefaultOv.entrySet()) {
            packet.writeVarInt(e.getKey());
            e.getValue().writePacket(packet);
        }
    }

    public static RailModelRepeater readPacket(FriendlyByteBuf packet) {
        RailModelRepeater repeater = new RailModelRepeater();
        repeater.attachments.clear();
        repeater.id = packet.readUtf();
        repeater.repeaterMode = RepeaterMode.fromIndex(packet.readByte());
        repeater.offset = packet.readFloat();
        repeater.offsetFromStart = packet.readBoolean();
        repeater.intervalOverride = packet.readFloat();
        int posCount = packet.readVarInt();
        List<Float> positions = new ArrayList<>(posCount);
        for (int i = 0; i < posCount; i++) {
            positions.add(packet.readFloat());
        }
        repeater.manualPositions = positions;
        int attCount = packet.readVarInt();
        for (int i = 0; i < attCount; i++) {
            repeater.attachments.add(RepeaterAttachment.readPacket(packet));
        }
        int ovCount = packet.readVarInt();
        for (int i = 0; i < ovCount; i++) {
            int idx = packet.readVarInt();
            RailModelInstanceOverride ov = RailModelInstanceOverride.readPacket(packet);
            if (!ov.isEmpty()) {
                repeater.instanceOverrides.put(idx, ov);
            }
        }
        if (repeater.attachments.isEmpty()) {
            repeater.attachments.add(new RepeaterAttachment());
        }
        return repeater;
    }

    // ==================== Propagation ====================

    private static final Map<UUID, List<UndoEntry>> undoSnapshots = new HashMap<>();

    private record UndoEntry(BlockPos posA, BlockPos posB,
                             List<RailModelRepeater> oldRepeatersAB,
                             List<RailModelRepeater> oldRepeatersBA) {}

    /**
     * Server-side propagation: starting from the rail (railStart -> railEnd),
     * compute the exit offset and propagate forward, only modifying the offset
     * on rails that already have a matching repeater (same getId(), same intervalOverride).
     * Also propagates firstModelIndex for each attachment.
     */
    public static void propagate(RailwayData railwayData, ServerPlayer player,
                                 BlockPos railStart, BlockPos railEnd,
                                 int placementIndex, String repeaterId,
                                 float interval, float initialOffset,
                                 int[] modelCounts, int[] initialFirstModelIndices) {
        ServerLevel level = player.level();
        List<UndoEntry> snapshot = new ArrayList<>();
        List<BlockPos[]> modifiedRails = new ArrayList<>();

        BlockPos entryNode = railStart;
        BlockPos exitNode = railEnd;
        final int MAX_PROPAGATION_STEPS = 1000;

        Rail firstRail = railwayData.getRail(entryNode, exitNode);
        if (firstRail == null) {
            player.sendSystemMessage(Component.literal("No rail found."));
            return;
        }

        snapshotRail(railwayData, snapshot, entryNode, exitNode);
        boolean isCanonical = entryNode.asLong() <= exitNode.asLong();
        applyFixedInterval(railwayData, entryNode, exitNode,
                placementIndex, repeaterId, interval,
                initialOffset, isCanonical, initialFirstModelIndices);
        modifiedRails.add(new BlockPos[]{entryNode, exitNode});

        float currentOffset = computeExitOffset(initialOffset, firstRail.getLength(), interval);
        int positionsPlaced = countPositions(initialOffset, firstRail.getLength(), interval);
        int[] currentFMI = computeExitFMI(initialFirstModelIndices, positionsPlaced, modelCounts);

        for (int step = 1; step < MAX_PROPAGATION_STEPS; step++) {
            Set<BlockPos> connections = railwayData.getRailConnectionsFrom(exitNode);
            connections.remove(entryNode);

            Vec3 exitTangent = computeExitTangent(firstRail);

            List<BlockPos> forwardCandidates = new ArrayList<>();
            boolean hasBackwardConnections = false;
            for (BlockPos candidate : connections) {
                Rail candidateRail = railwayData.getRail(exitNode, candidate);
                if (candidateRail == null) continue;
                Vec3 candidateTangent = computeEntryTangent(candidateRail);
                double dot = exitTangent.x * candidateTangent.x + exitTangent.z * candidateTangent.z;
                if (dot > 0) {
                    forwardCandidates.add(candidate);
                } else {
                    hasBackwardConnections = true;
                }
            }

            if (forwardCandidates.size() != 1 || hasBackwardConnections) {
                String msg = String.format("Propagation stopped at (%d, %d, %d). Exit offset: %.3f. Modified %d rail(s).",
                        exitNode.getX(), exitNode.getY(), exitNode.getZ(),
                        currentOffset, modifiedRails.size());
                if (forwardCandidates.isEmpty()) {
                    msg += " (dead end)";
                } else if (hasBackwardConnections) {
                    msg += " (junction: reverse-side rails present)";
                } else {
                    msg += String.format(" (%d branches)", forwardCandidates.size());
                }
                finishPropagation(level, player, snapshot, modifiedRails, railwayData, msg,
                        exitNode, currentOffset, repeaterId, interval, currentFMI);
                return;
            }

            BlockPos nextNode = forwardCandidates.get(0);
            Rail nextRail = railwayData.getRail(exitNode, nextNode);
            if (nextRail == null) break;

            int matchingIndex = findMatchingRepeater(nextRail, repeaterId, interval);
            if (matchingIndex < 0) {
                String msg = String.format("Propagation stopped at (%d, %d, %d). Exit offset: %.3f. Modified %d rail(s). (no matching repeater on next rail)",
                        exitNode.getX(), exitNode.getY(), exitNode.getZ(),
                        currentOffset, modifiedRails.size());
                finishPropagation(level, player, snapshot, modifiedRails, railwayData, msg,
                        exitNode, currentOffset, repeaterId, interval, currentFMI);
                return;
            }

            snapshotRail(railwayData, snapshot, exitNode, nextNode);
            boolean nextIsCanonical = exitNode.asLong() <= nextNode.asLong();
            setRepeaterOffset(railwayData, exitNode, nextNode, matchingIndex,
                    currentOffset, nextIsCanonical, currentFMI);
            modifiedRails.add(new BlockPos[]{exitNode, nextNode});

            int nextPositions = countPositions(currentOffset, nextRail.getLength(), interval);
            currentOffset = computeExitOffset(currentOffset, nextRail.getLength(), interval);
            currentFMI = computeExitFMI(currentFMI, nextPositions, modelCounts);
            entryNode = exitNode;
            exitNode = nextNode;
            firstRail = nextRail;
        }

        finishPropagation(level, player, snapshot, modifiedRails, railwayData,
                String.format("Propagation complete. Modified %d rail(s).", modifiedRails.size()),
                exitNode, currentOffset, repeaterId, interval, currentFMI);
    }

    public static void undoPropagate(RailwayData railwayData, ServerPlayer player) {
        List<UndoEntry> snapshot = undoSnapshots.remove(player.getUUID());
        if (snapshot == null || snapshot.isEmpty()) {
            player.sendSystemMessage(Component.literal("Nothing to undo."));
            return;
        }
        ServerLevel level = player.level();
        List<BlockPos[]> modifiedRails = new ArrayList<>();
        for (UndoEntry entry : snapshot) {
            Rail railAB = railwayData.getRail(entry.posA, entry.posB);
            if (railAB != null) {
                ((RailExtraSupplier) railAB).setRepeaters(entry.oldRepeatersAB);
            }
            Rail railBA = railwayData.getRail(entry.posB, entry.posA);
            if (railBA != null) {
                ((RailExtraSupplier) railBA).setRepeaters(entry.oldRepeatersBA);
            }
            modifiedRails.add(new BlockPos[]{entry.posA, entry.posB});
        }
        broadcastRailUpdates(level, railwayData, modifiedRails);
        player.sendSystemMessage(Component.literal(String.format("Undone propagation on %d rail(s).", snapshot.size())));
    }

    private static void snapshotRail(RailwayData railwayData, List<UndoEntry> snapshot,
                                     BlockPos posA, BlockPos posB) {
        Rail railAB = railwayData.getRail(posA, posB);
        Rail railBA = railwayData.getRail(posB, posA);
        List<RailModelRepeater> snapAB = copyRepeaterList(railAB);
        List<RailModelRepeater> snapBA = copyRepeaterList(railBA);
        snapshot.add(new UndoEntry(posA, posB, snapAB, snapBA));
    }

    private static List<RailModelRepeater> copyRepeaterList(Rail rail) {
        if (rail == null) return Collections.emptyList();
        List<RailModelRepeater> result = new ArrayList<>();
        for (RailModelRepeater p : ((RailExtraSupplier) rail).getRepeaters()) {
            result.add(p.copy());
        }
        return result;
    }

    private static int findMatchingRepeater(Rail rail, String repeaterId, float interval) {
        List<RailModelRepeater> repeaters = ((RailExtraSupplier) rail).getRepeaters();
        for (int i = 0; i < repeaters.size(); i++) {
            RailModelRepeater p = repeaters.get(i);
            if (p.repeaterMode == RepeaterMode.FIXED_INTERVAL
                    && p.getId().equals(repeaterId)
                    && Math.abs(p.intervalOverride - interval) < 0.001f) {
                return i;
            }
        }
        return -1;
    }

    private static void setRepeaterOffset(RailwayData railwayData,
                                          BlockPos posA, BlockPos posB,
                                          int placementIndex,
                                          float offset, boolean offsetFromStart,
                                          int[] firstModelIndices) {
        Rail railAB = railwayData.getRail(posA, posB);
        Rail railBA = railwayData.getRail(posB, posA);
        if (railAB != null) {
            RailModelRepeater p = ((RailExtraSupplier) railAB).getRepeaters().get(placementIndex);
            p.offset = offset;
            p.offsetFromStart = offsetFromStart;
            applyFMI(p, firstModelIndices);
        }
        if (railBA != null) {
            RailModelRepeater p = ((RailExtraSupplier) railBA).getRepeaters().get(placementIndex);
            p.offset = offset;
            p.offsetFromStart = offsetFromStart;
            applyFMI(p, firstModelIndices);
        }
    }

    private static void applyFMI(RailModelRepeater repeater, int[] firstModelIndices) {
        for (int i = 0; i < Math.min(firstModelIndices.length, repeater.attachments.size()); i++) {
            repeater.attachments.get(i).firstModelIndex = firstModelIndices[i];
        }
    }

    private static void applyFixedInterval(RailwayData railwayData,
                                           BlockPos posStart, BlockPos posEnd,
                                           int placementIndex, String repeaterId,
                                           float interval,
                                           float offset, boolean offsetFromStart,
                                           int[] firstModelIndices) {
        Rail railAB = railwayData.getRail(posStart, posEnd);
        Rail railBA = railwayData.getRail(posEnd, posStart);

        if (railAB != null) {
            ensureRepeaterIndexPresence((RailExtraSupplier) railAB, placementIndex);
            RailModelRepeater p = ((RailExtraSupplier) railAB).getRepeaters().get(placementIndex);
            p.repeaterMode = RepeaterMode.FIXED_INTERVAL;
            p.offset = offset;
            p.offsetFromStart = offsetFromStart;
            p.intervalOverride = interval > 0 ? interval : 0;
            applyFMI(p, firstModelIndices);
        }
        if (railBA != null) {
            ensureRepeaterIndexPresence((RailExtraSupplier) railBA, placementIndex);
            RailModelRepeater p = ((RailExtraSupplier) railBA).getRepeaters().get(placementIndex);
            p.repeaterMode = RepeaterMode.FIXED_INTERVAL;
            p.offset = offset;
            p.offsetFromStart = offsetFromStart;
            p.intervalOverride = interval > 0 ? interval : 0;
            applyFMI(p, firstModelIndices);
        }
    }

    private static void ensureRepeaterIndexPresence(RailExtraSupplier supplier, int index) {
        List<RailModelRepeater> repeaters = supplier.getRepeaters();
        while (repeaters.size() <= index) {
            repeaters.add(new RailModelRepeater());
        }
    }

    private static void finishPropagation(ServerLevel level, ServerPlayer player,
                                          List<UndoEntry> snapshot,
                                          List<BlockPos[]> modifiedRails,
                                          RailwayData railwayData, String message,
                                          BlockPos terminalNode, float exitOffset,
                                          String repeaterId, float interval,
                                          int[] exitFMI) {
        undoSnapshots.put(player.getUUID(), snapshot);
        broadcastRailUpdates(level, railwayData, modifiedRails);
        player.sendSystemMessage(Component.literal(message));

        final FriendlyByteBuf resultPacket = new FriendlyByteBuf(Unpooled.buffer());
        resultPacket.writeBlockPos(terminalNode);
        resultPacket.writeFloat(exitOffset);
        resultPacket.writeUtf(repeaterId);
        resultPacket.writeFloat(interval);
        resultPacket.writeVarInt(exitFMI.length);
        for (int fmi : exitFMI) {
            resultPacket.writeVarInt(fmi);
        }
        Registry.sendToPlayer(player, IPacket.PACKET_PROPAGATE_REPEATER_RESULT, resultPacket);
    }

    private static void broadcastRailUpdates(ServerLevel level, RailwayData railwayData,
                                             List<BlockPos[]> modifiedRails) {
        for (BlockPos[] pair : modifiedRails) {
            Rail railAB = railwayData.getRail(pair[0], pair[1]);
            Rail railBA = railwayData.getRail(pair[1], pair[0]);
            if (railAB == null || railBA == null) continue;

            final FriendlyByteBuf outbound = new FriendlyByteBuf(Unpooled.buffer());
            outbound.writeUtf(railAB.transportMode.toString());
            outbound.writeBlockPos(pair[0]);
            outbound.writeBlockPos(pair[1]);
            railAB.writePacket(outbound);
            railBA.writePacket(outbound);
            outbound.writeLong(0);

            for (ServerPlayer levelPlayer : level.players()) {
                Registry.sendToPlayer(levelPlayer, IPacket.PACKET_CREATE_RAIL, outbound);
            }
        }
    }

    /** Count how many positions are placed given offset, rail length, and interval. */
    static int countPositions(float offset, double railLength, float interval) {
        int count = 0;
        for (double t = offset; t < railLength - 0.001; t += interval) {
            count++;
        }
        return count;
    }

    /**
     * O_next = (I - (L - O) % I) % I, with special case for O >= L.
     */
    static float computeExitOffset(float offset, double railLength, float interval) {
        if (offset >= railLength) {
            return offset - (float) railLength;
        }
        double remainder = (railLength - offset) % interval;
        return (float) ((interval - remainder) % interval);
    }

    static int[] computeExitFMI(int[] currentFMI, int positionsPlaced, int[] modelCounts) {
        int[] result = new int[currentFMI.length];
        for (int i = 0; i < currentFMI.length; i++) {
            if (modelCounts[i] > 0) {
                result[i] = (currentFMI[i] + positionsPlaced) % modelCounts[i];
            } else {
                result[i] = 0;
            }
        }
        return result;
    }

    /**
     * Tangent at the exit node (t=L end), pointing forward (away from the rail).
     */
    private static Vec3 computeExitTangent(Rail rail) {
        return new Vec3(-rail.facingEnd.cos, 0, -rail.facingEnd.sin);
    }

    /**
     * Tangent at the entry node (t=0 end) of the rail.
     */
    private static Vec3 computeEntryTangent(Rail rail) {
        return new Vec3(rail.facingStart.cos, 0, rail.facingStart.sin);
    }
}
