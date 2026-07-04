package mtr.fabric;

import mtr.MTRClient;
import mtr.MTRFabric;
import mtr.data.IGui;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.FabricRegistryUtilities;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RegistryClientImpl {

	public static <T extends BlockEntityMapper, S extends BlockEntityRenderState> void registerTileEntityRenderer(Supplier<BlockEntityType<T>> type, Function<BlockEntityRenderDispatcher, BlockEntityRendererMapper<T, S>> function) {
		FabricRegistryUtilities.registerTileEntityRenderer(type.get(), function);
	}

	public static void registerKeyBinding(KeyMapping keyMapping) {
		KeyMappingHelper.registerKeyMapping(keyMapping);
	}

	public static void registerBlockColors(Supplier<Block> block) {
		BlockColorRegistry.register(List.of(new BlockTintSource() {
			@Override
			public int color(BlockState state) {
				return IGui.ARGB_GRAY;
			}

			@Override
			public int colorInWorld(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
				return MTRClient.getStationColor(pos);
			}
		}), block.get());
	}

	public static void registerNetworkReceiver(Identifier resourceLocation, Consumer<FriendlyByteBuf> consumer) {
		MTRFabric.PACKET_REGISTRY.registerNetworkReceiverS2C(resourceLocation, consumer);
	}

	public static void registerPlayerJoinEvent(Consumer<LocalPlayer> consumer) {
		ClientEntityEvents.ENTITY_LOAD.register((entity, clientWorld) -> {
			if (entity == Minecraft.getInstance().player) {
				consumer.accept((LocalPlayer) entity);
			}
		});
	}

	public static void registerTickEvent(Consumer<Minecraft> consumer) {
		ClientTickEvents.START_CLIENT_TICK.register(consumer::accept);
	}

	public static void sendToServer(Identifier id, FriendlyByteBuf packet) {
		MTRFabric.PACKET_REGISTRY.sendC2S(id, packet);
	}

	public static void submitGuiElementRenderState(GuiGraphicsExtractor guiGraphicsExtractor, GuiElementRenderState guiElementRenderState) {
		guiGraphicsExtractor.guiRenderState.addGuiElement(guiElementRenderState);
	}
}
