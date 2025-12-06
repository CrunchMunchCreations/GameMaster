package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.timeline

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineContext
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.KeypointManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.TimelineKeypoint

/**
 * Basically, runs a timeline within itself, additionally allowing said timeline to be cancelled and rewound at will.
 */
class CategorizeTimelineKeypoint(val category: String, val keypoints: List<TimelineKeypoint<*>>) : TimelineKeypoint<CategorizeTimelineKeypoint>(KeypointManager.CATEGORIZE_TIMELINE) {
    override suspend fun execute(context: TimelineContext) {
        context.pushNewKeypoints()
        context.remainingKeypoints.addAll(this.keypoints)
    }

    companion object {
        val CODEC = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.STRING
                    .fieldOf("category")
                    .forGetter(CategorizeTimelineKeypoint::category),
                TimelineKeypoint.CODEC.listOf()
                    .optionalFieldOf("keypoints", emptyList())
                    .forGetter(CategorizeTimelineKeypoint::keypoints)
            )
                .apply(instance, ::CategorizeTimelineKeypoint)
        }
    }
}