package xyz.crunchmunch.mods.gamemaster.server.camera.keyframes

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import xyz.crunchmunch.mods.gamemaster.server.camera.CameraTrackContext
import xyz.crunchmunch.mods.gamemaster.utils.sendTitle
import xyz.crunchmunch.mods.gamemaster.utils.setTitleAnimationTimes

@JvmRecord
data class SetTitleCameraKeyframe(
    val title: Component = Component.empty(),
    val subtitle: Component = Component.empty(),
    val fadeIn: Int = 10,
    val sustain: Int = 70,
    val fadeOut: Int = 20,
) : CameraKeyframe {
    override fun setup(context: CameraTrackContext) {
        for (player in context.activePlayers) {
            player.setTitleAnimationTimes(this.fadeIn, this.sustain, this.fadeOut)
            player.sendTitle(this.title, this.subtitle)
        }
    }

    override val sustainTicks: Int
        get() = this.fadeIn + this.sustain + this.fadeOut

    override val codec: MapCodec<out CameraKeyframe>
        get() = CODEC

    companion object {
        @JvmField val CODEC: MapCodec<SetTitleCameraKeyframe> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                ComponentSerialization.CODEC.optionalFieldOf("title", Component.empty())
                    .forGetter(SetTitleCameraKeyframe::title),
                ComponentSerialization.CODEC.optionalFieldOf("subtitle", Component.empty())
                    .forGetter(SetTitleCameraKeyframe::subtitle),

                Codec.INT.optionalFieldOf("fade_in", 10)
                    .forGetter(SetTitleCameraKeyframe::fadeIn),
                Codec.INT.optionalFieldOf("sustain", 70)
                    .forGetter(SetTitleCameraKeyframe::sustain),
                Codec.INT.optionalFieldOf("fade_out", 20)
                    .forGetter(SetTitleCameraKeyframe::fadeOut),
            )
                .apply(instance, ::SetTitleCameraKeyframe)
        }
    }
}
