package mtr.mixin;

import mtr.storage.RailwayDataManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    // TODO: This will block off the server thread. Have to check if it's also done this way in older MC versions.
    @Inject(method = "saveLevelData", at = @At("HEAD"))
    public void mtr$saveRailwayData(boolean sync, CallbackInfo ci) {
        RailwayDataManager.save((ServerLevel)(Object)this, sync);
    }
}
