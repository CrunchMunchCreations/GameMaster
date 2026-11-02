package xyz.crunchmunch.mods.gamemaster.server.camera

import com.mojang.brigadier.context.CommandContext
import de.phyrone.brig.wrapper.DSLCommandNode
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.ResourceKeyArgument
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import xyz.crunchmunch.mods.gamemaster.GameMasterRegistryKeys
import xyz.crunchmunch.mods.gamemaster.utils.DynamicCommandExceptions

fun DSLCommandNode<CommandSourceStack>.cameraCommands() {
    literal("camera") {
        literal("start") {
            fun startCamera(context: CommandContext<CommandSourceStack>, scene: CameraScene, targets: Collection<Entity> = listOf(context.source.entityOrException)): Int {
                val targets = targets.filterIsInstance<ServerPlayer>()

                if (targets.isEmpty()) {
                    context.source.sendFailure(Component.literal(""))
                    return 0
                }

                CameraManager.beginScene(context.source.server, scene, targets)

                return 1
            }

            fun startCamera(context: CommandContext<CommandSourceStack>, targets: Collection<Entity> = listOf(context.source.entityOrException)): Int {
                val sceneId = ResourceKeyArgument.getRegistryKey(context, "scene_id", GameMasterRegistryKeys.CAMERA_SCENE, DynamicCommandExceptions.RESOURCE_KEY)
                val scene = context.source.registryAccess().get(sceneId).map(Holder<CameraScene>::value).orElse(null)

                if (scene == null) {
                    context.source.sendFailure(Component.literal("No scene exists by ID \"$sceneId\"!"))
                    return 0
                }

                return startCamera(context, scene, targets)
            }

            argument("scene_id", ResourceKeyArgument.key(GameMasterRegistryKeys.CAMERA_SCENE)) {
                argument("targets", EntityArgument.entities()) {
                    executes {
                        startCamera(it, EntityArgument.getEntities(it, "targets"))
                    }
                }

                executes {
                    startCamera(it)
                }
            }
        }

        literal("exit") {
            executes {
                if (player == null) {
                    sendFailure(Component.literal("You are not a player!"))
                    return@executes 0
                }

                if (!player!!.hasAttached(CameraManager.ACTIVE_SCENE_ATTACHMENT)) {
                    sendFailure(Component.literal("You are not currently in a scene!"))
                    return@executes 0
                }

                playerOrException.removeAttached(CameraManager.ACTIVE_SCENE_ATTACHMENT)
                playerOrException.setCamera(null)
                sendSystemMessage(Component.literal("Exited out of camera scene."))

                1
            }
        }
    }
}
