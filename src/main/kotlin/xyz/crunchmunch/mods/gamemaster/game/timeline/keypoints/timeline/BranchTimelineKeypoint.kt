package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.timeline

import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.coroutines.launch
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.game.timeline.Timeline
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineContext
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.AwaitGameMasterKeypoint
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.GameMasterKeypoints
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.MultiKeypointHolder
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.TimelineKeypoint

/**
 * A timeline keypoint that is designed to run its own independent timeline at the same time as the main timeline.
 */
class BranchTimelineKeypoint(override val keypoints: List<TimelineKeypoint<*>>) : TimelineKeypoint<BranchTimelineKeypoint>(GameMasterKeypoints.BRANCH_TIMELINE), MultiKeypointHolder {
    override suspend fun execute(context: TimelineContext) {
        val forked = context.fork()
        forked.remainingKeypoints.addAll(keypoints)

        TimelineManager.scope.launch {
            while (!forked.keypointsQueue.isEmpty && context.isRunning) {
                try {
                    forked.checkPause()

                    if (forked.remainingKeypoints.isEmpty()) {
                        forked.popKeypoints()
                        continue
                    }

                    val keypoint = forked.remainingKeypoints.peek()

                    if (!forked.skippedKeypoints.contains(keypoint)) {
                        if (context.timeline.type == Timeline.Type.CONFIRMATIONAL) {
                            val awaitKeypoint = AwaitGameMasterKeypoint(Component.empty())
                            forked.remainingKeypoints.addFirst(awaitKeypoint)
                            awaitKeypoint.execute(context)
                        }

                        keypoint.execute(context)
                    }

                    forked.remainingKeypoints.pop()
                } catch (e: Exception) {
                    context.pause()
                    GameMaster.logger.error("An error occurred in forked timeline!", e)
                    context.broadcastToGameMasters(Component.literal("An error occurred while executing forked timeline! Timeline has been paused immediately for safety. Error:").withStyle(ChatFormatting.RED).append(e.message ?: e.localizedMessage))
                }
            }
        }
    }

    override fun toString(): String = "Branch Timeline (${this.keypoints.size} keypoints)"

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
