package xyz.crunchmunch.mods.gamemaster.server.camera

import com.google.common.collect.Queues
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import xyz.crunchmunch.mods.gamemaster.server.camera.keyframes.CameraKeyframe

class CameraTrack<T : CameraKeyframe>(private val keyframes: Map<Int, T>, level: ServerLevel, camera: CameraEntity, activeScene: ResourceKey<CameraScene>) {
    private val keyframeQueue = Queues.newArrayDeque(keyframes.map { QueuedKeyframe(it.key, it.value) })
    private val context = CameraTrackContext.Mutable(activeScene, level, camera)
    private var currentTick = 0

    fun validate() {
        val ranges = mutableListOf<IntRange>()

        for (keyframe in this.keyframeQueue) {
            ranges.add(keyframe.tick..(keyframe.tick + keyframe.keyframe.sustainTicks))
        }

        for ((i, range) in ranges.withIndex()) {
            if (i == 0) continue

            // overlap found! time to scream.
            if (range.intersect(ranges[i - 1]).isNotEmpty()) {
                // need to find the exact keyframe that this is dying on.
                val currentKeyframe = this.keyframeQueue.first { it.tick == range.first }
                val prevKeyframe = this.keyframeQueue.first { it.tick == ranges[i - 1].first }

                throw IllegalStateException("Keyframe $currentKeyframe overlaps with $prevKeyframe!")
            }
        }
    }

    fun tick() {
        if (keyframeQueue.isEmpty())
            return

        val frame = keyframeQueue.peek()

        if (currentTick++ >= frame.tick) {
            if (context.currentFrameTick == 0)
                frame.keyframe.setup(context)

            frame.keyframe.tick(context)

            if (context.currentFrameTick >= frame.keyframe.sustainTicks) {
                keyframeQueue.remove()
                frame.keyframe.stop(context)

                context.reset()
            } else {
                context.currentFrameTick++
            }
        }
    }

    fun stopAndReset() {
        val context = this.context

        if (this.currentTick >= this.keyframeQueue.first.tick)
            this.keyframeQueue.first.keyframe.stop(context)

        this.keyframeQueue.last.keyframe.stop(context) // Last keyframe should always send the player somewhere.
        this.keyframeQueue.clear()

        context.currentFrameTick = 0
        for (keyframe in this.keyframes.values) {
            keyframe.reset(context)
        }
    }

    val isComplete: Boolean
        get() = this.keyframeQueue.isEmpty()

    @JvmRecord
    private data class QueuedKeyframe<T : CameraKeyframe>(val tick: Int, val keyframe: T)
}
