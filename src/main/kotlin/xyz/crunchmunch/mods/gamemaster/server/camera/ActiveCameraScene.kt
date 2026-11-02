package xyz.crunchmunch.mods.gamemaster.server.camera

import com.mojang.serialization.MapCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import xyz.crunchmunch.mods.gamemaster.server.camera.keyframes.CameraKeyframe

@JvmRecord
data class ActiveCameraScene(
    val scene: CameraScene,
    val level: ServerLevel,
    val camera: CameraEntity,
    val tracks: Map<ResourceKey<MapCodec<out CameraKeyframe>>, CameraTrack<out CameraKeyframe>>,
)
