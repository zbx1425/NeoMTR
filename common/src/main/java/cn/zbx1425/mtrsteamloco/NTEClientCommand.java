package cn.zbx1425.mtrsteamloco;

import cn.zbx1425.mtrsteamloco.game.TrainVirtualDrive;
import cn.zbx1425.mtrsteamloco.gui.ConfigScreen;
import cn.zbx1425.mtrsteamloco.render.RenderUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import mtr.mappings.Text;
import net.minecraft.client.Minecraft;

import java.util.function.Function;

public class NTEClientCommand {

    private static final SimpleCommandExceptionType ERROR_NOT_RIDING = new SimpleCommandExceptionType(Text.translatable("commands.mtrsteamloco.virtdrive.not_riding"));

    public static <T> void register(CommandDispatcher<T> dispatcher, Function<String, LiteralArgumentBuilder<T>> literal) {
        dispatcher.register(literal.apply("mtrnte")
                .then(literal.apply("config")
                        .executes(context -> {
                            Minecraft.getInstance().execute(() -> {
                                Minecraft.getInstance().setScreen(ConfigScreen.createScreen(Minecraft.getInstance().screen));
                            });
                            return 1;
                        }))
                .then(literal.apply("hideriding")
                        .executes(context -> {
                            ClientConfig.hideRidingTrain = !ClientConfig.hideRidingTrain;
                            return 1;
                        }))
                .then(literal.apply("virtdrive")
                        .executes(context -> {
                            boolean successful = TrainVirtualDrive.startDrivingRidingTrain();
                            if (!successful) throw ERROR_NOT_RIDING.create();
                            return 1;
                        })
                        .then(literal.apply("atpcutout").executes(context -> {
                            if (TrainVirtualDrive.activeTrain == null) throw ERROR_NOT_RIDING.create();
                            TrainVirtualDrive.activeTrain.atpCutout = !TrainVirtualDrive.activeTrain.atpCutout;
                            return 1;
                        })))
                .then(literal.apply("stat")
                        .executes(context -> {
                            Minecraft.getInstance().execute(() -> {
                                String info = RenderUtil.getRenderStatusMessage();
                                Minecraft.getInstance().player.sendSystemMessage(Text.literal(info));
                            });
                            return 1;
                        }))
        );
    }
}
