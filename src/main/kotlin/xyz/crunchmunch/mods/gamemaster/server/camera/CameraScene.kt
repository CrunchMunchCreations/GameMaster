package xyz.crunchmunch.mods.gamemaster.server.camera

import com.google.common.collect.Multimap
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.PositionAndRotation
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import xyz.crunchmunch.mods.gamemaster.GameMasterRegistries
import xyz.crunchmunch.mods.gamemaster.server.camera.keyframes.CameraKeyframe
import xyz.crunchmunch.mods.gamemaster.utils.GameMasterCodecs

@JvmRecord
data class CameraScene(
    val keyframes: Multimap<Int, CameraKeyframe>,
    val dimension: ResourceKey<Level>,
    val start: PositionAndRotation,
) {
    fun validate() {
        val keyframeTracks = mutableMapOf<ResourceKey<MapCodec<out CameraKeyframe>>, MutableMap<Int, CameraKeyframe>>()

        this.keyframes.forEach { tick, keyframe ->
            val key = GameMasterRegistries.CAMERA_KEYFRAME_TYPE.getResourceKey(keyframe.codec).orElseThrow()
            val frames = keyframeTracks.computeIfAbsent(key) { mutableMapOf() }

            if (frames.contains(tick))
                throw IllegalArgumentException("Overlapping keyframe type ${key.identifier()} at tick $tick! (existing: ${frames[tick]!!}, new: $keyframe)")

            frames[tick] = keyframe
        }

        for ((_, track) in keyframeTracks) {
            val queue = track.map { it.key to it.value }.sortedBy { it.first }
            val ranges = mutableListOf<IntRange>()

            for ((tick, frame) in queue) {
                ranges.add(tick..(tick + frame.sustainTicks))
            }

            for ((i, range) in ranges.withIndex()) {
                if (i == 0) continue

                // overlap found! time to scream.
                if (range.intersect(ranges[i - 1]).isNotEmpty()) {
                    // need to find the exact keyframe that this is dying on.
                    val currentKeyframe = queue.first { it.first == range.first }
                    val prevKeyframe = queue.first { it.first == ranges[i - 1].first }

                    throw IllegalStateException("Keyframe $currentKeyframe overlaps with $prevKeyframe!")
                }
            }
        }
    }

    companion object {
        @JvmField val CODEC: Codec<CameraScene> = RecordCodecBuilder.create { instance ->
            instance.group(
                GameMasterCodecs.multimap(GameMasterCodecs.STRINGIFIED_INT, CameraKeyframe.CODEC).fieldOf("keyframes")
                    .forGetter(CameraScene::keyframes),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension")
                    .forGetter(CameraScene::dimension),
                GameMasterCodecs.POSITION_AND_ROTATION.fieldOf("start")
                    .forGetter(CameraScene::start),
            )
                .apply(instance, ::CameraScene)
        }
    }
}
