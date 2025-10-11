package xyz.crunchmunch.mods.gamemaster.players.metrics

import com.mojang.authlib.GameProfile
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
import org.jetbrains.annotations.ApiStatus

data class MetricData<T>(
    val profile: GameProfile,
    val data: T
) {
    @ApiStatus.Internal
    internal var isDirty = false

    /**
     * Marks this data as modified, and will be saved in the next tick.
     */
    fun markDirty() {
        isDirty = true
    }

    companion object {
        fun <T> makeCodec(dataCodec: Codec<T>): Codec<MetricData<T>> {
            return RecordCodecBuilder.create { instance ->
                instance.group(
                    ExtraCodecs.AUTHLIB_GAME_PROFILE
                        .fieldOf("profile")
                        .forGetter(MetricData<T>::profile),
                    dataCodec
                        .fieldOf("data")
                        .forGetter(MetricData<T>::data)
                )
                    .apply(instance, ::MetricData)
            }
        }
    }
}