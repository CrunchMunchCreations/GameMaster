package xyz.crunchmunch.mods.gamemaster.mixin.devtools.debug;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.gui.Hud;

import net.fabricmc.loader.api.FabricLoader;

@Mixin(Hud.class)
public abstract class HudMixin {
    @ModifyExpressionValue(method = "extractTabList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"))
    private boolean renderTabListInDev(boolean original) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return false;
        }

        return original;
    }
}
