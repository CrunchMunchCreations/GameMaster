package xyz.crunchmunch.mods.gamemaster.animator.util

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

@JvmRecord
data class InterpolatingTransforms(
    val translation: InterpolatingVector3f = InterpolatingVector3f(),
    val rotation: InterpolatingVector3f = InterpolatingVector3f(),
    val scale: InterpolatingVector3f = InterpolatingVector3f(1f, 1f, 1f),
) {
    fun isAnyAnimating(currentTick: Int): Boolean {
        return this.translation.hasRemaining(currentTick) || this.rotation.hasRemaining(currentTick) || this.scale.hasRemaining(currentTick)
    }

    fun estimateEndTick(currentTick: Int): Int {
        var estimatedEndTick = -1

        if (translation.endTick > 0 && translation.endTick > currentTick)
            estimatedEndTick = translation.endTick

        if (rotation.endTick > 0 && rotation.endTick > currentTick && (estimatedEndTick == -1 || rotation.endTick < estimatedEndTick))
            estimatedEndTick = rotation.endTick

        if (scale.endTick > 0 && scale.endTick > currentTick && (estimatedEndTick == -1 || scale.endTick < estimatedEndTick))
            estimatedEndTick = scale.endTick

        if (estimatedEndTick == -1)
            return currentTick

        return estimatedEndTick
    }

    fun update(posRotScale: PosRotScale, startTick: Int, endTick: Int): InterpolatingTransforms {
        val translation = posRotScale.position.map { this.translation.update(it, startTick, endTick) }.orElse(this.translation)
        val rotation = posRotScale.rotation.map { this.rotation.update(it, startTick, endTick) }.orElse(this.rotation)
        val scale = posRotScale.scale.map { this.scale.update(it, startTick, endTick) }.orElse(this.scale)

        if (translation != this.translation || rotation != this.rotation || scale != this.scale)
            return InterpolatingTransforms(translation, rotation, scale)

        return this
    }

    companion object {
        @JvmField val CODEC: Codec<InterpolatingTransforms> = RecordCodecBuilder.create { instance ->
            instance.group(
                InterpolatingVector3f.CODEC.optionalFieldOf("translation", InterpolatingVector3f())
                    .forGetter(InterpolatingTransforms::translation),
                InterpolatingVector3f.CODEC.optionalFieldOf("rotation", InterpolatingVector3f())
                    .forGetter(InterpolatingTransforms::rotation),
                InterpolatingVector3f.CODEC.optionalFieldOf("scale", InterpolatingVector3f(1f, 1f, 1f))
                    .forGetter(InterpolatingTransforms::scale),
            )
                .apply(instance, ::InterpolatingTransforms)
        }
    }
}
