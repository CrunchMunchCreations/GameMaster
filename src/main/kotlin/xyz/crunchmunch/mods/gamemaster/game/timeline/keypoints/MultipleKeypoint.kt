package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints

import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.coroutines.runBlocking
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineContext

/**
 * A timeline keypoint that is designed to run multiple keypoints in the exact same tick.
 */
class MultipleKeypoint(override val keypoints: List<TimelineKeypoint<*>>) : TimelineKeypoint<MultipleKeypoint>(GameMasterKeypoints.MULTIPLE), MultiKeypointHolder {
    override suspend fun execute(context: TimelineContext) {
        for (keypoint in keypoints) {
            context.server.execute {
                runBlocking {
                    keypoint.execute(context)
                }
            }
        }
    }

    override fun toString(): String = "Multiple (${keypoints.joinToString(", ")})"

    companion object {
        val CODEC = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                TimelineKeypoint.CODEC.listOf()
                    .optionalFieldOf("keypoints", emptyList())
                    .forGetter(MultipleKeypoint::keypoints)
            )
                .apply(instance, ::MultipleKeypoint)
        }
    }
}
