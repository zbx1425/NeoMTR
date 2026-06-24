package mtr.mixin;

import mtr.client.ClientData;
import mtr.data.TrainClient;
import mtr.render.RenderTrains;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    public void mtr$interiorLightPassenger(Entity entity, float partialTickTime, CallbackInfoReturnable<Integer> cir) {
        if(entity instanceof Player player) {
            for(TrainClient trainClient : ClientData.TRAINS) {
                if(trainClient.isOnRoute() && trainClient.isPlayerRiding(player)) {
                    cir.setReturnValue(RenderTrains.MAX_LIGHT_INTERIOR);
                }
            }
        }
    }
}
