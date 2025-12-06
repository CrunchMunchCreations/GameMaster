package xyz.crunchmunch.mods.gamemaster

import xyz.crunchmunch.mods.gamemaster.animator.AnimatableManager
import xyz.crunchmunch.mods.gamemaster.game.CustomGameManager

object GameMasterRegistryKeys {
    // Custom game
    val CUSTOM_GAME = CustomGameManager.GAME_REGISTRY_KEY
    val GAME_METADATA = CustomGameManager.GAME_METADATA_REGISTRY_KEY

    // Animation
    val ANIMATION_TYPE = AnimatableManager.ANIMATION_TYPE_REGISTRY_KEY
    val ANIMATION = AnimatableManager.ANIMATION_REGISTRY_KEY
    val MODEL = AnimatableManager.MODEL_REGISTRY_KEY

    // Timeline
//    val TIMELINE = TimelineManager.REGISTRY_KEY
//    val KEYPOINT_TYPE = KeypointManager.REGISTRY_KEY
//    val PREREQUISITE_TYPE = PrerequisiteManager.REGISTRY_KEY

    fun init() {}
}