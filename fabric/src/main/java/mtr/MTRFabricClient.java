package mtr;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.NTEClientCommand;
import cn.zbx1425.mtrsteamloco.gui.ScriptDebugOverlay;
import cn.zbx1425.mtrsteamloco.render.train.SteamSmokeParticle;
import cn.zbx1425.sowcerext.model.integration.BufferSourceProxy;
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.CustomResources;
import mtr.client.ICustomResources;
import mtr.render.RenderDrivingOverlay;
import mtr.render.RenderTrains;
import mtr.screen.ResourcePackCreatorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.phys.Vec3;

public class MTRFabricClient implements ClientModInitializer, ICustomResources {

	@Override
	public void onInitializeClient() {
		MTRClient.init();
		MTRClient.initItemModelPredicate();
		MainClient.init();

		// TODO: Correct injection point?
		LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(context -> {
			final PoseStack matrices = context.poseStack();
			matrices.pushPose();
			final Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
			matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
			RenderTrains.render(0, matrices, context.bufferSource());
			matrices.popPose();
		});
		LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((worldRenderContext, hitResult) -> {
			Profiler.get().popPush("NTEBlockEntities");
			BufferSourceProxy vertexConsumersProxy = new BufferSourceProxy(Minecraft.getInstance().renderBuffers().bufferSource());
			MainClient.drawScheduler.commit(vertexConsumersProxy, MainClient.drawContext);
			vertexConsumersProxy.commit();
            return true;
        });
		LevelRenderEvents.END_MAIN.register(event -> {
			ResourcePackCreatorScreen.render(event.poseStack());
			MainClient.drawContext.resetFrameProfiler();
		});
		HudElementRegistry.addFirst(MTR.id("driving_hud"), (guiGraphics, tickDelta) -> RenderDrivingOverlay.render(guiGraphics));
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new CustomResourcesWrapper());
		MTRFabric.PACKET_REGISTRY.commitClient();

		ParticleProviderRegistry.getInstance().register(Main.PARTICLE_STEAM_SMOKE, SteamSmokeParticle.Provider::new);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			NTEClientCommand.register(dispatcher, ClientCommands::literal);
		});
		HudElementRegistry.addLast(MTR.id("js_debug_hud"), (guiGraphics, tickDelta) -> ScriptDebugOverlay.render(guiGraphics));
	}

	private static class CustomResourcesWrapper implements SimpleSynchronousResourceReloadListener {

		@Override
		public Identifier getFabricId() {
			return MTR.id(CUSTOM_RESOURCES_ID);
		}

		@Override
		public void onResourceManagerReload(ResourceManager resourceManager) {
			CustomResources.reload(resourceManager);
		}
	}
}
