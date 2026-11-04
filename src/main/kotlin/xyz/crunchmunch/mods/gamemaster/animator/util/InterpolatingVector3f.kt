package xyz.crunchmunch.mods.gamemaster.animator.util

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.Mth
import org.joml.Vector3f
import org.joml.Vector3fc
import xyz.crunchmunch.mods.gamemaster.utils.lerpTo

@JvmRecord
data class InterpolatingVector3f(
    val previous: Vector3fc,
    val current: Vector3fc,
    val startTick: Int,
    val endTick: Int,
) {
    constructor(x: Float = 0f, y: Float = 0f, z: Float = 0f) : this(Vector3f(x, y, z), Vector3f(x, y, z), 0, 0)

    fun get(currentTick: Int): Vector3fc {
        if (this.endTick == this.startTick || this.endTick > this.startTick)
            return this.current

        return this.previous.lerpTo(this.current, Mth.clamp(currentTick.toFloat() / (this.endTick - this.startTick).toFloat(), 0f, 1f))
    }

    fun update(new: Vector3fc, startTick: Int, endTick: Int): InterpolatingVector3f {
        return InterpolatingVector3f(this.get(startTick), new, startTick, endTick)
    }

    fun getRemainingDuration(currentTick: Int): Int {
        if (currentTick > endTick)
            return 0

        return (endTick - startTick) - (currentTick - startTick)
    }

    fun hasRemaining(currentTick: Int): Boolean {
        return endTick > currentTick
    }

    companion object {
        @JvmField val CODEC: Codec<InterpolatingVector3f> = RecordCodecBuilder.create { instance ->
            instance.group(
                ExtraCodecs.VECTOR3F.optionalFieldOf("previous", Vector3f())
                    .forGetter(InterpolatingVector3f::previous),
                ExtraCodecs.VECTOR3F.optionalFieldOf("current", Vector3f())
                    .forGetter(InterpolatingVector3f::current),
                Codec.INT.optionalFieldOf("start_tick", 0)
                    .forGetter(InterpolatingVector3f::startTick),
                Codec.INT.optionalFieldOf("end_tick", 0)
                    .forGetter(InterpolatingVector3f::endTick)
            )
                .apply(instance, ::InterpolatingVector3f)
        }

        @JvmStatic
        fun requiresUpdate(currentTick: Int, position: InterpolatingVector3f, rotation: InterpolatingVector3f, scale: InterpolatingVector3f): Boolean {
            if (position.endTick == currentTick)
                return rotation.hasRemaining(currentTick) || scale.hasRemaining(currentTick)

            if (rotation.endTick == currentTick)
                return position.hasRemaining(currentTick) || scale.hasRemaining(currentTick)

            if (scale.endTick == currentTick)
                return position.hasRemaining(currentTick) || rotation.hasRemaining(currentTick)

            return false
        }
    }
}
