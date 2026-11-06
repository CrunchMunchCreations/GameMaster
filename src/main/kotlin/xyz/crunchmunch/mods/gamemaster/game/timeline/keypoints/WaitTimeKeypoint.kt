package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.coroutines.delay
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineContext
import xyz.crunchmunch.mods.gamemaster.utils.GameMasterCodecs
import xyz.crunchmunch.mods.gamemaster.utils.getTimeString
import kotlin.time.Duration

class WaitTimeKeypoint(val duration: Duration) : TimelineKeypoint<WaitTimeKeypoint>(KeypointManager.WAIT_TIME) {
    override suspend fun execute(context: TimelineContext) {
        delay(this.duration)
    }

    override fun toString(): String = "Wait (${duration.getTimeString(true)})"

    companion object {
        val CODEC: MapCodec<WaitTimeKeypoint> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                GameMasterCodecs.DURATION
                    .fieldOf("duration")
                    .forGetter(WaitTimeKeypoint::duration)
            )
                .apply(instance, ::WaitTimeKeypoint)
        }
    }
}
