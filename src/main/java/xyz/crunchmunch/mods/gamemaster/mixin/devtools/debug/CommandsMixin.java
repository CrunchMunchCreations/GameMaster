package xyz.crunchmunch.mods.gamemaster.mixin.devtools.debug;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Commands.class)
public class CommandsMixin {
    @ModifyExpressionValue(method = "performCommand", at = @At(value = "FIELD", target = "Lnet/minecraft/SharedConstants;IS_RUNNING_IN_IDE:Z"))
    private boolean alwaysPrintCommandErrors(boolean original) {
        return true;
    }
}
