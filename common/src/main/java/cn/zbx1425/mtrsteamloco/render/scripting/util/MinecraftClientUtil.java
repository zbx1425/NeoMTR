package cn.zbx1425.mtrsteamloco.render.scripting.util;

import cn.zbx1425.sowcer.math.Vector3f;
import com.mojang.text2speech.Narrator;
import mtr.mappings.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class MinecraftClientUtil {

    public static boolean worldIsRaining() {
        return Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.isRaining();
    }

    public static boolean worldIsRainingAt(Vector3f pos) {
        return Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.isRainingAt(pos.toBlockPos());
    }

    public static int worldDayTime() {
        return Minecraft.getInstance().level != null
                ? (int) Minecraft.getInstance().level.getOverworldClockTime() : 0;
    }

    public static void narrate(String message) {
        Minecraft.getInstance().execute(() -> {
            Narrator.getNarrator().say(message, true, 1.0f);
        });
    }

    public static void displayMessage(String message, boolean actionBar) {
        final Player player = Minecraft.getInstance().player;
        if (player != null) {
            Minecraft.getInstance().execute(() -> {
                if(actionBar) {
                    player.sendOverlayMessage(Text.literal(message));
                } else {
                    player.sendSystemMessage(Text.literal(message));
                }
            });
        }
    }
}
