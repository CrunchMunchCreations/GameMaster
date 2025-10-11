package xyz.crunchmunch.mods.gamemaster.mixin.events;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.crunchmunch.mods.gamemaster.events.PlayerEvents;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @ModifyReturnValue(
        method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At("RETURN")
    )
    private ItemEntity drop(ItemEntity original) {
        if (original != null && PlayerEvents.DROP_ITEM.invoker().onDropItem((ServerPlayer) (Object) this, original)) {
            return null;
        }

        return original;
    }

    @WrapMethod(method = "tick")
    private void handlePlayerTickEvents(Operation<Void> original) {
        PlayerEvents.START_TICK.invoker().onGenericEvent((ServerPlayer) (Object) this);
        original.call();
        PlayerEvents.END_TICK.invoker().onGenericEvent((ServerPlayer) (Object) this);
    }
}
