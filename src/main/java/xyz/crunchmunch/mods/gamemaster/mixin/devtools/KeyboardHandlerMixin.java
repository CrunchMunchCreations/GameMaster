package xyz.crunchmunch.mods.gamemaster.mixin.devtools;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xyz.crunchmunch.mods.gamemaster.client.devtools.screen.DebugRenderersScreen;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow protected abstract boolean handleChunkDebugKeys(int keyCode);

    @WrapOperation(
        method = {"keyPress"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/KeyboardHandler;handleDebugKeys(I)Z"
        )}
    )
    private boolean devtools$f3Key(KeyboardHandler keyboard, int key, Operation<Boolean> original) {
        if (key == GLFW.GLFW_KEY_F6) {
            Minecraft.getInstance().setScreen(new DebugRenderersScreen());
        }

        return this.handleChunkDebugKeys(key) || original.call(keyboard, key);
    }
}
