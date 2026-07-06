package cn.zbx1425.mtrsteamloco.neoforge;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.NTEClientCommand;
import cn.zbx1425.mtrsteamloco.gui.DebugHud;
import cn.zbx1425.mtrsteamloco.gui.ScriptDebugOverlay;
import cn.zbx1425.mtrsteamloco.render.train.SteamSmokeParticle;
import cn.zbx1425.sowcer.shader.ShaderManager;
import mtr.MTRClient;
import mtr.screen.ConfigScreen;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public class ClientProxy {

    public static void initClient() {

    }

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (modContainer, arg) -> new ConfigScreen());
    }

    public static class ModEventBusListener {

        @SubscribeEvent
        public static void onClientSetupEvent(FMLClientSetupEvent event) {
            MTRClient.onClientSetup();
            MainClient.onClientSetup();
        }

        @SubscribeEvent
        public static void onRegistryParticleFactory(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(Main.PARTICLE_STEAM_SMOKE, SteamSmokeParticle.Provider::new);
        }

        @SubscribeEvent
        public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.SCOREBOARD_SIDEBAR, Main.id("script_debug_overlay"),
                    (guiGraphics, tickDelta) -> ScriptDebugOverlay.render(guiGraphics));
        }

        @SubscribeEvent
        public static void onDebugOverlay(RegisterDebugEntriesEvent event) {
            event.register(Main.id("renderer_info"), new DebugHud());
        }

        @SubscribeEvent
        public static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
            ShaderManager.registerPipelines(event::registerPipeline);
        }
    }

    public static class ForgeEventBusListener {

        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            NTEClientCommand.register(event.getDispatcher(), Commands::literal);
        }
    }
}