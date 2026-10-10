package xyz.crunchmunch.mods.gamemaster.utils

import org.joml.Vector3f
import org.joml.Vector3fc

fun Vector3fc.lerpTo(to: Vector3fc, a: Float): Vector3f {
    return this.lerp(to, a, Vector3f())
}
