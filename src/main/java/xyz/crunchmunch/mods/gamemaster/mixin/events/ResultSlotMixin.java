package xyz.crunchmunch.mods.gamemaster.mixin.events;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.crunchmunch.mods.gamemaster.events.PlayerEvents;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Shadow @Final private Player player;

    @Shadow @Final private CraftingContainer craftSlots;

    @Inject(method = "checkTakeAchievements", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;onCraftedBy(Lnet/minecraft/world/entity/player/Player;I)V",
        shift = At.Shift.AFTER))
    private void craft(ItemStack itemStack, CallbackInfo ci) {
        PlayerEvents.CRAFT_ITEM.invoker().onCraftItem(player, itemStack, craftSlots);
    }
}

