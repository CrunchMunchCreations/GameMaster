package xyz.crunchmunch.mods.gamemaster.utils.serializable

import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB

data class BlockArea(
    val from: BlockPos,
    val to: BlockPos
) {
    fun getBoundingBox(): AABB {
        return AABB(from.center, to.center).inflate(1.05)
    }

    companion object {
        val CODEC = RecordCodecBuilder.create { instance ->
            instance.group(
                BlockPos.CODEC
                    .fieldOf("from")
                    .forGetter(BlockArea::from),
                BlockPos.CODEC
                    .fieldOf("to")
                    .forGetter(BlockArea::to)
            )
                .apply(instance, ::BlockArea)
        }
    }
}
