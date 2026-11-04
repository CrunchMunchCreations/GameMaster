package xyz.crunchmunch.mods.gamemaster.animator.animation

import com.mojang.serialization.Codec
import xyz.crunchmunch.mods.gamemaster.animator.AnimatableManager

interface MultiAnimationDefinition {
    val animations: Map<String, Animation>
    val type: AnimationDefinitionType<*>

    companion object {
        val CODEC: Codec<MultiAnimationDefinition> = Codec.withAlternative(
            AnimatableManager.ANIMATION_TYPE_REGISTRY.byNameCodec()
                .dispatch("type", MultiAnimationDefinition::type, AnimationDefinitionType<*>::codec),
            BedrockMultiAnimationDefinition.CODEC.codec()
        )
    }
}
