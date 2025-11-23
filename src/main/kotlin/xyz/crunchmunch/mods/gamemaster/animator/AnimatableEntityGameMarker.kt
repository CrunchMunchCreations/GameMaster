package xyz.crunchmunch.mods.gamemaster.animator

import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import org.joml.Vector3f
import xyz.crunchmunch.mods.gamemaster.GameMasterAttachments
import xyz.crunchmunch.mods.gamemaster.game.marker.GameMarker
import xyz.crunchmunch.mods.gamemaster.game.marker.GameMarkerType
import xyz.crunchmunch.mods.gamemaster.utils.leftRotation

abstract class AnimatableEntityGameMarker<D : AnimatableMarkerData>(type: GameMarkerType<out AnimatableEntityGameMarker<D>, D>, entity: Entity, data: D) : GameMarker<D>(type, entity, data) {
    private val registry = entity.level().registryAccess()
    var animatable = AnimatableModel(
        registry.lookupOrThrow(AnimatableManager.MODEL_REGISTRY_KEY).getValueOrThrow(data.model),
        registry.lookupOrThrow(AnimatableManager.ANIMATION_REGISTRY_KEY).getValueOrThrow(data.animations),
        entity.level() as ServerLevel
    )

    private var hasLogged = false

    open fun tryLocateEntity() {
        val entity = this.entity

        if (entity is Display) {
            if (entity.getAttached(AnimatorAttachments.MODEL_KEY) != this.data.model || entity.getAttached(AnimatorAttachments.ANIMATIONS_KEY) != this.data.animations) {
                animatable.remove(false)
                animatable.createNew(entity.position(), entity)
            } else {
                animatable.loadFromExisting(entity)
                AnimatableManager.respawn(animatable)
            }
        } else {
            throw IllegalStateException("Game marker entity ${entity.level().dimension().location()}/${entity.uuid} is not using a display entity!")
//            if (!hasLogged) {
//                GameMaster.logger.warn("[GameMaster] Game marker entity ${entity.level().dimension().location()}/${entity.uuid} is not using a display, this may cause weird problems with chunk unloading.")
//                hasLogged = true
//            }
//
//            val associated = entity.getAttached(AnimatorAttachments.ASSOCIATED_ENTITY)
//
//            if (associated != null) {
//
//            } else {
//                animatable.createNew(entity.position())
//
//            }
        }
    }

    override fun tick() {
        super.tick()

        if (!this.isUnloaded && !this.animatable.isEntityLoaded()) {
            if (this.entity.hasAttached(GameMasterAttachments.HAS_PASSENGERS) && this.entity.passengers.isEmpty())
                return

            this.tryLocateEntity()
        }

        val entity = this.entity

        if (entity is Display) {
            val yRot = entity.leftRotation.getEulerAnglesXYZ(Vector3f()).y * Mth.RAD_TO_DEG
            if (yRot != 0f) {
                this.entity.yRot = yRot
            }
        }

        if (!this.isUnloaded && this.animatable.isEntityLoaded()) {
            this.animatable.tick()
        }
    }
}