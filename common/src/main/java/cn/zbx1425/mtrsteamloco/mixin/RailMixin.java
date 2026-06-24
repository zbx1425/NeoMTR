package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import cn.zbx1425.mtrsteamloco.data.RailModelRepeater;
import io.netty.buffer.Unpooled;
import mtr.data.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.Value;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.*;

@Mixin(value = Rail.class, priority = 1425)
public abstract class RailMixin implements RailExtraSupplier {

    @Unique
    private List<RailModelRepeater> mtrnte$repeaters = new ArrayList<>(Collections.singletonList(new RailModelRepeater()));
    private float verticalCurveRadius = 0f;
    private boolean isSecondaryDir = false;

    @Override
    public boolean getIsSecondaryDir() {
        return isSecondaryDir;
    }

    @Override
    public void setIsSecondaryDir(boolean value) {
        this.isSecondaryDir = value;
        dataBytes = null;
    }

    @Override
    public float getVerticalCurveRadius() {
        return verticalCurveRadius;
    }

    @Override
    public void setVerticalCurveRadius(float value) {
        this.verticalCurveRadius = value;
    }

    @Override
    public int getHeight() {
        return yEnd - yStart;
    }

    @Override
    public List<RailModelRepeater> getRepeaters() {
        return mtrnte$repeaters;
    }

    @Override
    public void setRepeaters(List<RailModelRepeater> repeaters) {
        if(repeaters == null) throw new IllegalStateException("Cannot be null!");
        this.mtrnte$repeaters = new ArrayList<>(repeaters);
        dataBytes = null;
    }

    @Inject(method = "<init>(Ljava/util/Map;)V", at = @At("TAIL"), remap = false)
    private void fromMessagePack(Map<String, Value> map, CallbackInfo ci) {
        MessagePackHelper messagePackHelper = new MessagePackHelper(map);
        verticalCurveRadius = messagePackHelper.getFloat("vertical_curve_radius", 0);
        isSecondaryDir = messagePackHelper.getBoolean("is_secondary_dir", false);

        if (map.containsKey("repeaters")) {
            ArrayValue arr = map.get("repeaters").asArrayValue();
            mtrnte$repeaters = new ArrayList<>(arr.size());
            for (Value v : arr) {
                mtrnte$repeaters.add(RailModelRepeater.fromMessagePack(v.asMapValue()));
            }
        } else {
            String legacyModelKey = messagePackHelper.getString("model_key", "");
            mtrnte$repeaters = new ArrayList<>(Collections.singletonList(new RailModelRepeater(legacyModelKey, false)));
        }
    }

    @Inject(method = "toMessagePack", at = @At("TAIL"), remap = false)
    private void toMessagePack(MessagePacker messagePacker, CallbackInfo ci) throws IOException {
        messagePacker.packString("vertical_curve_radius").packFloat(verticalCurveRadius);

        if (mtrnte$repeaters.size() == 1 && mtrnte$repeaters.getFirst().isLegacyCompatible()) {
            messagePacker.packString("model_key").packString(mtrnte$repeaters.getFirst().getPrimaryModelTypeKey());
            messagePacker.packString("is_secondary_dir").packBoolean(isSecondaryDir);
        } else {
            messagePacker.packString("model_key").packString(mtrnte$repeaters.isEmpty()
                ? "null" : mtrnte$repeaters.getFirst().getPrimaryModelTypeKey());
            messagePacker.packString("is_secondary_dir").packBoolean(isSecondaryDir);
            messagePacker.packString("repeaters").packArrayHeader(mtrnte$repeaters.size());
            for (RailModelRepeater repeater : mtrnte$repeaters) {
                repeater.toMessagePack(messagePacker);
            }
        }
    }

    @Inject(method = "messagePackLength", at = @At("TAIL"), cancellable = true, remap = false)
    private void messagePackLength(CallbackInfoReturnable<Integer> cir) {
        if (mtrnte$repeaters.size() == 1 && mtrnte$repeaters.getFirst().isLegacyCompatible()) {
            cir.setReturnValue(cir.getReturnValue() + 3);
        } else {
            cir.setReturnValue(cir.getReturnValue() + 4);
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
    private void fromPacket(FriendlyByteBuf packet, CallbackInfo ci) {
        if (!Main.enableRegistry) return;
        verticalCurveRadius = packet.readFloat();
        isSecondaryDir = packet.readBoolean();
        int count = packet.readVarInt();
        mtrnte$repeaters = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            mtrnte$repeaters.add(RailModelRepeater.readPacket(packet));
        }
    }

    @Inject(method = "writePacket", at = @At("TAIL"))
    private void toPacket(FriendlyByteBuf packet, CallbackInfo ci) {
        if (!Main.enableRegistry) return;
        if(mtrnte$repeaters == null) { // TODO: This shouldn't be null...
            mtrnte$repeaters = new ArrayList<>(Collections.singletonList(new RailModelRepeater()));
        }
        packet.writeFloat(verticalCurveRadius);
        packet.writeBoolean(isSecondaryDir);
        packet.writeVarInt(mtrnte$repeaters.size());
        for (RailModelRepeater repeater : mtrnte$repeaters) {
            repeater.writePacket(packet);
        }
    }

//    @Redirect(method = "renderSegment", remap = false, at = @At(value = "INVOKE", target = "Ljava/lang/Math;round(D)J"))
//    private long redirectRenderSegmentRound(double r) {
//        if (ClientConfig.getRailRenderLevel() < 2) return Math.round(r);
//
//        Rail instance = (Rail)(Object)this;
//        if (instance.railType == RailType.NONE) {
//            return Math.round(r);
//        } else {
//            return Math.round(r / RailModelRegistry.getProperty(RailRenderDispatcher.getModelKeyForRender(instance)).repeatInterval);
//        }
//    }

    @Shadow @Final private int yStart, yEnd;

    private float vTheta;

    @Inject(method = "getPositionY", at = @At("HEAD"), cancellable = true, remap = false)
    private void getPositionY(double rawValue, CallbackInfoReturnable<Double> cir) {
        if (((Rail)(Object)this).railType.railSlopeStyle == RailType.RailSlopeStyle.CABLE) return;
        double H = Math.abs(yEnd - yStart);
        double L = ((Rail)(Object)this).getLength();
        int sign = yStart < yEnd ? 1 : -1;
        double maxRadius = (H == 0) ? 0 : Math.abs((H * H + L * L) / (H * 4));
        if (verticalCurveRadius < 0) {
            // Magic value for a flat rail
            cir.setReturnValue(sign * ((rawValue / L) * H) + yStart);
        } else if (verticalCurveRadius == 0 || verticalCurveRadius > maxRadius) {
            // Magic default value / impossible radius, fallback to MTR all curvy track
        } else {
            if (vTheta == 0) vTheta = RailExtraSupplier.getVTheta((Rail)(Object)this, verticalCurveRadius);
            if (!Double.isFinite(vTheta)) return;
            float curveL = Mth.sin(vTheta) * verticalCurveRadius;
            float curveH = (1 - Mth.cos(vTheta)) * verticalCurveRadius;
            if (rawValue < curveL) {
                float r = (float)rawValue;
                cir.setReturnValue(sign * (verticalCurveRadius - Math.sqrt(verticalCurveRadius * verticalCurveRadius - r * r)) + yStart);
            } else if (rawValue > L - curveL) {
                float r = (float)(L - rawValue);
                cir.setReturnValue(-sign * (verticalCurveRadius - Math.sqrt(verticalCurveRadius * verticalCurveRadius - r * r)) + yEnd);
            } else {
                cir.setReturnValue(sign * (((rawValue - curveL) / (L - 2 * curveL)) * (H - 2 * curveH) + curveH) + yStart);
            }
        }
    }

    private static final FriendlyByteBuf hashBuilder = new FriendlyByteBuf(Unpooled.buffer());
    private byte[] dataBytes;
    private int hashCode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (dataBytes == null) createDataBytes();
        if (((RailMixin)o).dataBytes == null) ((RailMixin)o).createDataBytes();
        return Arrays.equals(dataBytes, ((RailMixin)o).dataBytes);
    }

    @Override
    public int hashCode() {
        if (dataBytes == null) createDataBytes();
        return hashCode;
    }

    private void createDataBytes() {
        hashBuilder.clear();
        ((Rail)(Object)this).writePacket(hashBuilder);
        dataBytes = new byte[hashBuilder.writerIndex()];
        hashBuilder.getBytes(0, dataBytes);
        hashCode = Arrays.hashCode(dataBytes);
    }

}
