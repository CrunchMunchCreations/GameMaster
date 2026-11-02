package xyz.crunchmunch.mods.gamemaster.server.camera.keyframes

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.Holder
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.phys.Vec3
import xyz.crunchmunch.mods.gamemaster.server.camera.CameraTrackContext
import xyz.crunchmunch.mods.gamemaster.utils.GameMasterCodecs
import java.util.*

@JvmRecord
data class SoundCameraKeyframe(
    val soundEvent: Holder<SoundEvent>,
    val position: Optional<Vec3> = Optional.empty(),
    val source: SoundSource = SoundSource.VOICE,
    val volume: Float = 1f,
    val pitch: Float = 1f,
) : CameraKeyframe {
    override val codec: MapCodec<out CameraKeyframe>
        get() = CODEC

    override fun setup(context: CameraTrackContext) {
        val position = this.position.orElse(context.camera.position())!!
        context.level.playSound(null, position.x, position.y, position.z, this.soundEvent, this.source, this.volume, this.pitch)
    }

    companion object {
        @JvmField val CODEC: MapCodec<SoundCameraKeyframe> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                SoundEvent.CODEC.fieldOf("sound")
                    .forGetter(SoundCameraKeyframe::soundEvent),
                Vec3.CODEC.optionalFieldOf("position")
                    .forGetter(SoundCameraKeyframe::position),
                GameMasterCodecs.SOUND_SOURCE.lenientOptionalFieldOf("source", SoundSource.VOICE)
                    .forGetter(SoundCameraKeyframe::source),
                ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("volume", 1f)
                    .forGetter(SoundCameraKeyframe::volume),
                Codec.floatRange(0.5f, 2f).lenientOptionalFieldOf("pitch", 1f)
                    .forGetter(SoundCameraKeyframe::pitch),
            )
                .apply(instance, ::SoundCameraKeyframe)
        }
    }
}
