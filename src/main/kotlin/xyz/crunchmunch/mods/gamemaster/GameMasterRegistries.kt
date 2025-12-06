package xyz.crunchmunch.mods.gamemaster

import xyz.crunchmunch.mods.gamemaster.animator.AnimatableManager
import xyz.crunchmunch.mods.gamemaster.game.CustomGameManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.prerequisite.PrerequisiteManager

object GameMasterRegistries {
    // Custom Game
    val CUSTOM_GAME = CustomGameManager.GAME_REGISTRY

    // Animation
    val ANIMATION_TYPE = AnimatableManager.ANIMATION_TYPE_REGISTRY

    // Timeline
//    val KEYPOINT = KeypointManager.REGISTRY
    val PREREQUISITE = PrerequisiteManager.REGISTRY

    fun init() {}
}