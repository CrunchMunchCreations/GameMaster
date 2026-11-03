package xyz.crunchmunch.mods.gamemaster

import com.mojang.serialization.MapCodec
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import xyz.crunchmunch.mods.gamemaster.animator.AnimatableManager
import xyz.crunchmunch.mods.gamemaster.game.CustomGameManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.KeypointManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.prerequisite.PrerequisiteManager
import xyz.crunchmunch.mods.gamemaster.server.CustomClickHandler
import xyz.crunchmunch.mods.gamemaster.server.camera.CameraScene
import xyz.crunchmunch.mods.gamemaster.server.camera.keyframes.CameraKeyframe

object GameMasterRegistryKeys {
    // Custom game
    val CUSTOM_GAME = CustomGameManager.GAME_REGISTRY_KEY
    val GAME_METADATA = CustomGameManager.GAME_METADATA_REGISTRY_KEY

    // Animation
    val ANIMATION_TYPE = AnimatableManager.ANIMATION_TYPE_REGISTRY_KEY
    val ANIMATION = AnimatableManager.ANIMATION_REGISTRY_KEY
    val MODEL = AnimatableManager.MODEL_REGISTRY_KEY
    val ANIMATABLE_ENTITY = AnimatableManager.ANIMATABLE_ENTITY_REGISTRY_KEY

    val CUSTOM_CLICK_HANDLER = CustomClickHandler.REGISTRY_KEY

    // Camera
    @JvmField val CAMERA_SCENE = key<CameraScene>("camera/scene")
    @JvmField val CAMERA_KEYFRAME_TYPE = key<MapCodec<out CameraKeyframe>>("camera/keyframe_type")

    private fun <T : Any> key(path: String): ResourceKey<Registry<T>> {
        return ResourceKey.createRegistryKey(GameMaster.id(path))
    }

    // Timeline
    val TIMELINE = TimelineManager.REGISTRY_KEY
    val KEYPOINT_TYPE = KeypointManager.REGISTRY_KEY
    val PREREQUISITE_TYPE = PrerequisiteManager.REGISTRY_KEY

    fun init() {}
}
