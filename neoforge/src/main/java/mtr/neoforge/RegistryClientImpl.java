package mtr.neoforge;

import com.mojang.datafixers.util.Pair;
import mtr.MTRClient;
import mtr.mappings.*;
import mtr.util.event.Event;
import mtr.util.event.EventFactory;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RegistryClientImpl {

	private static final List<Consumer<EntityRenderersEvent.RegisterRenderers>> BLOCK_ENTITY_RENDERERS = new ArrayList<>();
	public static <T extends BlockEntityMapper, S extends BlockEntityRenderState> void registerTileEntityRenderer(Supplier<BlockEntityType<T>> type, Function<BlockEntityRenderDispatcher, BlockEntityRendererMapper<T, S>> function) {
		BLOCK_ENTITY_RENDERERS.add(event -> {
			event.registerBlockEntityRenderer(type.get(), ctx -> function.apply(ctx.blockEntityRenderDispatcher()));
		});
	}

	private static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();
	public static void registerKeyBinding(KeyMapping keyMapping) {
		KEY_MAPPINGS.add(keyMapping);
	}

	private static final List<Pair<BlockTintSource, Supplier<Block>>> COLOR_HANDLERS = new ArrayList<>();
	public static void registerBlockColors(Supplier<Block> block) {
		COLOR_HANDLERS.add(new Pair<>(StationColor.INSTANCE, block));
	}

	public static void registerNetworkReceiver(Identifier resourceLocation, Consumer<FriendlyByteBuf> consumer) {
		MTRForge.PACKET_REGISTRY.registerNetworkReceiverS2C(resourceLocation, consumer);
	}

	private static final Event<Consumer<LocalPlayer>> PLAYER_JOIN_EVENT = EventFactory.createLoop();
	public static void registerPlayerJoinEvent(Consumer<LocalPlayer> consumer) {
		PLAYER_JOIN_EVENT.register(consumer);
	}

    private static final Event<Consumer<Minecraft>> CLIENT_TICK_POST_EVENT = EventFactory.createLoop();
    public static void registerPostTickEvent(Consumer<Minecraft> consumer) {
		CLIENT_TICK_POST_EVENT.register(consumer);
    }

	public static void sendToServer(Identifier id, FriendlyByteBuf packet) {
		packet.resetReaderIndex();
		MTRForge.PACKET_REGISTRY.sendC2S(id, packet);
	}

	public static void submitGuiElementRenderState(GuiGraphicsExtractor guiGraphicsExtractor, GuiElementRenderState guiElementRenderState) {
		guiGraphicsExtractor.submitGuiElementRenderState(guiElementRenderState);
	}

	private static class StationColor implements BlockTintSource {

		public static final StationColor INSTANCE = new StationColor();

		@Override
		public int color(BlockState blockState) {
			return MTRClient.getStationColor(null);
		}

		@Override
		public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
			return MTRClient.getStationColor(pos);
		}
	}

	public static class ClientForgeEventBusListener {

		@SubscribeEvent
		public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
			if (!event.getPlayer().level().isClientSide()) return;
			PLAYER_JOIN_EVENT.invoker().accept(event.getPlayer());
		}

		@SubscribeEvent
		public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
			if (!event.getEntity().level().isClientSide()) return;
			PLAYER_JOIN_EVENT.invoker().accept((LocalPlayer) event.getEntity());
		}

        @SubscribeEvent
        public static void onClientTickPost(ClientTickEvent.Post event) {
            CLIENT_TICK_POST_EVENT.invoker().accept(Minecraft.getInstance());
        }
	}

	public static class ClientModEventBusListener {

		@SubscribeEvent
		public static void onRegisterColorHandlers(RegisterColorHandlersEvent.BlockTintSources event) {
			for (Pair<BlockTintSource, Supplier<Block>> pair : COLOR_HANDLERS) {
				event.register(List.of(pair.getFirst()), pair.getSecond().get());
			}
		}

		@SubscribeEvent
		public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
			for (Consumer<EntityRenderersEvent.RegisterRenderers> consumer : BLOCK_ENTITY_RENDERERS) {
				consumer.accept(event);
			}
		}

		@SubscribeEvent
		public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
			for (KeyMapping keyMapping : KEY_MAPPINGS) {
				event.register(keyMapping);
			}
		}
	}
}
