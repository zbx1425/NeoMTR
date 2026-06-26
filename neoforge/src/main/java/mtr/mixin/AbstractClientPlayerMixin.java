package mtr.mixin;

import mtr.client.ClientData;
import mtr.client.ViewBobbingHelper;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Shadow
    public abstract ClientAvatarState avatarState();

    @Inject(method = "updateBob", at = @At("HEAD"), cancellable = true)
    public void mtr$updateBobFromTrainMovement(CallbackInfo ci) {
        UUID uuid = ((AbstractClientPlayer)(Object)this).getUUID();
        if(ClientData.isRiding(uuid)) {
            avatarState().updateBob(ViewBobbingHelper.getBobbingFactor());
            ci.cancel();
        }
    }

    @Inject(method = "addWalkedDistance", at = @At("TAIL"))
    public void mtr$addTrainMovementDistance(float distance, CallbackInfo ci) {
        avatarState().addWalkDistance(ViewBobbingHelper.getBobDistance());
    }
}
