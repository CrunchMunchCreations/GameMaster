package xyz.crunchmunch.mods.gamemaster.mixin.devtools;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SharedConstants.class)
public abstract class SharedConstantsMixin {
    @Shadow public static boolean IS_RUNNING_IN_IDE;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void enableMinecraftDevMode(CallbackInfo ci) {
        // you would think Fabric would do this automatically
        IS_RUNNING_IN_IDE = FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
