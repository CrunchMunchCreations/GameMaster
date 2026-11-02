package xyz.crunchmunch.mods.gamemaster.server.camera.keyframes

import com.mojang.serialization.MapCodec
import net.minecraft.core.Registry
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.GameMasterRegistries

object CameraKeyframeTypes {
    @JvmField val PATH = register("path", PathCameraKeyframe.CODEC)
    @JvmField val SOUND = register("sound", SoundCameraKeyframe.CODEC)

    fun init() {}

    private fun <T : CameraKeyframe> register(name: String, codec: MapCodec<T>): MapCodec<T> {
        return Registry.register(GameMasterRegistries.CAMERA_KEYFRAME_TYPE, GameMaster.id(name), codec)
    }
}
