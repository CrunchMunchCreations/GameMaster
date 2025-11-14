package xyz.crunchmunch.mods.gamemaster.animator.animation

import com.mojang.serialization.Codec
import xyz.crunchmunch.mods.gamemaster.animator.AnimatableManager

abstract class MultiAnimationDefinition {
    abstract val animations: Map<String, Animation>
    abstract val type: AnimationDefinitionType<*>

    companion object {
        val CODEC: Codec<MultiAnimationDefinition> = AnimatableManager.ANIMATION_TYPE_REGISTRY.byNameCodec()
            .dispatch("type", MultiAnimationDefinition::type, AnimationDefinitionType<*>::codec)
    }
}