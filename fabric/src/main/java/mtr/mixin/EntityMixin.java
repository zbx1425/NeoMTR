package mtr.mixin;

import mtr.client.ClientData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public abstract UUID getUUID();

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    public void shouldRender(double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if(ClientData.isRiding(this.getUUID())) {
            cir.setReturnValue(true);
        }
    }
}
