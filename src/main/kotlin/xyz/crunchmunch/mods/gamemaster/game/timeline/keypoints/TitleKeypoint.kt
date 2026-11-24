package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineContext
import xyz.crunchmunch.mods.gamemaster.utils.sendTitle
import xyz.crunchmunch.mods.gamemaster.utils.setTitleAnimationTimes

data class TitleKeypoint(
    val title: Component = Component.empty(),
    val subtitle: Component = Component.empty(),
    val fadeIn: Int = 10,
    val sustain: Int = 70,
    val fadeOut: Int = 20,
) : MainThreadKeypoint<TitleKeypoint>(GameMasterKeypoints.TITLE) {
    override fun executeOnMainThread(context: TimelineContext) {
        for (player in context.server.playerList.players) {
            player.setTitleAnimationTimes(fadeIn, sustain, fadeOut)
            player.sendTitle(title, subtitle)
        }
    }

    companion object {
        @JvmField val CODEC: MapCodec<TitleKeypoint> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                ComponentSerialization.CODEC.optionalFieldOf("title", Component.empty())
                    .forGetter(TitleKeypoint::title),
                ComponentSerialization.CODEC.optionalFieldOf("subtitle", Component.empty())
                    .forGetter(TitleKeypoint::subtitle),
                Codec.INT.optionalFieldOf("fade_in", 10)
                    .forGetter(TitleKeypoint::fadeIn),
                Codec.INT.optionalFieldOf("sustain", 70)
                    .forGetter(TitleKeypoint::sustain),
                Codec.INT.optionalFieldOf("fade_out", 20)
                    .forGetter(TitleKeypoint::fadeOut),
            )
                .apply(instance, ::TitleKeypoint)
        }
    }
}
