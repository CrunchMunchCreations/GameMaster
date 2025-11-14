package xyz.crunchmunch.mods.gamemaster.animator.animation

import net.minecraft.core.Registry
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.animator.AnimatableManager

object AnimationDefinitionTypes {
    @JvmField val BEDROCK = register("bedrock", AnimationDefinitionType(BedrockMultiAnimationDefinition.CODEC))

    fun init() {}

    private fun <T : MultiAnimationDefinition> register(path: String, type: AnimationDefinitionType<T>): AnimationDefinitionType<T> {
        return Registry.register(AnimatableManager.ANIMATION_TYPE_REGISTRY, GameMaster.id(path), type)
    }
}