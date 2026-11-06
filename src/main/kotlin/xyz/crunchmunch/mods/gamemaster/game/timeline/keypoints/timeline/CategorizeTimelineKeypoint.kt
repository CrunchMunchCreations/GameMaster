package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.timeline

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineContext
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.GameMasterKeypoints
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.MultiKeypointHolder
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.TimelineKeypoint

/**
 * Basically, runs a timeline within itself, additionally allowing said timeline to be cancelled and rewound at will.
 */
class CategorizeTimelineKeypoint(val category: String, override val keypoints: List<TimelineKeypoint<*>>) : TimelineKeypoint<CategorizeTimelineKeypoint>(GameMasterKeypoints.CATEGORIZE_TIMELINE), MultiKeypointHolder {
    override suspend fun execute(context: TimelineContext) {
        context.remainingKeypoints.pop() // we need to pop ourselves out.
        context.pushNewKeypoints()
        context.remainingKeypoints.addAll(this.keypoints)
    }

    override fun toString(): String = "Categorize ($category, ${keypoints.size} keypoints)"

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
