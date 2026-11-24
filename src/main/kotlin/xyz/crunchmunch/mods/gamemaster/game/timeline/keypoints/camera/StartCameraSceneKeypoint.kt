package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.camera

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.Holder
import net.minecraft.core.registries.codec.RegistryCodecs
import xyz.crunchmunch.mods.gamemaster.GameMasterRegistryKeys
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineContext
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.GameMasterKeypoints
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.TimelineKeypoint
import xyz.crunchmunch.mods.gamemaster.server.camera.CameraManager
import xyz.crunchmunch.mods.gamemaster.server.camera.CameraScene

data class StartCameraSceneKeypoint(
    val scene: Holder<CameraScene>,
) : TimelineKeypoint<StartCameraSceneKeypoint>(GameMasterKeypoints.START_CAMERA_SCENE) {
    override suspend fun execute(context: TimelineContext) {
        CameraManager.startAndAwaitScene(context.server, this.scene.value(), context.server.playerList.players)
    }

    companion object {
        @JvmField val CODEC: MapCodec<StartCameraSceneKeypoint> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                RegistryCodecs.holder(GameMasterRegistryKeys.CAMERA_SCENE)
                    .fieldOf("scene")
                    .forGetter(StartCameraSceneKeypoint::scene),
            )
                .apply(instance, ::StartCameraSceneKeypoint)
        }
    }
}
