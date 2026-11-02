package xyz.crunchmunch.mods.gamemaster

import xyz.crunchmunch.mods.gamemaster.animator.AnimatableManager
import xyz.crunchmunch.mods.gamemaster.game.CustomGameManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.prerequisite.PrerequisiteManager
import xyz.crunchmunch.mods.gamemaster.server.CustomClickHandler

object GameMasterRegistries {
    // Custom Game
    val CUSTOM_GAME = CustomGameManager.GAME_REGISTRY

    // Animation
    val ANIMATION_TYPE = AnimatableManager.ANIMATION_TYPE_REGISTRY
    val ANIMATABLE_ENTITY = AnimatableManager.ANIMATABLE_ENTITY_REGISTRY

    // Timeline
//    val KEYPOINT = KeypointManager.REGISTRY
    val PREREQUISITE = PrerequisiteManager.REGISTRY

    val CUSTOM_CLICK_HANDLER = CustomClickHandler.REGISTRY

    fun init() {}
}
