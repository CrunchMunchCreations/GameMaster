package xyz.crunchmunch.mods.gamemaster.mixin.animatable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.crunchmunch.mods.gamemaster.GameMasterAttachments;

import net.minecraft.world.entity.Entity;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;

@Mixin(Entity.class)
public abstract class EntityMixin implements AttachmentTarget {
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void gamemaster$handleAnimatableEntityRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        if (this.hasAttached(GameMasterAttachments.ANIMATABLE_ENTITY)) {
            var animatable = this.getAttachedOrThrow(GameMasterAttachments.ANIMATABLE_ENTITY);
            animatable.onRemoved(reason);
        }
    }
}
