package xyz.crunchmunch.mods.gamemaster

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.fabricmc.fabric.api.event.registry.RegistryAttribute
import xyz.crunchmunch.mods.gamemaster.animator.AnimatableManager
import xyz.crunchmunch.mods.gamemaster.game.CustomGameManager
import xyz.crunchmunch.mods.gamemaster.game.marker.GameMarkerManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.GameMasterKeypoints
import xyz.crunchmunch.mods.gamemaster.game.timeline.prerequisite.GameMasterPrerequisites
import xyz.crunchmunch.mods.gamemaster.server.CustomClickHandler

object GameMasterRegistries {
    // Game Markers
    @JvmField val GAME_MARKER_TYPE = GameMarkerManager.TYPE_REGISTRY

    // Custom Game
    @JvmField val CUSTOM_GAME = CustomGameManager.GAME_REGISTRY

    // Animation
    @JvmField val ANIMATION_TYPE = AnimatableManager.ANIMATION_TYPE_REGISTRY
    @JvmField val ANIMATABLE_ENTITY = AnimatableManager.ANIMATABLE_ENTITY_REGISTRY

    // Timeline
    @JvmField val KEYPOINT = GameMasterKeypoints.REGISTRY
    @JvmField val PREREQUISITE = GameMasterPrerequisites.REGISTRY

    @JvmField val CUSTOM_CLICK_HANDLER = CustomClickHandler.REGISTRY

    // Camera
    @JvmField val CAMERA_KEYFRAME_TYPE = FabricRegistryBuilder.create(GameMasterRegistryKeys.CAMERA_KEYFRAME_TYPE)
        .attribute(RegistryAttribute.OPTIONAL)
        .buildAndRegister()

    fun init() {}
}
