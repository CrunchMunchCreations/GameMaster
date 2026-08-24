package xyz.crunchmunch.mods.gamemaster.animator

import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.LivingEntity
import xyz.crunchmunch.mods.gamemaster.animator.animation.MultiAnimationDefinition

@JvmRecord
data class AnimatableEntityType<E : LivingEntity>(
    val builder: (E) -> AnimatableEntity<E>,
    val dimensions: EntityDimensions,

    val model: ResourceKey<ModelDefinition>,
    val animations: ResourceKey<MultiAnimationDefinition>,
)
