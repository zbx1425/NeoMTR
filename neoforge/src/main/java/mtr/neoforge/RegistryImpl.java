package mtr.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import mtr.neoforge.mappings.ForgeUtilities;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.NetworkUtilities;
import mtr.util.Utilities;
import mtr.mixin.PlayerTeleportationStateAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class RegistryImpl {

	public static boolean isFabric() {
		return false;
	}

	public static boolean isClientEnvironment() {
		return Platform.getEnvironment() == Env.CLIENT;
	}

	public static <T extends BlockEntityMapper> BlockEntityType<T> getBlockEntityType(Utilities.TileEntitySupplier<T> supplier, Block block) {
		return new BlockEntityType<>(supplier::supplier, block);
	}

	public static Supplier<CreativeModeTab> getCreativeModeTab(Identifier id, Supplier<ItemStack> supplier) {
		String normalizedPath = id.getPath().startsWith(id.getNamespace() + "_")
				? id.getPath().substring(id.getNamespace().length() + 1) : id.getPath();
		return ForgeUtilities.createCreativeModeTab(id, supplier,
				String.format("itemGroup.%s.%s", id.getNamespace(), normalizedPath));
	}

	public static void registerCreativeModeTab(Identifier resourceLocation, Item item) {
		ForgeUtilities.registerCreativeModeTab(resourceLocation, item);
	}

	public static void registerNetworkPacket(Identifier resourceLocation) {
		MTRForge.PACKET_REGISTRY.registerPacket(resourceLocation);
	}

	public static void registerNetworkReceiver(Identifier resourceLocation, NetworkUtilities.PacketCallback packetCallback) {
		MTRForge.PACKET_REGISTRY.registerNetworkReceiverC2S(resourceLocation, packetCallback);
	}

	public static void registerPlayerJoinEvent(Consumer<ServerPlayer> consumer) {
		RegistryUtilities.registerPlayerJoinEvent(consumer);
		RegistryUtilities.registerPlayerChangeDimensionEvent(consumer);
	}

	public static void registerPlayerQuitEvent(Consumer<ServerPlayer> consumer) {
		RegistryUtilities.registerPlayerQuitEvent(consumer);
	}

	public static void registerServerStartingEvent(Consumer<MinecraftServer> consumer) {
		RegistryUtilities.registerServerStartingEvent(consumer);
	}

	public static void registerServerStoppingEvent(Consumer<MinecraftServer> consumer) {
		RegistryUtilities.registerServerStoppingEvent(consumer);
	}

	public static void registerTickEvent(Consumer<MinecraftServer> consumer) {
		RegistryUtilities.registerTickEvent(consumer);
	}

	public static void sendToPlayer(ServerPlayer player, Identifier id, FriendlyByteBuf packet) {
		packet.resetReaderIndex();
		MTRForge.PACKET_REGISTRY.sendS2C(player, id, packet);
	}

	public static void setInTeleportationState(Player player, boolean isRiding) {
		((PlayerTeleportationStateAccessor) player).setInTeleportationState(isRiding);
	}


	public interface RegistryUtilities {

		static void registerCommand(Consumer<CommandDispatcher<CommandSourceStack>> callback) {
			CommandRegistrationEvent.EVENT.register((dispatcher, dedicated, commandSelection) -> callback.accept(dispatcher));
		}

		static void registerPlayerJoinEvent(Consumer<ServerPlayer> consumer) {
			PlayerEvent.PLAYER_JOIN.register(consumer::accept);
		}

		static void registerPlayerQuitEvent(Consumer<ServerPlayer> consumer) {
			PlayerEvent.PLAYER_QUIT.register(consumer::accept);
		}

		static void registerPlayerChangeDimensionEvent(Consumer<ServerPlayer> consumer) {
			PlayerEvent.CHANGE_DIMENSION.register(((player, oldWorld, newWorld) -> consumer.accept(player)));
		}

		static void registerServerStartingEvent(Consumer<MinecraftServer> consumer) {
			LifecycleEvent.SERVER_STARTING.register(consumer::accept);
		}

		static void registerServerStoppingEvent(Consumer<MinecraftServer> consumer) {
			LifecycleEvent.SERVER_STOPPING.register(consumer::accept);
		}

		static void registerTickEvent(Consumer<MinecraftServer> consumer) {
			TickEvent.SERVER_PRE.register(consumer::accept);
		}
	}
}

