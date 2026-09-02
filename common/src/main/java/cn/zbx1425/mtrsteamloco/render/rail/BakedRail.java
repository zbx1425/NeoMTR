package cn.zbx1425.mtrsteamloco.render.rail;

import cn.zbx1425.mtrsteamloco.data.*;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.util.AttrUtil;
import mtr.data.Rail;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class BakedRail {

    public record TransformOnBoundary(long dedupHash, Matrix4f matrix) {}

    public Map<ModelRef, HashMap<Long, ArrayList<Matrix4f>>> interiorModelsByChunks = new HashMap<>();
    public Map<ModelRef, HashMap<Long, ArrayList<TransformOnBoundary>>> boundaryModelsByChunks = new HashMap<>();

    public static final int POS_SHIFT = 1;

    public int color;

    public BakedRail(Rail rail, BlockPos posStart, BlockPos posEnd) {
        color = AttrUtil.argbToBgr(rail.railType.color | 0xFF000000);
        boolean isCanonical = posStart.asLong() <= posEnd.asLong();
        BlockPos canonStart = isCanonical ? posStart : posEnd;
        BlockPos canonEnd = isCanonical ? posEnd : posStart;

        boolean isSecondaryDir = ((RailExtraSupplier) rail).getIsSecondaryDir();
        List<RailModelRepeater> repeaters = ((RailExtraSupplier) rail).getRepeaters();
        double railLength = rail.getLength();

        for (RailModelRepeater repeater : repeaters) {
            if (repeater.attachments.isEmpty()) continue;

            // Resolve interval from first attachment's model type
            String primaryTypeKey = RailRenderDispatcher.getModelKeyForRender(rail, repeater.getPrimaryModelTypeKey());
            if (primaryTypeKey.equals("null") || primaryTypeKey.isEmpty()) continue;

            RailModelProperties primaryProps = RailModelRegistry.getProperty(primaryTypeKey);
            float interval = repeater.resolveInterval(primaryProps);

            PositionResult posResult = computePositions(
                    repeater, railLength, interval, canonStart, canonEnd);

            double chordHalfSpan = switch (repeater.repeaterMode) {
                case STRETCH_INTERVAL -> {
                    int N = Math.max(2, Math.round((float) (railLength / interval)) + 1);
                    yield (railLength / (N - 1)) / 2.0;
                }
                case FIXED_INTERVAL -> interval / 2.0;
                case MANUAL -> 0;
            };

            for (IndexedPosition ip : posResult.interior) {
                RailModelInstanceOverride ipOv = repeater.instanceOverrides.get(ip.originalIndex);
                if (ipOv != null && ipOv.suppressed) continue;
                List<RepeaterAttachment> effectiveAttachments = resolveAttachments(repeater, ip.originalIndex);
                placeAttachments(rail, ip.tCanon, isCanonical, isSecondaryDir, railLength,
                        chordHalfSpan, interval, effectiveAttachments,
                        ip.originalIndex, repeater.offsetFromStart, true, 0, canonStart);
            }
            for (BoundaryPosition bp : posResult.boundary) {
                RailModelInstanceOverride bpOv = repeater.instanceOverrides.get(bp.originalIndex);
                if (bpOv != null && bpOv.suppressed) continue;
                List<RepeaterAttachment> effectiveAttachments = resolveAttachments(repeater, bp.originalIndex);
                placeAttachments(rail, bp.tCanon, isCanonical, isSecondaryDir, railLength,
                        chordHalfSpan, interval, effectiveAttachments,
                        bp.originalIndex, repeater.offsetFromStart, false, bp.blockPosHash, canonStart);
            }
        }
    }

    private List<RepeaterAttachment> resolveAttachments(RailModelRepeater repeater, int positionIndex) {
        RailModelInstanceOverride override = repeater.instanceOverrides.get(positionIndex);
        if (override != null && !override.isEmpty()) return override.attachments;
        return repeater.attachments;
    }

    private void placeAttachments(Rail rail, double tCanon, boolean isCanonical, boolean isSecondaryDir,
                                  double railLength, double chordHalfSpan, float interval,
                                  List<RepeaterAttachment> attachments,
                                  int placementIndex, boolean offsetFromStart,
                                  boolean isInterior, long blockPosHash, BlockPos canonStart) {
        for (RepeaterAttachment attachment : attachments) {
            String resolvedTypeKey = RailRenderDispatcher.getModelKeyForRender(rail, attachment.modelTypeKey);
            if (resolvedTypeKey.equals("null") || resolvedTypeKey.isEmpty()) continue;

            RailModelProperties props = RailModelRegistry.getProperty(resolvedTypeKey);
            if (props.getModelCount() == 0) continue;

            int modelIndex = computeModelIndex(attachment, placementIndex, props.getModelCount());

            boolean effectiveReversed = isSecondaryDir ^ offsetFromStart ^ attachment.reversed;

            ModelRef modelRef = new ModelRef(resolvedTypeKey, modelIndex);

            Matrix4f mat = computeMatrix(rail, tCanon, isCanonical, railLength,
                    props, chordHalfSpan, attachment, offsetFromStart, effectiveReversed);

            if (isInterior) {
                double tLocal = isCanonical ? tCanon : (railLength - tCanon);
                Vec3 pos = rail.getPosition(tLocal);
                long chunkId = chunkIdFromWorldPos(Mth.floor((float) pos.x), Mth.floor((float) pos.z));
                HashMap<Long, ArrayList<Matrix4f>> chunks =
                        interiorModelsByChunks.computeIfAbsent(modelRef, k -> new HashMap<>());
                chunks.computeIfAbsent(chunkId, ignored -> new ArrayList<>()).add(mat);
            } else {
                double tLocal = isCanonical ? tCanon : (railLength - tCanon);
                Vec3 pos = rail.getPosition(tLocal);
                long chunkId = chunkIdFromWorldPos(Mth.floor((float) pos.x), Mth.floor((float) pos.z));
                HashMap<Long, ArrayList<TransformOnBoundary>> nChunks =
                        boundaryModelsByChunks.computeIfAbsent(modelRef, k -> new HashMap<>());
                long dedupHash = blockPosHash ^ (attachment.reversed ? Long.MIN_VALUE : 0)
                        ^ ((long) attachment.modelTypeKey.hashCode() << 16);
                nChunks.computeIfAbsent(chunkId, ignored -> new ArrayList<>())
                        .add(new TransformOnBoundary(dedupHash, mat));
            }
        }
    }

    private int computeModelIndex(RepeaterAttachment attachment, int placementIndex, int modelCount) {
        int fmi = attachment.firstModelIndex % modelCount;
        return (fmi + placementIndex) % modelCount;
    }

    private record IndexedPosition(double tCanon, int originalIndex) {}
    private record BoundaryPosition(double tCanon, long blockPosHash, int originalIndex) {}
    private record PositionResult(List<IndexedPosition> interior, List<BoundaryPosition> boundary) {}

    private Matrix4f computeMatrix(Rail rail, double tCanon, boolean isCanonical,
                                   double railLength, RailModelProperties props,
                                   double chordHalfSpan, RepeaterAttachment attachment,
                                   boolean offsetFromStart, boolean effectiveReversed) {
        double tLocal = isCanonical ? tCanon : (railLength - tCanon);
        Vec3 pos = rail.getPosition(tLocal);

        double tA, tB;
        if (chordHalfSpan > 0) {
            tA = Math.max(0, tLocal - chordHalfSpan);
            tB = Math.min(railLength, tLocal + chordHalfSpan);
        } else {
            if (tLocal + 0.01 <= railLength) {
                tA = tLocal;
                tB = tLocal + 0.01;
            } else if (tLocal - 0.01 >= 0) {
                tA = tLocal - 0.01;
                tB = tLocal;
            } else {
                tA = 0;
                tB = railLength;
            }
        }
        Vec3 pA = rail.getPosition(tA);
        Vec3 pB = rail.getPosition(tB);

        float xc = (float) pos.x;
        float yc = (float) pos.y + props.yOffset;
        float zc = (float) pos.z;
        float xf = xc + (float)(pB.x - pA.x);
        float yf = yc + (props.tiltToGradient ? (float)(pB.y - pA.y) : 0);
        float zf = zc + (float)(pB.z - pA.z);

        // Offset is applied in the offsetFromStart-aligned coordinate system.
        // Tangent (pB-pA) points posStart→posEnd = canonical direction iff isCanonical.
        // Need to flip tangent to get offsetFromStart direction when they differ.
        boolean needFlipForOffset = isCanonical != offsetFromStart;

        Matrix4f mat = getLookAtMat(xc, yc, zc, xf, yf, zf, needFlipForOffset);
        if (attachment.offsetX != 0 || attachment.offsetY != 0 || attachment.offsetZ != 0) {
            mat.translate(attachment.offsetX, attachment.offsetY, attachment.offsetZ);
        }

        // Additional rotation to reach the final model orientation.
        // Uses identity: RotY(a) * RotX(p) * RotY(π) = RotY(a+π) * RotX(-p)
        boolean needAdditionalPi = effectiveReversed != needFlipForOffset;
        if (needAdditionalPi) {
            mat.rotateY((float) Math.PI);
        }
        return mat;
    }

    private static final double BOUNDARY_EPSILON = 0.01;

    private PositionResult computePositions(RailModelRepeater p, double L, float I,
                                            BlockPos canonStart, BlockPos canonEnd) {
        List<IndexedPosition> interior = new ArrayList<>();
        List<BoundaryPosition> boundary = new ArrayList<>();

        switch (p.repeaterMode) {
            case STRETCH_INTERVAL: {
                if (L < I * 0.5) {
                    interior.add(new IndexedPosition(L / 2, 0));
                    break;
                }
                int N = Math.max(2, Math.round((float) (L / I)) + 1);
                double actualI = L / (N - 1);
                for (int k = 0; k < N; k++) {
                    int placementIdx = p.offsetFromStart ? k : (N - 1 - k);
                    double t = k * actualI;
                    if (k == 0) {
                        boundary.add(new BoundaryPosition(t, canonStart.asLong(), placementIdx));
                    } else if (k == N - 1) {
                        boundary.add(new BoundaryPosition(t, canonEnd.asLong(), placementIdx));
                    } else {
                        interior.add(new IndexedPosition(t, placementIdx));
                    }
                }
                break;
            }
            case FIXED_INTERVAL: {
                List<double[]> rawPositions = new ArrayList<>();
                if (p.offsetFromStart) {
                    for (double t = p.offset; t < L - 0.001; t += I) {
                        rawPositions.add(new double[]{t});
                    }
                } else {
                    for (double t = L - p.offset; t > 0.001; t -= I) {
                        rawPositions.add(new double[]{t});
                    }
                    Collections.reverse(rawPositions);
                }
                int unclippedCount = rawPositions.size();

                double canonMin = 0, canonMax = L;
                if (p.rangeStart >= 0 || p.rangeEnd >= 0) {
                    float effStart = (p.rangeStart < 0) ? 0 : p.rangeStart;
                    float effEnd = (p.rangeEnd < 0) ? (float) L : Math.min(p.rangeEnd, (float) L);
                    if (p.offsetFromStart) {
                        canonMin = effStart; canonMax = effEnd;
                    } else {
                        canonMin = L - effEnd; canonMax = L - effStart;
                    }
                }
                for (int i = 0; i < rawPositions.size(); i++) {
                    double t = rawPositions.get(i)[0];
                    if (t < canonMin - 0.001 || t > canonMax + 0.001) continue;
                    int placementIdx = p.offsetFromStart ? i : (unclippedCount - 1 - i);
                    classifyPosition(t, placementIdx, L, canonStart, canonEnd, interior, boundary);
                }
                break;
            }
            case MANUAL: {
                for (int i = 0; i < p.manualPositions.size(); i++) {
                    float placementT = p.manualPositions.get(i);
                    double canonT = p.offsetFromStart ? placementT : (L - placementT);
                    if (canonT >= 0 && canonT <= L) {
                        classifyPosition(canonT, i, L, canonStart, canonEnd, interior, boundary);
                    }
                }
                break;
            }
        }
        return new PositionResult(interior, boundary);
    }

    private static void classifyPosition(double t, int idx, double L,
                                         BlockPos canonStart, BlockPos canonEnd,
                                         List<IndexedPosition> interior,
                                         List<BoundaryPosition> boundary) {
        if (t < BOUNDARY_EPSILON) {
            boundary.add(new BoundaryPosition(t, canonStart.asLong(), idx));
        } else if (t > L - BOUNDARY_EPSILON) {
            boundary.add(new BoundaryPosition(t, canonEnd.asLong(), idx));
        } else {
            interior.add(new IndexedPosition(t, idx));
        }
    }

    public static long chunkIdFromWorldPos(float bpX, float bpZ) {
        return ((long) ((int) bpX >> (4 + POS_SHIFT)) << 32) | ((long) ((int) bpZ >> (4 + POS_SHIFT)) & 0xFFFFFFFFL);
    }

    public static long chunkIdFromSectPos(int spX, int spZ) {
        return ((long) (spX >> POS_SHIFT) << 32) | ((long) (spZ >> POS_SHIFT) & 0xFFFFFFFFL);
    }

    public static Matrix4f getLookAtMat(float posX, float posY, float posZ, float tgX, float tgY, float tgZ, boolean reverse) {
        Matrix4f matrix4f = Matrix4f.translation(posX, posY, posZ);

        float dx = tgX - posX;
        float dy = tgY - posY;
        float dz = tgZ - posZ;
        float hDist = (float) Math.sqrt(dx * dx + dz * dz);
        final float yaw = (float) Mth.atan2(dx, dz);
        final float pitch = (float) Mth.atan2(dy, Math.max(hDist, 0.0001f));

        matrix4f.rotateY((reverse ? (float) Math.PI : 0f) + yaw);
        matrix4f.rotateX(reverse ? pitch : -pitch);

        return matrix4f;
    }
}
