package xyz.crunchmunch.mods.gamemaster.animator

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import org.joml.Vector3f
import xyz.crunchmunch.mods.gamemaster.GameMasterAttachments

abstract class AnimatableEntity<T : LivingEntity>(val type: AnimatableEntityType<T, out AnimatableEntity<T>>, val baseEntity: T) {
    protected val registry = this.baseEntity.registryAccess()
    val animatable = AnimatableModel(
        registry.lookupOrThrow(AnimatableManager.MODEL_REGISTRY_KEY).getValueOrThrow(type.model),
        registry.lookupOrThrow(AnimatableManager.ANIMATION_REGISTRY_KEY).getValueOrThrow(type.animations),
        baseEntity.level() as ServerLevel
    )
    lateinit var interaction: Interaction
        private set

    private val hasHeadRotation = this.animatable.model.hasPart("head")
    private val hasTorsoRotation = this.animatable.model.hasPart("torso")
    protected var enableHeadRotation = this.hasHeadRotation
    protected var enableTorsoRotation = this.hasTorsoRotation

    protected fun lockHeadRotation() {
        if (this.hasHeadRotation)
            this.enableHeadRotation = false
    }

    protected fun unlockHeadRotation() {
        if (this.hasHeadRotation)
            this.enableHeadRotation = true
    }

    protected fun lockTorsoRotation() {
        if (this.hasTorsoRotation)
            this.enableTorsoRotation = false
    }

    protected fun unlockTorsoRotation() {
        if (this.hasTorsoRotation)
            this.enableTorsoRotation = true
    }

    init {
        this.baseEntity.setAttached(GameMasterAttachments.ANIMATABLE_ENTITY_TYPE, this.type)
        this.baseEntity.setAttached(GameMasterAttachments.ANIMATABLE_ENTITY, this)
        this.baseEntity.isInvisible = true
        this.baseEntity.addEffect(MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 255, true, false, false))
        this.baseEntity.isPermanentlyInvulnerable = true
        this.baseEntity.isSilent = true
    }

    protected open fun tryLoadEntity() {
        this.animatable.createNew(this.baseEntity.position())
        this.animatable.rootDisplay.startRiding(this.baseEntity, true, true)

        if (this.type.hasHitbox) {
            val interaction = EntityTypes.INTERACTION.create(baseEntity.level(), EntitySpawnReason.LOAD)!!
            interaction.absSnapTo(baseEntity.x, baseEntity.y, baseEntity.z, baseEntity.yRot, baseEntity.xRot)
            interaction.response = true
            interaction.width = this.type.dimensions.width
            interaction.height = this.type.dimensions.height
            interaction.startRiding(this.baseEntity, true, true)

            this.baseEntity.level().addFreshEntity(interaction)
            this.interaction = interaction
        }
    }

    open fun tick() {
        if (!this.animatable.isEntityLoaded()) {
            if (this.baseEntity.hasAttached(GameMasterAttachments.HAS_PASSENGERS) && this.baseEntity.passengers.isEmpty())
                return

            this.tryLoadEntity()
        }

        this.animatable.rootDisplay.yRot = this.baseEntity.yRot
        this.animatable.tick()

        if (this.enableHeadRotation) {
            val headDisplay = this.getHeadDisplay()
            headDisplay?.setAttached(AnimatorAttachments.OFFSET_ROTATIONS, Vector3f(this.baseEntity.xRot, this.baseEntity.yHeadRot - this.baseEntity.yRot, 0f))
        }

        if (this.enableTorsoRotation) {
            val torsoDisplay = this.getTorsoDisplay()
            torsoDisplay?.setAttached(AnimatorAttachments.OFFSET_ROTATIONS, Vector3f(0f, this.baseEntity.yBodyRot - this.baseEntity.yRot, 0f))
        }

        if (this.baseEntity.removalReason != null) {
            this.onRemoved(this.baseEntity.removalReason!!)
            return
        }
    }

    open fun onRemoved(reason: Entity.RemovalReason) {
        this.animatable.remove(true)
        if (this.type.hasHitbox && this::interaction.isInitialized)
            this.interaction.remove(Entity.RemovalReason.DISCARDED)
    }

    protected open fun getHeadDisplay(): Display? {
        return this.animatable.getPartById("head")
    }

    protected open fun getTorsoDisplay(): Display? {
        return this.animatable.getPartById("torso")
    }
}
