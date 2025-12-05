package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.timeline

import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.server.MinecraftServer
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.KeypointManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.TimelineKeypoint

/**
 * A timeline keypoint that is designed to run its own independent timeline at the same time as the main timeline.
 */
class BranchTimelineKeypoint(val keypoints: List<TimelineKeypoint<*>>) : TimelineKeypoint<BranchTimelineKeypoint>(KeypointManager.BRANCH_TIMELINE) {
    override suspend fun execute(server: MinecraftServer) {
        TODO("Not yet implemented")
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