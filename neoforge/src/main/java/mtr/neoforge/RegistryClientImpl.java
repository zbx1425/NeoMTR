package mtr.neoforge;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import mtr.MTRClient;
import mtr.neoforge.mappings.ForgeUtilities;
import mtr.mappings.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;
import java.util.function.Function;

public class RegistryClientImpl {

	public static <T extends BlockEntityMapper, S extends BlockEntityRenderState> void registerTileEntityRenderer(BlockEntityType<T> type, Function<BlockEntityRenderDispatcher, BlockEntityRendererMapper<T, S>> function) {
		BlockEntityRendererRegistry.register(type, context -> function.apply(null));
	}

	public static void registerKeyBinding(KeyMapping keyMapping) {
		ForgeUtilities.registerKeyBinding(keyMapping);
	}

	public static void registerBlockColors(Block block) {
		RegistryUtilitiesClient.registerBlockColors(new StationColor(), block);
	}

	public static void registerNetworkReceiver(Identifier resourceLocation, Consumer<FriendlyByteBuf> consumer) {
		MTRForge.PACKET_REGISTRY.registerNetworkReceiverS2C(resourceLocation, consumer);
	}

	public static void registerPlayerJoinEvent(Consumer<LocalPlayer> consumer) {
		RegistryUtilitiesClient.registerPlayerJoinEvent(consumer);
	}

	public static void registerTickEvent(Consumer<Minecraft> consumer) {
		RegistryUtilitiesClient.registerClientTickEvent(consumer);
	}

	public static void sendToServer(Identifier id, FriendlyByteBuf packet) {
		packet.resetReaderIndex();
		MTRForge.PACKET_REGISTRY.sendC2S(id, packet);
	}

	public static void submitGuiElementRenderState(GuiGraphicsExtractor guiGraphicsExtractor, GuiElementRenderState guiElementRenderState) {
		guiGraphicsExtractor.submitGuiElementRenderState(guiElementRenderState);
	}

	private static class StationColor implements BlockTintSource {

		@Override
		public int color(BlockState blockState) {
			return MTRClient.getStationColor(null);
		}

		@Override
		public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
			return MTRClient.getStationColor(pos);
		}
	}


	public interface RegistryUtilitiesClient {

		static void registerBlockColors(BlockTintSource blockColor, Block block) {
			ColorHandlerRegistry.registerBlockColors(blockColor, block);
		}

		static void registerPlayerJoinEvent(Consumer<LocalPlayer> consumer) {
			ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(consumer::accept);
		}

		static void registerClientStoppingEvent(Consumer<Minecraft> consumer) {
			ClientLifecycleEvent.CLIENT_STOPPING.register(consumer::accept);
		}

		static void registerClientTickEvent(Consumer<Minecraft> consumer) {
			ClientTickEvent.CLIENT_PRE.register(consumer::accept);
		}
	}
}
