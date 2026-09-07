package cn.zbx1425.mtrsteamloco.mixin;

// This class should've been in mtr.mixin, but that creates package duplication between common and loader submodule

import mtr.data.RailwayData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class TeleportDismountMixin {

	@Shadow
	public ServerPlayer player;

	@Inject(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("HEAD"))
	private void mtr$onTeleport(PositionMoveRotation destination, Set<Relative> relatives, CallbackInfo ci) {
		player.level().getServer().execute(() -> {
			final RailwayData railwayData = RailwayData.getInstance(player.level());
			if (railwayData != null) {
				railwayData.railwayDataCoolDownModule.dismountPlayer(player);
			}
		});
	}
}
