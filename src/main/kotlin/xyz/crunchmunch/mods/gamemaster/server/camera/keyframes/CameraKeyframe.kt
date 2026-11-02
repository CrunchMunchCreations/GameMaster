package xyz.crunchmunch.mods.gamemaster.server.camera.keyframes

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import xyz.crunchmunch.mods.gamemaster.GameMasterRegistries
import xyz.crunchmunch.mods.gamemaster.server.camera.CameraTrackContext
import java.util.function.Function

interface CameraKeyframe {
    val codec: MapCodec<out CameraKeyframe>

    val sustainTicks: Int
        get() = 0

    fun setup(context: CameraTrackContext) {}
    fun tick(context: CameraTrackContext) {}
    fun stop(context: CameraTrackContext) {}
    fun reset(context: CameraTrackContext) {}

    companion object {
        @JvmField val CODEC: Codec<CameraKeyframe> = GameMasterRegistries.CAMERA_KEYFRAME_TYPE.byNameCodec().dispatch("type", CameraKeyframe::codec, Function.identity())
    }
}
