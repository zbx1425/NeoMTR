package cn.zbx1425.mtrsteamloco.data;

import net.minecraft.network.FriendlyByteBuf;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RailModelInstanceOverride {

    public List<RepeaterAttachment> attachments;
    public boolean suppressed;

    public RailModelInstanceOverride() {
        this.attachments = null;
        this.suppressed = false;
    }

    public boolean isEmpty() {
        return !suppressed && (attachments == null || attachments.isEmpty());
    }

    public RailModelInstanceOverride copy() {
        RailModelInstanceOverride c = new RailModelInstanceOverride();
        c.suppressed = this.suppressed;
        if (this.attachments != null) {
            c.attachments = new ArrayList<>(this.attachments.size());
            for (RepeaterAttachment att : this.attachments) {
                c.attachments.add(att.copy());
            }
        }
        return c;
    }

    public void toMessagePack(MessagePacker packer) throws IOException {
        int fieldCount = 0;
        if (suppressed) fieldCount++;
        if (attachments != null) fieldCount++;
        packer.packMapHeader(fieldCount);
        if (suppressed) {
            packer.packString("sup").packBoolean(true);
        }
        if (attachments != null) {
            packer.packString("att").packArrayHeader(attachments.size());
            for (RepeaterAttachment att : attachments) {
                att.toMessagePack(packer);
            }
        }
    }

    public static RailModelInstanceOverride fromMessagePack(MapValue mapValue) {
        RailModelInstanceOverride ov = new RailModelInstanceOverride();
        for (Map.Entry<Value, Value> entry : mapValue.map().entrySet()) {
            String key = entry.getKey().asStringValue().asString();
            Value val = entry.getValue();
            switch (key) {
                case "sup":
                    ov.suppressed = val.asBooleanValue().getBoolean();
                    break;
                case "att":
                    ArrayValue arr = val.asArrayValue();
                    ov.attachments = new ArrayList<>(arr.size());
                    for (Value v : arr) {
                        ov.attachments.add(RepeaterAttachment.fromMessagePack(v.asMapValue()));
                    }
                    break;
            }
        }
        return ov;
    }

    public void writePacket(FriendlyByteBuf packet) {
        packet.writeBoolean(suppressed);
        if (attachments != null) {
            packet.writeBoolean(true);
            packet.writeVarInt(attachments.size());
            for (RepeaterAttachment att : attachments) {
                att.writePacket(packet);
            }
        } else {
            packet.writeBoolean(false);
        }
    }

    public static RailModelInstanceOverride readPacket(FriendlyByteBuf packet) {
        RailModelInstanceOverride ov = new RailModelInstanceOverride();
        ov.suppressed = packet.readBoolean();
        if (packet.readBoolean()) {
            int count = packet.readVarInt();
            ov.attachments = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ov.attachments.add(RepeaterAttachment.readPacket(packet));
            }
        }
        return ov;
    }
}
