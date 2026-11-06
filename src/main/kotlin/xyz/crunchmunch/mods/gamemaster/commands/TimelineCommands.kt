package xyz.crunchmunch.mods.gamemaster.commands

import de.phyrone.brig.wrapper.DSLCommandNode
import de.phyrone.brig.wrapper.executesNoResult
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.ResourceKeyArgument
import net.minecraft.network.chat.Component
import xyz.crunchmunch.mods.gamemaster.GameMasterRegistryKeys
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineManager
import xyz.crunchmunch.mods.gamemaster.utils.DynamicCommandExceptions
import xyz.crunchmunch.mods.gamemaster.utils.sendSuccess

fun DSLCommandNode<CommandSourceStack>.timelineCommands() {
    literal("timeline") {
        literal("start") {
            argument("id", ResourceKeyArgument.key(GameMasterRegistryKeys.TIMELINE)) {
                executesNoResult { ctx ->
                    val key = ResourceKeyArgument.getRegistryKey(ctx, "id", GameMasterRegistryKeys.TIMELINE, DynamicCommandExceptions.RESOURCE_KEY)

                    if (TimelineManager.isActive(key)) {
                        sendFailure(Component.literal("This timeline is already active!"))
                        return@executesNoResult
                    }

                    TimelineManager.start(server, key)
                    sendSuccess(Component.literal("Started timeline ${key.identifier()}."))
                }
            }
        }

        literal("stop") {
            argument("id", ResourceKeyArgument.key(GameMasterRegistryKeys.TIMELINE)) {
                executesNoResult { ctx ->
                    val key = ResourceKeyArgument.getRegistryKey(ctx, "id", GameMasterRegistryKeys.TIMELINE, DynamicCommandExceptions.RESOURCE_KEY)

                    if (!TimelineManager.isActive(key)) {
                        sendFailure(Component.literal("This timeline is not active!"))
                        return@executesNoResult
                    }

                    TimelineManager.stop(key)
                    sendSuccess(Component.literal("Stopped timeline ${key.identifier()}."))
                }
            }
        }

        literal("pause") {
            argument("id", ResourceKeyArgument.key(GameMasterRegistryKeys.TIMELINE)) {
                executesNoResult { ctx ->
                    val key = ResourceKeyArgument.getRegistryKey(ctx, "id", GameMasterRegistryKeys.TIMELINE, DynamicCommandExceptions.RESOURCE_KEY)
                    val context = TimelineManager.getContext(key)

                    if (context == null) {
                        sendFailure(Component.literal("This timeline is not active!"))
                        return@executesNoResult
                    }

                    if (!context.isPaused()) {
                        context.pause()
                        sendSuccess(Component.literal("Paused timeline ${key.identifier()}."))
                    } else {
                        sendFailure(Component.literal("This timeline is already paused!"))
                    }
                }
            }
        }

        literal("resume") {
            alias("unpause")
            argument("id", ResourceKeyArgument.key(GameMasterRegistryKeys.TIMELINE)) {
                executesNoResult { ctx ->
                    val key = ResourceKeyArgument.getRegistryKey(ctx, "id", GameMasterRegistryKeys.TIMELINE, DynamicCommandExceptions.RESOURCE_KEY)
                    val context = TimelineManager.getContext(key)

                    if (context == null) {
                        sendFailure(Component.literal("This timeline is not active!"))
                        return@executesNoResult
                    }

                    if (context.isPaused()) {
                        context.resume()
                        sendSuccess(Component.literal("Resumed timeline ${key.identifier()}."))
                    } else {
                        sendFailure(Component.literal("This timeline is not paused!"))
                    }
                }
            }
        }

        literal("status") {
            argument("id", ResourceKeyArgument.key(GameMasterRegistryKeys.TIMELINE)) {
                executesNoResult { ctx ->
                    val key = ResourceKeyArgument.getRegistryKey(ctx, "id", GameMasterRegistryKeys.TIMELINE, DynamicCommandExceptions.RESOURCE_KEY)
                    val context = TimelineManager.getContext(key)

                    if (context == null) {
                        val timeline = registryAccess().getOrThrow(key)
                        sendSystemMessage(Component.empty()
                            .append(Component.literal("Timeline: ").withStyle(ChatFormatting.BOLD))
                            .append("${key.identifier()}")
                        )
                        sendSystemMessage(Component.empty()
                            .append(" - Status:")
                            .append(Component.literal(" Inactive").withStyle(ChatFormatting.DARK_RED))
                        )

                        sendSystemMessage(Component.literal(" - Prerequisites:"))

                        for (prerequisite in timeline.value().prerequisites) {
                            val prefix = if (prerequisite.meetsPrerequisite(server))
                                Component.literal("✓").withStyle(ChatFormatting.GREEN)
                            else
                                Component.literal("✗").withStyle(ChatFormatting.RED)

                            sendSystemMessage(Component.empty()
                                .append(prefix)
                                .append(" - ")
                                .append("$prerequisite")
                            )
                        }

                        return@executesNoResult
                    }

                    sendSystemMessage(Component.empty()
                        .append(Component.literal("Timeline: ").withStyle(ChatFormatting.BOLD))
                        .append("${key.identifier()}")
                    )
                    sendSystemMessage(Component.empty()
                        .append(" - Status:")
                        .apply {
                            if (context.isRunning) {
                                if (context.isPaused())
                                    append(Component.literal("Paused").withStyle(ChatFormatting.RED))
                                else
                                    append(Component.literal("Active").withStyle(ChatFormatting.GREEN))
                            } else {
                                append(Component.literal("Stopped/Stopping").withStyle(ChatFormatting.RED))
                            }
                        }
                    )

                    sendSystemMessage(Component.literal("Active Keypoints").withStyle(ChatFormatting.BOLD))
                    for (keypoint in context.remainingKeypoints) {
                        sendSystemMessage(Component.literal("$keypoint"))
                    }
                }
            }
        }
    }
}
