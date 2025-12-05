package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints

import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.coroutines.runBlocking
import net.minecraft.server.MinecraftServer

/**
 * A timeline keypoint that is designed to run multiple keypoints in the exact same tick.
 */
class MultipleKeypoint(val keypoints: List<TimelineKeypoint<*>>) : TimelineKeypoint<MultipleKeypoint>(KeypointManager.MULTIPLE) {
    override suspend fun execute(server: MinecraftServer) {
        server.execute {
            for (keypoint in keypoints) {
                runBlocking {
                    keypoint.execute(server)
                }
            }
        }
    }

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