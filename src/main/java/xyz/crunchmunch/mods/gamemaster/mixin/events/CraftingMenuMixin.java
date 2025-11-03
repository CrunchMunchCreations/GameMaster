package xyz.crunchmunch.mods.gamemaster.mixin.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.crunchmunch.mods.gamemaster.events.PlayerEvents;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @WrapOperation(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setRecipeUsed(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/crafting/RecipeHolder;)Z"))
    private static boolean preventCraftingItem(ResultContainer instance, ServerPlayer player, RecipeHolder<CraftingRecipe> recipeHolder, Operation<Boolean> original, @Local(argsOnly = true) CraftingContainer craftingSlots) {
        if (!PlayerEvents.CAN_CRAFT_ITEM.invoker().checkCanCraftItem(player, recipeHolder, craftingSlots)) {
            return false;
        }

        return original.call(instance, player, recipeHolder);
    }
}
