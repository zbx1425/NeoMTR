package mtr;

import dev.architectury.injectables.annotations.ExpectPlatform;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.NetworkUtilities;
import mtr.mappings.Utilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Registry {

	@ExpectPlatform
	public static boolean isFabric() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static boolean isClientEnvironment() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static <T extends BlockEntityMapper> BlockEntityType<T> getBlockEntityType(Utilities.TileEntitySupplier<T> supplier, Block block) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static Supplier<CreativeModeTab> getCreativeModeTab(Identifier id, Supplier<ItemStack> supplier) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void registerCreativeModeTab(Identifier resourceLocation, Item item) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static Packet<?> createAddEntityPacket(Entity entity) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void registerNetworkPacket(Identifier resourceLocation) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void registerNetworkReceiver(Identifier resourceLocation, NetworkUtilities.PacketCallback packetCallback) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void registerPlayerJoinEvent(Consumer<ServerPlayer> consumer) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void registerPlayerQuitEvent(Consumer<ServerPlayer> consumer) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void registerServerStartingEvent(Consumer<MinecraftServer> consumer) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void registerServerStoppingEvent(Consumer<MinecraftServer> consumer) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void registerTickEvent(Consumer<MinecraftServer> consumer) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void sendToPlayer(ServerPlayer player, Identifier id, FriendlyByteBuf packet) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void setInTeleportationState(Player player, boolean isRiding) {
		throw new AssertionError();
	}
}
