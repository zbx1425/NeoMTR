package mtr.neoforge;

import mtr.neoforge.mappings.ForgeUtilities;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.NetworkUtilities;
import mtr.util.Utilities;
import mtr.mixin.PlayerTeleportationStateAccessor;
import mtr.util.event.Event;
import mtr.util.event.EventFactory;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class RegistryImpl {

	public static boolean isFabric() {
		return false;
	}

	public static boolean isClientEnvironment() {
		return FMLEnvironment.getDist().isClient();
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

	private static final Event<Consumer<ServerPlayer>> PLAYER_JOIN_EVENT = EventFactory.createLoop();
	public static void registerPlayerJoinEvent(Consumer<ServerPlayer> consumer) {
		PLAYER_JOIN_EVENT.register(consumer);
	}

	private static final Event<Consumer<ServerPlayer>> PLAYER_LEAVE_EVENT = EventFactory.createLoop();
	public static void registerPlayerQuitEvent(Consumer<ServerPlayer> consumer) {
		PLAYER_LEAVE_EVENT.register(consumer);
	}

	private static final Event<Consumer<MinecraftServer>> SERVER_STARTING_EVENT = EventFactory.createLoop();
	public static void registerServerStartingEvent(Consumer<MinecraftServer> consumer) {
		SERVER_STARTING_EVENT.register(consumer);
	}

	private static final Event<Consumer<MinecraftServer>> SERVER_STOPPING_EVENT = EventFactory.createLoop();
	public static void registerServerStoppingEvent(Consumer<MinecraftServer> consumer) {
		SERVER_STOPPING_EVENT.register(consumer);
	}

	private static final Event<Consumer<MinecraftServer>> SERVER_TICK_EVENT = EventFactory.createLoop();
	public static void registerTickEvent(Consumer<MinecraftServer> consumer) {
		SERVER_TICK_EVENT.register(consumer);
	}

	public static void sendToPlayer(ServerPlayer player, Identifier id, FriendlyByteBuf packet) {
		packet.resetReaderIndex();
		MTRForge.PACKET_REGISTRY.sendS2C(player, id, packet);
	}

	public static void setInTeleportationState(Player player, boolean isRiding) {
		((PlayerTeleportationStateAccessor) player).setInTeleportationState(isRiding);
	}

	public static class ServerForgeEventBusListener {

		@SubscribeEvent
		public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
			if (event.getEntity().level().isClientSide()) return;
			PLAYER_JOIN_EVENT.invoker().accept((ServerPlayer) event.getEntity());
		}

		@SubscribeEvent
		public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
			if (event.getEntity().level().isClientSide()) return;
			PLAYER_JOIN_EVENT.invoker().accept((ServerPlayer) event.getEntity());
		}

		@SubscribeEvent
		public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
			if (event.getEntity().level().isClientSide()) return;
			PLAYER_LEAVE_EVENT.invoker().accept((ServerPlayer) event.getEntity());
		}

		@SubscribeEvent
		public static void onServerStarting(ServerStartingEvent event) {
			SERVER_STARTING_EVENT.invoker().accept(event.getServer());
		}

		@SubscribeEvent
		public static void onServerStopping(ServerStoppingEvent event) {
			SERVER_STOPPING_EVENT.invoker().accept(event.getServer());
		}

		@SubscribeEvent
		public static void onServerTick(ServerTickEvent.Pre event) {
			SERVER_TICK_EVENT.invoker().accept(event.getServer());
		}
	}

}

