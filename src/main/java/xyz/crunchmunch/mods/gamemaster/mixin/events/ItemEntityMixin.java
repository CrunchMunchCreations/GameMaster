package xyz.crunchmunch.mods.gamemaster.mixin.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.crunchmunch.mods.gamemaster.events.PlayerEvents;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void callPlayerPickupEvent(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;

        if (!PlayerEvents.CAN_PICKUP_ITEM.invoker().canPickupItem(serverPlayer, (ItemEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
