package cn.zbx1425.mtrsteamloco.gui;

import cn.zbx1425.mtrsteamloco.MainClient;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugHud implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        displayer.addLine(
                "[NTE] Calls: " + MainClient.drawContext.drawCallCount
                        + ", Batches: " + MainClient.drawContext.batchCount
                        + ", Faces: " + (MainClient.drawContext.singleFaceCount + MainClient.drawContext.instancedFaceCount)
        );
    }
}
