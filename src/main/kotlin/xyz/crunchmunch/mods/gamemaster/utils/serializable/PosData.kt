package xyz.crunchmunch.mods.gamemaster.utils.serializable

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.phys.Vec3

data class PosData(
    val pos: Vec3,
    val yaw: Float
) {
    companion object {
        val CODEC = RecordCodecBuilder.create { instance ->
            instance.group(
                Vec3.CODEC
                    .fieldOf("pos")
                    .forGetter(PosData::pos),
                Codec.FLOAT
                    .fieldOf("yaw")
                    .forGetter(PosData::yaw)
            )
                .apply(instance, ::PosData)
        }
    }
}
