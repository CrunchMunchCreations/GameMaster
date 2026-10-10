package xyz.crunchmunch.mods.gamemaster.animator.util

import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
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
}
