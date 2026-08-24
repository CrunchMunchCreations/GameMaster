package xyz.crunchmunch.mods.gamemaster.commands

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import de.phyrone.brig.wrapper.DSLCommandNode
import de.phyrone.brig.wrapper.executesNoResult
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.IdentifierArgument
import net.minecraft.commands.arguments.ResourceArgument
import net.minecraft.commands.arguments.UuidArgument
import net.minecraft.commands.arguments.coordinates.RotationArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntitySpawnRequest
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import xyz.crunchmunch.mods.gamemaster.GameMasterRegistryKeys
import xyz.crunchmunch.mods.gamemaster.animator.AnimatableEntityType
import xyz.crunchmunch.mods.gamemaster.animator.AnimatableManager
import xyz.crunchmunch.mods.gamemaster.utils.execute
import xyz.crunchmunch.mods.gamemaster.utils.sendSuccess

fun DSLCommandNode<CommandSourceStack>.animatorCommands(context: CommandBuildContext) {
    literal("animator") {
        literal("create") {
            argument("model", IdentifierArgument.id()) {
                suggest {
                    for (key in context.lookupOrThrow(AnimatableManager.MODEL_REGISTRY_KEY).listElementIds()) {
                        suggest(key.identifier().toString())
                    }
                }

                argument("animations", IdentifierArgument.id()) {
                    suggest {
                        for (key in context.lookupOrThrow(AnimatableManager.ANIMATION_REGISTRY_KEY).listElementIds()) {
                            suggest(key.identifier().toString())
                        }
                    }

                    argument("pos", Vec3Argument.vec3(true)) {
                        executesNoResult { ctx ->
                            val modelHolder = context.lookupOrThrow(AnimatableManager.MODEL_REGISTRY_KEY).getOrThrow(ResourceKey.create(AnimatableManager.MODEL_REGISTRY_KEY, IdentifierArgument.getId(ctx, "model")))
                            val animationsHolder = context.lookupOrThrow(AnimatableManager.ANIMATION_REGISTRY_KEY).getOrThrow(ResourceKey.create(AnimatableManager.ANIMATION_REGISTRY_KEY, IdentifierArgument.getId(ctx, "animations")))
                            val pos = Vec3Argument.getVec3(ctx, "pos")

                            val animatable = AnimatableManager.create(modelHolder.value(), animationsHolder.value(), this.level, pos)
                            this.sendSuccess(Component.literal("Spawned in animatable entity with UUID ${animatable.rootDisplay.uuid}"), true)
                        }
                    }
                }
            }
        }

        literal("animate") {
            literal("entity") {
                argument("entity", EntityArgument.entity()) {
                    suggest { suggestAnimatables() }

                    argument("animation", StringArgumentType.string()) {
                        suggest { ctx ->
                            val uuid = UuidArgument.getUuid(ctx, "uuid")
                            val animatable = AnimatableManager.animatables[uuid] ?: return@suggest

                            for ((key, _) in animatable.animations.animations) {
                                suggest(key)
                            }
                        }

                        executesNoResult { ctx ->
                            val entity = EntityArgument.getEntity(ctx, "entity")
                            val animatable = AnimatableManager.animatables[entity.uuid]
                            val animationId = StringArgumentType.getString(ctx, "animation")

                            if (animatable == null) {
                                sendFailure(Component.literal("No entity exists by UUID ${entity.uuid}!"))
                                return@executesNoResult
                            }

                            if (!animatable.animations.animations.contains(animationId)) {
                                sendFailure(Component.literal("No animation exists by ID $animationId in ${animatable.animationsKey.identifier()}!"))
                                return@executesNoResult
                            }

                            animatable.queueAnimation(animationId)
                            sendSuccess(Component.literal("Queued animation $animationId for animatable ${entity.uuid}."), true)
                        }
                    }
                }
            }

            argument("uuid", UuidArgument.uuid()) {
                suggest { suggestAnimatables() }

                argument("animation", StringArgumentType.string()) {
                    suggest { ctx ->
                        val uuid = UuidArgument.getUuid(ctx, "uuid")
                        val animatable = AnimatableManager.animatables[uuid] ?: return@suggest

                        for ((key, _) in animatable.animations.animations) {
                            suggest(key)
                        }
                    }

                    executesNoResult { ctx ->
                        val uuid = UuidArgument.getUuid(ctx, "uuid")
                        val animatable = AnimatableManager.animatables[uuid]
                        val animationId = StringArgumentType.getString(ctx, "animation")

                        if (animatable == null) {
                            sendFailure(Component.literal("No entity exists by UUID $uuid!"))
                            return@executesNoResult
                        }

                        if (!animatable.animations.animations.contains(animationId)) {
                            sendFailure(Component.literal("No animation exists by ID $animationId in ${animatable.animationsKey.identifier()}!"))
                            return@executesNoResult
                        }

                        animatable.queueAnimation(animationId)
                        sendSuccess(Component.literal("Queued animation $animationId for animatable $uuid."), true)
                    }
                }
            }
        }

        literal("remove") {
            argument("uuid", UuidArgument.uuid()) {
                suggest { suggestAnimatables() }

                executesNoResult { ctx ->
                    val uuid = UuidArgument.getUuid(ctx, "uuid")
                    val animatable = AnimatableManager.animatables[uuid]

                    if (animatable == null) {
                        sendFailure(Component.literal("No entity exists by UUID $uuid!"))
                        return@executesNoResult
                    }

                    AnimatableManager.remove(animatable)
                    this.sendSuccess(Component.literal("Removed animatable entity ${uuid}!"), true)
                }
            }
        }

        literal("entity") {
            literal("create") {
                argument("entity_type", ResourceArgument.resource(context, GameMasterRegistryKeys.ANIMATABLE_ENTITY)) {
                    argument("position", Vec3Argument.vec3(true)) {
                        argument("rotation", RotationArgument.rotation()) {
                            execute {
                                val entityType = ResourceArgument.getResource(this, "entity_type", GameMasterRegistryKeys.ANIMATABLE_ENTITY)
                                val position = Vec3Argument.getVec3(this, "position")
                                val rotation = RotationArgument.getRotation(this, "rotation").getRotation(this.source)
                                this.source.spawnAnimatableEntity(entityType.value(), position, rotation)
                            }
                        }

                        execute {
                            val entityType = ResourceArgument.getResource(this, "entity_type", GameMasterRegistryKeys.ANIMATABLE_ENTITY)
                            val position = Vec3Argument.getVec3(this, "position")
                            this.source.spawnAnimatableEntity(entityType.value(), position, Vec2.ZERO)
                        }
                    }

                    execute {
                        val entityType = ResourceArgument.getResource(this, "entity_type", GameMasterRegistryKeys.ANIMATABLE_ENTITY)
                        this.source.spawnAnimatableEntity(entityType.value(), this.source.position, Vec2.ZERO)
                    }
                }
            }
        }
    }
}

private fun <E : LivingEntity> CommandSourceStack.spawnAnimatableEntity(entityType: AnimatableEntityType<E>, position: Vec3, rotation: Vec2): Component {
    val entity = entityType.baseEntityType.create(this.level, EntitySpawnRequest(EntitySpawnReason.COMMAND, true))
        ?: return Component.literal("Failed to create base entity ").withColor(TextColor.RED).append(entityType.baseEntityType.description)
    entity.snapTo(position, rotation.y, rotation.x)
    val animatable = entityType.builder(entity)

    this.level.addFreshEntity(entity)
    return Component.literal("Successfully created animatable entity $animatable under UUID ${entity.stringUUID}")
}

private fun SuggestionsBuilder.suggestAnimatables() {
    for ((uuid, animatable) in AnimatableManager.animatables) {
        suggest(uuid.toString(), LiteralMessage(animatable.modelKey.identifier().toString()))
    }
}
