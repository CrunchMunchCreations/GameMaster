package xyz.crunchmunch.mods.gamemaster.game.timeline

import kotlinx.coroutines.*
import net.fabricmc.fabric.api.event.registry.DynamicRegistries
import net.minecraft.ChatFormatting
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.game.GameManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.AwaitGameMasterKeypoint
import java.util.*
import java.util.concurrent.Executors

object TimelineManager {
    val REGISTRY_KEY: ResourceKey<Registry<Timeline>> = ResourceKey.createRegistryKey(GameMaster.id("timeline"))
    val dispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher() + CoroutineName("GameMaster Timeline Dispatcher")
    val scope = CoroutineScope(dispatcher)

    private val contexts: MutableMap<ResourceKey<Timeline>, TimelineContext> = Collections.synchronizedMap(mutableMapOf())

    fun init() {
        DynamicRegistries.register(REGISTRY_KEY, Timeline.CODEC)
    }

    fun start(server: MinecraftServer, key: ResourceKey<Timeline>, manager: GameManager<*, *, *>) {
        if (this.contexts.contains(key)) {
            throw IllegalArgumentException("This timeline is currently already running!")
        }

        val timeline = server.registryAccess().lookupOrThrow(REGISTRY_KEY).getValueOrThrow(key)

        for (prerequisite in timeline.prerequisites) {
            if (!prerequisite.meetsPrerequisite(server)) {
                for (player in server.playerList.players) {
                    if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                        player.sendSystemMessage(Component.literal("Prerequisite not met! Timeline cannot start!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                        player.sendSystemMessage(prerequisite.prerequisiteFailedMessage)
                    }
                }

                throw IllegalStateException("Prerequisite not met! ${prerequisite.prerequisiteFailedMessage.string}")
            }
        }

        val context = TimelineContext(server, timeline, manager)
        this.contexts[key] = context

        // Load all the timeline's keypoints into the queue
        context.pushNewKeypoints()
        context.remainingKeypoints.addAll(timeline.keypoints)

        runBlocking {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                while (!context.keypointsQueue.isEmpty && context.isRunning) {
                    try {
                        context.checkPause()

                        if (context.remainingKeypoints.isEmpty()) {
                            context.popKeypoints()
                            continue
                        }

                        val keypoint = context.remainingKeypoints.pop()

                        if (!context.skippedKeypoints.contains(keypoint)) {
                            if (context.timeline.type == Timeline.Type.CONFIRMATIONAL) {
                                val awaitKeypoint = AwaitGameMasterKeypoint(Component.empty())
                                context.remainingKeypoints.addFirst(awaitKeypoint)
                                awaitKeypoint.execute(context)
                            }

                            keypoint.execute(context)
                        }
                    } catch (e: Exception) {
                        context.pause()
                        GameMaster.logger.error("An error occurred in timeline ${key.identifier()}!", e)
                        context.broadcastToGameMasters(Component.literal("An error occurred while executing timeline ${key.identifier()}! Timeline has been paused immediately for safety. Error:").withStyle(ChatFormatting.RED).append(e.message ?: e.localizedMessage))
                    }
                }

                contexts.remove(key)
            }
        }
    }

    fun stop(key: ResourceKey<Timeline>) {
        val context = this.getContext(key) ?: throw IllegalArgumentException("This timeline is not active!")

        context.stop()
    }

    fun isActive(key: ResourceKey<Timeline>): Boolean {
        val context = this.getContext(key) ?: return false
        return context.isRunning
    }

    fun getContext(key: ResourceKey<Timeline>): TimelineContext? {
        return this.contexts[key]
    }

    fun findWaitingContexts(): Collection<ResourceKey<Timeline>> {
        return this.contexts.filterValues { it.remainingKeypoints.firstOrNull() is AwaitGameMasterKeypoint }.keys
    }

    fun proceedContext(key: ResourceKey<Timeline>, player: ServerPlayer) {
        val context = this.getContext(key) ?: return

        runBlocking {
            (context.remainingKeypoints.firstOrNull() as? AwaitGameMasterKeypoint)
                ?.channel?.send(player)
        }
    }
}
