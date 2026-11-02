package xyz.crunchmunch.mods.gamemaster.server.camera

import com.google.common.collect.Multimap
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.PositionAndRotation
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import xyz.crunchmunch.mods.gamemaster.server.camera.keyframes.CameraKeyframe
import xyz.crunchmunch.mods.gamemaster.utils.GameMasterCodecs

@JvmRecord
data class CameraScene(
    val keyframes: Multimap<Int, CameraKeyframe>,
    val dimension: ResourceKey<Level>,
    val start: PositionAndRotation,
) {
    companion object {
        @JvmField val CODEC: Codec<CameraScene> = RecordCodecBuilder.create { instance ->
            instance.group(
                GameMasterCodecs.multimap(Codec.INT, CameraKeyframe.CODEC).fieldOf("keyframes")
                    .forGetter(CameraScene::keyframes),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension")
                    .forGetter(CameraScene::dimension),
                GameMasterCodecs.POSITION_AND_ROTATION.fieldOf("start")
                    .forGetter(CameraScene::start),
            )
                .apply(instance, ::CameraScene)
        }
    }
}
