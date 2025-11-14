package xyz.crunchmunch.mods.gamemaster.animator

import net.minecraft.resources.ResourceKey
import xyz.crunchmunch.mods.gamemaster.animator.animation.MultiAnimationDefinition

abstract class AnimatableMarkerData(
    val model: ResourceKey<ModelDefinition>,
    val animations: ResourceKey<MultiAnimationDefinition>
) {
}