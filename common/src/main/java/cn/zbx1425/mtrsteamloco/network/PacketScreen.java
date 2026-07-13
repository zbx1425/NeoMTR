package cn.zbx1425.mtrsteamloco.network;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.gui.RailEditorGeometryScreen;
import cn.zbx1425.mtrsteamloco.gui.RailEditorVisualScreen;
import cn.zbx1425.mtrsteamloco.gui.EyeCandyScreen;
import io.netty.buffer.Unpooled;
import mtr.Registry;
import mtr.util.UtilitiesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class PacketScreen {

    public static Identifier PACKET_SHOW_SCREEN = Main.id("show_screen");

    public static void sendScreenBlockS2C(ServerPlayer player, String screenName, BlockPos pos) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeUtf(screenName);
        packet.writeBlockPos(pos);
        Registry.sendToPlayer(player, PACKET_SHOW_SCREEN, packet);
    }

    public static void receiveScreenS2C(FriendlyByteBuf packet) {
        MakeClassLoaderHappy.receiveScreenS2C(packet);
    }

    private static class MakeClassLoaderHappy {
        public static void receiveScreenS2C(FriendlyByteBuf packet) {
            Minecraft minecraftClient = Minecraft.getInstance();
            String screenName = packet.readUtf();
            BlockPos pos = packet.readBlockPos();
            minecraftClient.execute(() -> {
                switch (screenName) {
                    case "eye_candy":
                        UtilitiesClient.setScreen(minecraftClient, new EyeCandyScreen(pos));
                        break;
                    case "rail_editor_visual":
                        UtilitiesClient.setScreen(minecraftClient, new RailEditorVisualScreen());
                        break;
                    case "rail_editor_geometry":
                        RailEditorGeometryScreen.acquirePickInfoWhenUse(pos);
                        UtilitiesClient.setScreen(minecraftClient, new RailEditorGeometryScreen());
                        break;
                }
            });
        }
    }
}
