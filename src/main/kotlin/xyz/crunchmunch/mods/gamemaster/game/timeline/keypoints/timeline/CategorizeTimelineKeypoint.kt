package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.timeline

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.server.MinecraftServer
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.KeypointManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.TimelineKeypoint

class CategorizeTimelineKeypoint(val category: String, val keypoints: List<TimelineKeypoint<*>>) : TimelineKeypoint<CategorizeTimelineKeypoint>(KeypointManager.CATEGORIZE_TIMELINE) {
    override suspend fun execute(server: MinecraftServer) {
        TODO("Not yet implemented")
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