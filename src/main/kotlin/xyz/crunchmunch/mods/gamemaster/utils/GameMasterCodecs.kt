package xyz.crunchmunch.mods.gamemaster.utils

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object GameMasterCodecs {
    @JvmField
    val AABB_CODEC: Codec<AABB> = RecordCodecBuilder.create { instance ->
        instance.group(
            Vec3.CODEC
                .fieldOf("start")
                .forGetter(AABB::getMinPosition),
            Vec3.CODEC
                .fieldOf("to")
                .forGetter(AABB::getMaxPosition)
        )
            .apply(instance, ::AABB)
    }
}