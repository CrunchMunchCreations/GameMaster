package xyz.crunchmunch.mods.gamemaster

import com.mojang.serialization.MapCodec
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import xyz.crunchmunch.mods.gamemaster.animator.AnimatableManager
import xyz.crunchmunch.mods.gamemaster.game.CustomGameManager
import xyz.crunchmunch.mods.gamemaster.game.marker.GameMarkerManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.TimelineKeypoint
import xyz.crunchmunch.mods.gamemaster.game.timeline.prerequisite.GameMasterPrerequisites
import xyz.crunchmunch.mods.gamemaster.server.CustomClickHandler
import xyz.crunchmunch.mods.gamemaster.server.camera.CameraScene
import xyz.crunchmunch.mods.gamemaster.server.camera.keyframes.CameraKeyframe

object GameMasterRegistryKeys {
    // Game Markers
    @JvmField val GAME_MARKER_TYPE = GameMarkerManager.TYPE_REGISTRY_KEY

    // Custom game
    @JvmField val CUSTOM_GAME = CustomGameManager.GAME_REGISTRY_KEY
    @JvmField val GAME_METADATA = CustomGameManager.GAME_METADATA_REGISTRY_KEY

    // Animation
    @JvmField val ANIMATION_TYPE = AnimatableManager.ANIMATION_TYPE_REGISTRY_KEY
    @JvmField val ANIMATION = AnimatableManager.ANIMATION_REGISTRY_KEY
    @JvmField val MODEL = AnimatableManager.MODEL_REGISTRY_KEY
    @JvmField val ANIMATABLE_ENTITY = AnimatableManager.ANIMATABLE_ENTITY_REGISTRY_KEY

    @JvmField val CUSTOM_CLICK_HANDLER = CustomClickHandler.REGISTRY_KEY

    // Camera
    @JvmField val CAMERA_SCENE = key<CameraScene>("camera/scene")
    @JvmField val CAMERA_KEYFRAME_TYPE = key<MapCodec<out CameraKeyframe>>("camera/keyframe_type")

    private fun <T : Any> key(path: String): ResourceKey<Registry<T>> {
        return ResourceKey.createRegistryKey(GameMaster.id(path))
    }

    // Timeline
    @JvmField val TIMELINE = TimelineManager.REGISTRY_KEY
    @JvmField val KEYPOINT_TYPE = ResourceKey.createRegistryKey<TimelineKeypoint.Type<*>>(GameMaster.id("timeline/keypoint"))
    @JvmField val PREREQUISITE_TYPE = GameMasterPrerequisites.REGISTRY_KEY

    fun init() {}
}
