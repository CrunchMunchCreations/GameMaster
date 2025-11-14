package xyz.crunchmunch.mods.gamemaster.animator.animation

import com.mojang.serialization.MapCodec

data class AnimationDefinitionType<T : MultiAnimationDefinition>(
    val codec: MapCodec<T>
)
