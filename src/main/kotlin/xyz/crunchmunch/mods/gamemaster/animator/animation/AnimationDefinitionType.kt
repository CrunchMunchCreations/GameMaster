package xyz.crunchmunch.mods.gamemaster.animator.animation

import com.mojang.serialization.MapCodec

@JvmRecord
data class AnimationDefinitionType<T : MultiAnimationDefinition>(
    val codec: MapCodec<T>
)
