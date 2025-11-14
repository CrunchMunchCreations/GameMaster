package xyz.crunchmunch.mods.gamemaster.commands

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import de.phyrone.brig.wrapper.DSLCommandNode
import de.phyrone.brig.wrapper.executesNoResult
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.commands.arguments.UuidArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import xyz.crunchmunch.mods.gamemaster.animator.AnimatableManager
import xyz.crunchmunch.mods.gamemaster.utils.sendSuccess

fun DSLCommandNode<CommandSourceStack>.animatorCommands(context: CommandBuildContext) {
    literal("animator") {
        literal("create") {
            argument("model", ResourceLocationArgument.id()) {
                suggest {
                    for (key in context.lookupOrThrow(AnimatableManager.MODEL_REGISTRY_KEY).listElementIds()) {
                        suggest(key.location().toString())
                    }
                }

                argument("animations", ResourceLocationArgument.id()) {
                    suggest {
                        for (key in context.lookupOrThrow(AnimatableManager.ANIMATION_REGISTRY_KEY).listElementIds()) {
                            suggest(key.location().toString())
                        }
                    }

                    argument("pos", Vec3Argument.vec3(true)) {
                        executesNoResult { ctx ->
                            val modelHolder = context.lookupOrThrow(AnimatableManager.MODEL_REGISTRY_KEY).getOrThrow(ResourceKey.create(AnimatableManager.MODEL_REGISTRY_KEY, ResourceLocationArgument.getId(ctx, "model")))
                            val animationsHolder = context.lookupOrThrow(AnimatableManager.ANIMATION_REGISTRY_KEY).getOrThrow(ResourceKey.create(AnimatableManager.ANIMATION_REGISTRY_KEY, ResourceLocationArgument.getId(ctx, "animations")))
                            val pos = Vec3Argument.getVec3(ctx, "pos")

                            val animatable = AnimatableManager.create(modelHolder.value(), animationsHolder.value(), this.level, pos)
                            this.sendSuccess(Component.literal("Spawned in animatable entity with UUID ${animatable.rootDisplay.uuid}"), true)
                        }
                    }
                }
            }
        }

        literal("animate") {
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
                            sendFailure(Component.literal("No animation exists by ID $animationId in ${animatable.animationsKey.location()}!"))
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
    }
}

private fun SuggestionsBuilder.suggestAnimatables() {
    for ((uuid, animatable) in AnimatableManager.animatables) {
        suggest(uuid.toString(), LiteralMessage(animatable.modelKey.location().toString()))
    }
}