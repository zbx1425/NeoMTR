package cn.zbx1425.mtrsteamloco.network;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import cn.zbx1425.mtrsteamloco.data.RailModelRepeater;
import cn.zbx1425.mtrsteamloco.data.RepeaterAttachment;
import cn.zbx1425.mtrsteamloco.mixin.RailwayDataAccessor;
import io.netty.buffer.Unpooled;
import mtr.Registry;
import mtr.RegistryClient;
import mtr.data.Rail;
import mtr.data.RailwayData;
import mtr.packet.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PacketUpdateRail {

    public static Identifier PACKET_UPDATE_RAIL = Main.id("update_rail");

    public static void sendUpdateC2S(Rail newState, BlockPos posStart, BlockPos posEnd) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeIdentifier(Minecraft.getInstance().level.dimension().identifier());
        packet.writeBlockPos(posStart);
        packet.writeBlockPos(posEnd);
        newState.writePacket(packet);

        RegistryClient.sendToServer(PACKET_UPDATE_RAIL, packet);
    }

    public static void receiveUpdateC2S(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet) {
        ResourceKey<Level> levelKey = packet.readResourceKey(net.minecraft.core.registries.Registries.DIMENSION);
        BlockPos posStart = packet.readBlockPos();
        BlockPos posEnd = packet.readBlockPos();
        RailExtraSupplier extraTarget = (RailExtraSupplier)(new Rail(packet));
        server.execute(() -> {
            ServerLevel level = server.getLevel(levelKey);
            if (level == null) return;

            RailwayData railwayData = RailwayData.getInstance(level);
            Map<BlockPos, Map<BlockPos, Rail>> rails = ((RailwayDataAccessor)railwayData).getRails();
            Rail railForward = rails.get(posStart).get(posEnd);
            Rail railBackward = rails.get(posEnd).get(posStart);
            if (railForward == null || railBackward == null) return;
            RailExtraSupplier extraForward = (RailExtraSupplier) railForward;
            RailExtraSupplier extraBackward = (RailExtraSupplier) railBackward;

            List<String> oldData = serializeRailExtra(extraForward);

            List<RailModelRepeater> repeaters = new ArrayList<>();
            for (RailModelRepeater p : extraTarget.getRepeaters()) {
                repeaters.add(p.copy());
            }
            extraForward.setRepeaters(repeaters);
            extraBackward.setRepeaters(repeaters);
            extraForward.setVerticalCurveRadius(extraTarget.getVerticalCurveRadius());
            extraBackward.setVerticalCurveRadius(extraTarget.getVerticalCurveRadius());

            List<String> newData = serializeRailExtra(extraForward);
            railwayData.railwayDataLoggingModule.addEvent(player, Rail.class, oldData, newData, posStart, posEnd);

            final FriendlyByteBuf outboundPacket = new FriendlyByteBuf(Unpooled.buffer());
            outboundPacket.writeUtf(railForward.transportMode.toString());
            outboundPacket.writeBlockPos(posStart);
            outboundPacket.writeBlockPos(posEnd);
            railForward.writePacket(outboundPacket);
            railBackward.writePacket(outboundPacket);
            outboundPacket.writeLong(0); // We're actually updating instead of creating, so don't create saved rail

            for (ServerPlayer levelPlayer : level.players()) {
                Registry.sendToPlayer(levelPlayer, IPacket.PACKET_CREATE_RAIL, outboundPacket);
            }
        });
    }

    static List<String> serializeRailExtra(RailExtraSupplier extra) {
        List<String> data = new ArrayList<>();
        data.add("verticalCurveRadius:" + extra.getVerticalCurveRadius());
        List<RailModelRepeater> repeaters = extra.getRepeaters();
        data.add("repeaterCount:" + repeaters.size());
        for (int i = 0; i < repeaters.size(); i++) {
            RailModelRepeater r = repeaters.get(i);
            String p = "repeater_" + i + "_";
            data.add(p + "id:\"" + r.getId() + "\"");
            data.add(p + "mode:" + r.repeaterMode);
            data.add(p + "intervalOverride:" + r.intervalOverride);
            data.add(p + "offset:" + r.offset);
            data.add(p + "offsetFromStart:" + r.offsetFromStart);
            data.add(p + "rangeStart:" + r.rangeStart);
            data.add(p + "rangeEnd:" + r.rangeEnd);
            data.add(p + "manualPositionCount:" + r.manualPositions.size());
            data.add(p + "overrideCount:" + r.instanceOverrides.size());
            for (int j = 0; j < r.attachments.size(); j++) {
                RepeaterAttachment att = r.attachments.get(j);
                String ap = p + "att_" + j + "_";
                data.add(ap + "modelTypeKey:\"" + att.modelTypeKey + "\"");
                data.add(ap + "reversed:" + att.reversed);
                data.add(ap + "offsetX:" + att.offsetX);
                data.add(ap + "offsetY:" + att.offsetY);
                data.add(ap + "offsetZ:" + att.offsetZ);
                data.add(ap + "firstModelIndex:" + att.firstModelIndex);
            }
        }
        return data;
    }
}
