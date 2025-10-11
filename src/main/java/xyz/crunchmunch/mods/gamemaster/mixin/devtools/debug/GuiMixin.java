package xyz.crunchmunch.mods.gamemaster.mixin.devtools.debug;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public class GuiMixin {
    @ModifyExpressionValue(method = "renderTabList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"))
    private boolean renderTabListInDev(boolean original) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return false;
        }

        return original;
    }
}
