package mtr.mappings;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface NetworkUtilities {

	@FunctionalInterface
	interface PacketCallback {
		void packetCallback(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet);
	}
}
