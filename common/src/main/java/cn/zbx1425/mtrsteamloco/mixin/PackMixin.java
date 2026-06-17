package cn.zbx1425.mtrsteamloco.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.function.Supplier;

@Mixin(Pack.class)
public class PackMixin {
    @Shadow @Final private Pack.ResourcesSupplier resources;
    @Shadow @Final private PackLocationInfo location;
    @Unique private Boolean isMTRPack = null;

    @Inject(method = "getCompatibility", at = @At("HEAD"), cancellable = true)
    private void getCompatibility(CallbackInfoReturnable<PackCompatibility> cir) {
        if (isMTRPack == null) {
            try (PackResources packResources = resources.openPrimary(location)) {
                IoSupplier<InputStream> ioSupplier;
                try {
                    ioSupplier = packResources.getResource(PackType.CLIENT_RESOURCES,
                            Identifier.fromNamespaceAndPath("mtr", "mtr_custom_resources.json"));
                    isMTRPack = (ioSupplier != null);
                } catch (Exception ignored) {
                    isMTRPack = false;
                }
            }
        }
        if (isMTRPack) cir.setReturnValue(PackCompatibility.COMPATIBLE);
    }
}
