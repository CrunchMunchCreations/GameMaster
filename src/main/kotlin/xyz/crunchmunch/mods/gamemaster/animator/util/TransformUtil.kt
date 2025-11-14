package xyz.crunchmunch.mods.gamemaster.animator.util

import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector3fc

object TransformUtil {
    fun Vec3.toQuaternion(): Quaternionf {
        return toQuaternion(this.x.toFloat(), this.y.toFloat(), this.z.toFloat())
    }

    fun Vector3fc.toQuaternion(): Quaternionf {
        return toQuaternion(this.x(), this.y(), this.z())
    }

    fun toQuaternion(yaw: Float, pitch: Float, roll: Float): Quaternionf {
        return Quaternionf()
            .rotationXYZ(yaw * Mth.DEG_TO_RAD, pitch * Mth.DEG_TO_RAD, -roll * Mth.DEG_TO_RAD)
    }

    fun lerp(delta: Float, from: Vector3f, to: Vector3f): Vector3f {
        return Vector3f(
            Mth.lerp(delta, from.x, to.x),
            Mth.lerp(delta, from.y, to.y),
            Mth.lerp(delta, from.z, to.z)
        )
    }

    fun lerp(delta: Float, from: Quaternionf, to: Quaternionf): Quaternionf {
        return Quaternionf(
            Mth.lerp(delta, from.x, to.x),
            Mth.lerp(delta, from.y, to.y),
            Mth.lerp(delta, from.z, to.z),
            Mth.lerp(delta, from.w, to.w)
        )
    }
}