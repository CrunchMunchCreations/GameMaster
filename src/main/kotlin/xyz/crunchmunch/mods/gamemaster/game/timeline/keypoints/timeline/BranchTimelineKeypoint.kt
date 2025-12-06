package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.timeline

import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.minecraft.network.chat.Component
import xyz.crunchmunch.mods.gamemaster.game.timeline.Timeline
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineContext
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.AwaitGameMasterKeypoint
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.KeypointManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.TimelineKeypoint

/**
 * A timeline keypoint that is designed to run its own independent timeline at the same time as the main timeline.
 */
class BranchTimelineKeypoint(val keypoints: List<TimelineKeypoint<*>>) : TimelineKeypoint<BranchTimelineKeypoint>(KeypointManager.BRANCH_TIMELINE) {
    override suspend fun execute(context: TimelineContext) {
        coroutineScope {
            launch {
                for (keypoint in keypoints) {
                    if (context.timeline.type == Timeline.Type.CONFIRMATIONAL) {
                        AwaitGameMasterKeypoint(Component.empty()).execute(context)
                    }

                    context.checkPause()
                    keypoint.execute(context)
                }
            }
        }
    }

    companion object {
        val CODEC = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                TimelineKeypoint.CODEC.listOf()
                    .optionalFieldOf("keypoints", emptyList())
                    .forGetter(BranchTimelineKeypoint::keypoints)
            )
                .apply(instance, ::BranchTimelineKeypoint)
        }
    }
}