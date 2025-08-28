package xyz.crunchmunch.mods.gamemaster.mixin.devtools;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "selfTest", at = @At("HEAD"), cancellable = true)
    private void avoidVanillaSelfTest(CallbackInfo ci) {
        ci.cancel();
    }

}
