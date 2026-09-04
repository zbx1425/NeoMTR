package cn.zbx1425.mtrsteamloco.mixin;

// This class should've been in mtr.mixin, but that creates package duplication between common and loader submodule

import mtr.client.ClientData;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientTeleportDismountMixin {

	@Inject(method = "handleMovePlayer", at = @At("HEAD"))
	private void mtr$onClientTeleport(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
		ClientData.dismountClientPlayer();
	}
}
