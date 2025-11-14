package xyz.crunchmunch.mods.gamemaster.animator

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.registry.DynamicRegistries
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.fabricmc.fabric.api.event.registry.RegistryAttribute
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.GameMasterAttachments
import xyz.crunchmunch.mods.gamemaster.animator.animation.AnimationDefinitionType
import xyz.crunchmunch.mods.gamemaster.animator.animation.AnimationDefinitionTypes
import xyz.crunchmunch.mods.gamemaster.animator.animation.MultiAnimationDefinition
import java.util.*

object AnimatableManager {
    @JvmField val ANIMATION_REGISTRY_KEY: ResourceKey<Registry<MultiAnimationDefinition>> = ResourceKey.createRegistryKey(GameMaster.id("animations"))
    @JvmField val ANIMATION_TYPE_REGISTRY_KEY: ResourceKey<Registry<AnimationDefinitionType<*>>> = ResourceKey.createRegistryKey(GameMaster.id("animation_type"))
    @JvmField val MODEL_REGISTRY_KEY: ResourceKey<Registry<ModelDefinition>> = ResourceKey.createRegistryKey(GameMaster.id("models"))

    @JvmField val ANIMATION_TYPE_REGISTRY: Registry<AnimationDefinitionType<*>> = FabricRegistryBuilder.createDefaulted(ANIMATION_TYPE_REGISTRY_KEY, GameMaster.id("bedrock"))
        .attribute(RegistryAttribute.OPTIONAL)
        .buildAndRegister()

    private val animatablesMutable = Collections.synchronizedMap(mutableMapOf<UUID, AnimatableModel>())
    val animatables: Map<UUID, AnimatableModel> = Collections.unmodifiableMap(animatablesMutable)

    fun create(model: ModelDefinition, animation: MultiAnimationDefinition, level: ServerLevel, pos: Vec3): AnimatableModel {
        val animatable = AnimatableModel(model, animation, level)
        animatable.createNew(pos)
        this.animatablesMutable[animatable.rootDisplay.uuid] = animatable

        return animatable
    }

    fun remove(animatable: AnimatableModel) {
        animatable.remove()
        this.animatablesMutable.remove(animatable.rootDisplay.uuid)
    }

    init {
        DynamicRegistries.register(ANIMATION_REGISTRY_KEY, MultiAnimationDefinition.CODEC)
        DynamicRegistries.register(MODEL_REGISTRY_KEY, ModelDefinition.CODEC)

        AnimationDefinitionTypes.init()

        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity.hasAttached(AnimatorAttachments.MODEL_KEY) && entity.hasAttached(AnimatorAttachments.ANIMATIONS_KEY)) {
                // Register the animatable entity, but don't actually initialize it yet.
                // The initialization will happen in the next tick.
                if (!this.animatables.contains(entity.uuid)) {
                    val animatable = AnimatableModel(
                        world.registryAccess().lookupOrThrow(MODEL_REGISTRY_KEY)
                            .getValueOrThrow(entity.getAttachedOrThrow(AnimatorAttachments.MODEL_KEY)),
                        world.registryAccess().lookupOrThrow(ANIMATION_REGISTRY_KEY)
                            .getValueOrThrow(entity.getAttachedOrThrow(AnimatorAttachments.ANIMATIONS_KEY)),
                        world
                    )

                    this.animatablesMutable[entity.uuid] = animatable
                }
            }
        }

        ServerTickEvents.END_WORLD_TICK.register { level ->
            synchronized(this.animatables) {
                for ((uuid, animatable) in this.animatables) {
                    val existing = level.getEntity(uuid) ?: continue

                    if (existing !is Display)
                        continue

                    // check if our stored entity is actually still around
                    if (!animatable.isEntityLoaded()) {
                        if (!recursiveCheckIsFullyLoaded(existing)) {
                            // Defer, we need all the passengers.
                            continue
                        }

                        // We need to reload the entity.
                        animatable.loadFromExisting(existing)
                    }

                    animatable.tick()
                }
            }
        }
    }

    private fun recursiveCheckIsFullyLoaded(entity: Entity): Boolean {
        if (entity.hasAttached(GameMasterAttachments.HAS_PASSENGERS) && entity.passengers.isEmpty())
            return false

        for (passenger in entity.passengers) {
            if (!recursiveCheckIsFullyLoaded(passenger))
                return false
        }

        return true
    }

    fun init() {}
}