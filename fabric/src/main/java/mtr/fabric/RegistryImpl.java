package mtr.fabric;

import mtr.MTRFabric;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.FabricRegistryUtilities;
import mtr.mappings.NetworkUtilities;
import mtr.util.Utilities;
import mtr.mixin.PlayerTeleportationStateAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
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
		return true;
	}

	public static boolean isClientEnvironment() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	}

	public static <T extends BlockEntityMapper> BlockEntityType<T> getBlockEntityType(Utilities.TileEntitySupplier<T> supplier, Block block) {
		return FabricBlockEntityTypeBuilder.create(supplier::supplier, block).build();
	}

	public static Supplier<CreativeModeTab> getCreativeModeTab(Identifier id, Supplier<ItemStack> supplier) {
		return () -> FabricRegistryUtilities.createCreativeModeTab(id, supplier);
	}

	public static void registerCreativeModeTab(Identifier resourceLocation, Item item) {
	}

	public static void registerNetworkPacket(Identifier resourceLocation) {
		MTRFabric.PACKET_REGISTRY.registerPacket(resourceLocation);
	}

	public static void registerNetworkReceiver(Identifier resourceLocation, NetworkUtilities.PacketCallback packetCallback) {
		MTRFabric.PACKET_REGISTRY.registerNetworkReceiverC2S(resourceLocation, packetCallback);
	}

	public static void registerPlayerJoinEvent(Consumer<ServerPlayer> consumer) {
		ServerEntityEvents.ENTITY_LOAD.register((entity, serverWorld) -> {
			if (entity instanceof ServerPlayer) {
				consumer.accept((ServerPlayer) entity);
			}
		});
	}

	public static void registerPlayerQuitEvent(Consumer<ServerPlayer> consumer) {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> consumer.accept(handler.player));
	}

	public static void registerServerStartingEvent(Consumer<MinecraftServer> consumer) {
		ServerLifecycleEvents.SERVER_STARTING.register(consumer::accept);
	}

	public static void registerServerStoppingEvent(Consumer<MinecraftServer> consumer) {
		ServerLifecycleEvents.SERVER_STOPPING.register(consumer::accept);
	}

	public static void registerTickEvent(Consumer<MinecraftServer> consumer) {
		ServerTickEvents.START_SERVER_TICK.register(consumer::accept);
	}

	public static void sendToPlayer(ServerPlayer player, Identifier id, FriendlyByteBuf packet) {
		MTRFabric.PACKET_REGISTRY.sendS2C(player, id, packet);
	}

	public static void setInTeleportationState(Player player, boolean isRiding) {
		((PlayerTeleportationStateAccessor) player).setInTeleportationState(isRiding);
	}
}
