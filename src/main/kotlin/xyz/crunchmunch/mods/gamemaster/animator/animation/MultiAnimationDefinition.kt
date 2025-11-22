package xyz.crunchmunch.mods.gamemaster.animator.animation

import com.mojang.serialization.Codec
import xyz.crunchmunch.mods.gamemaster.animator.AnimatableManager

abstract class MultiAnimationDefinition {
    abstract val animations: Map<String, Animation>
    abstract val type: AnimationDefinitionType<*>

    companion object {
        val TICKS_AS_SECONDS_STRING_CODEC: Codec<Int> = Codec.STRING.xmap({ (it.toFloat() * 20).toInt() }, { (it.toFloat() / 20f).toString() })
        val TICKS_AS_SECONDS_FLOAT_CODEC: Codec<Int> = Codec.FLOAT.xmap({ (it.toFloat() * 20).toInt() }, { (it.toFloat() / 20f) })

        val CODEC: Codec<MultiAnimationDefinition> = AnimatableManager.ANIMATION_TYPE_REGISTRY.byNameCodec()
            .dispatch("type", MultiAnimationDefinition::type, AnimationDefinitionType<*>::codec)
    }
}