package xyz.crunchmunch.mods.gamemaster.animator

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import xyz.crunchmunch.mods.gamemaster.GameMasterAttachments

abstract class AnimatableEntity<T : LivingEntity>(val type: AnimatableEntityType<T>, val baseEntity: T) {
    protected val registry = this.baseEntity.registryAccess()
    val animatable = AnimatableModel(
        registry.lookupOrThrow(AnimatableManager.MODEL_REGISTRY_KEY).getValueOrThrow(type.model),
        registry.lookupOrThrow(AnimatableManager.ANIMATION_REGISTRY_KEY).getValueOrThrow(type.animations),
        baseEntity.level() as ServerLevel
    )
    lateinit var interaction: Interaction
        private set

    init {
        this.baseEntity.setAttached(GameMasterAttachments.ANIMATABLE_ENTITY_TYPE, this.type)
        this.baseEntity.setAttached(GameMasterAttachments.ANIMATABLE_ENTITY, this)
        this.baseEntity.isInvisible = true
        this.baseEntity.addEffect(MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 255, true, false, false))
        this.baseEntity.isInvulnerable = true
    }

    protected open fun tryLoadEntity() {
        this.animatable.createNew(this.baseEntity.position())
        this.animatable.rootDisplay.startRiding(this.baseEntity, true, true)

        val interaction = EntityTypes.INTERACTION.create(baseEntity.level(), EntitySpawnReason.LOAD)!!
        interaction.snapTo(baseEntity.x, baseEntity.y, baseEntity.z, baseEntity.yRot, baseEntity.xRot)
        interaction.response = true
        interaction.width = this.type.dimensions.width
        interaction.height = this.type.dimensions.height
        interaction.startRiding(this.baseEntity, true, true)

        this.baseEntity.level().addFreshEntity(interaction)
        this.interaction = interaction
    }

    open fun tick() {
        if (!this.animatable.isEntityLoaded()) {
            if (this.baseEntity.hasAttached(GameMasterAttachments.HAS_PASSENGERS) && this.baseEntity.passengers.isEmpty())
                return

            this.tryLoadEntity()
        }

        if (this.baseEntity.removalReason != null) {
            this.onRemoved(this.baseEntity.removalReason!!)
            return
        }
    }

    open fun onRemoved(reason: Entity.RemovalReason) {
        this.animatable.remove(false)
        this.interaction.remove(Entity.RemovalReason.DISCARDED)
    }
}
