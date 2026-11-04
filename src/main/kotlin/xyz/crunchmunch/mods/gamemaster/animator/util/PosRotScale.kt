package xyz.crunchmunch.mods.gamemaster.animator.util

import org.joml.Vector3fc
import java.util.*

@JvmRecord
data class PosRotScale(
    val position: Optional<Vector3fc> = Optional.empty(),
    val rotation: Optional<Vector3fc> = Optional.empty(),
    val scale: Optional<Vector3fc> = Optional.empty(),
) {
    fun withPosition(value: Vector3fc): PosRotScale = PosRotScale(Optional.of(value), this.rotation, this.scale)
    fun withRotation(value: Vector3fc): PosRotScale = PosRotScale(this.position, Optional.of(value), this.scale)
    fun withScale(value: Vector3fc): PosRotScale = PosRotScale(this.position, this.rotation, Optional.of(value))
}
