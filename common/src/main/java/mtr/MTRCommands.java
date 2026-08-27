package mtr;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mtr.data.RailwayData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class MTRCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mtr")
            .then(buildConfigNode())
        );
    }

    private static final ConfigEntry[] CONFIG_ENTRIES = {
        new ConfigEntry("use_time_and_wind_sync",
            RailwayData::getUseTimeAndWindSync, RailwayData::setUseTimeAndWindSync),
        new ConfigEntry("disable_train_block_interaction",
            RailwayData::getDisableTrainBlockInteraction, RailwayData::setDisableTrainBlockInteraction),
    };

    private static LiteralArgumentBuilder<CommandSourceStack> buildConfigNode() {
        var configNode = Commands.literal("config")
            .requires(Commands.hasPermission(new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER)));
        for (ConfigEntry entry : CONFIG_ENTRIES) {
            configNode = configNode
                .then(Commands.literal(entry.name)
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> setConfigAll(context, entry))
                    )
                    .executes(context -> getConfigAll(context, entry))
                );
        }
        var dimArgNode = Commands.argument("dimension", DimensionArgument.dimension());
        for (ConfigEntry entry : CONFIG_ENTRIES) {
            dimArgNode = dimArgNode
                .then(Commands.literal(entry.name)
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> setConfig(context, entry))
                    )
                    .executes(context -> getConfig(context, entry))
                );
        }
        configNode = configNode.then(Commands.literal("dimension").then(dimArgNode));
        return configNode;
    }

    private static int setConfigAll(CommandContext<CommandSourceStack> context, ConfigEntry entry) {
        final boolean value = BoolArgumentType.getBool(context, "value");
        final MinecraftServer server = context.getSource().getServer();
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            final RailwayData railwayData = RailwayData.getInstance(level);
            if (railwayData != null) {
                entry.setter.accept(railwayData, value);
                count++;
            }
        }
        final int finalCount = count;
        context.getSource().sendSuccess(() -> Component.literal("[all(" + finalCount + ")] " + entry.name + " = " + value), true);
        return count > 0 ? 1 : 0;
    }

    private static int getConfigAll(CommandContext<CommandSourceStack> context, ConfigEntry entry) {
        final MinecraftServer server = context.getSource().getServer();
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            final RailwayData railwayData = RailwayData.getInstance(level);
            if (railwayData != null) {
                final boolean value = entry.getter.apply(railwayData);
                if (count > 0) sb.append("\n");
                sb.append("[").append(level.dimension().identifier()).append("] ").append(entry.name).append(" = ").append(value);
                count++;
            }
        }
        final String result = sb.toString();
        context.getSource().sendSuccess(() -> Component.literal(result), false);
        return count > 0 ? 1 : 0;
    }

    private static int setConfig(CommandContext<CommandSourceStack> context, ConfigEntry entry) throws CommandSyntaxException {
        final boolean value = BoolArgumentType.getBool(context, "value");
        final ServerLevel level = DimensionArgument.getDimension(context, "dimension");
        final RailwayData railwayData = RailwayData.getInstance(level);
        if (railwayData == null) return 0;
        entry.setter.accept(railwayData, value);
        final String dim = level.dimension().identifier().toString();
        context.getSource().sendSuccess(() -> Component.literal("[" + dim + "] " + entry.name + " = " + value), true);
        return 1;
    }

    private static int getConfig(CommandContext<CommandSourceStack> context, ConfigEntry entry) throws CommandSyntaxException {
        final ServerLevel level = DimensionArgument.getDimension(context, "dimension");
        final RailwayData railwayData = RailwayData.getInstance(level);
        if (railwayData == null) return 0;
        final boolean value = entry.getter.apply(railwayData);
        final String dim = level.dimension().identifier().toString();
        context.getSource().sendSuccess(() -> Component.literal("[" + dim + "] " + entry.name + " = " + value), false);
        return 1;
    }

    private record ConfigEntry(String name, Function<RailwayData, Boolean> getter, BiConsumer<RailwayData, Boolean> setter) {}
}
