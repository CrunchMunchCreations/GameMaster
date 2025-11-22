package xyz.crunchmunch.mods.gamemaster.animator.util

import org.joml.Vector3f

data class PosRotScale(
    val position: Vector3f = Vector3f(),
    val rotation: Vector3f = Vector3f(),
    val scale: Vector3f = Vector3f(1f, 1f, 1f)
)
