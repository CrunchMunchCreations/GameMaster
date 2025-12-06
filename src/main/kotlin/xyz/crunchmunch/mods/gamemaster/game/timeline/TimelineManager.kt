package xyz.crunchmunch.mods.gamemaster.game.timeline

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.fabricmc.fabric.api.event.registry.DynamicRegistries
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.AwaitGameMasterKeypoint
import java.util.*

object TimelineManager {
    val REGISTRY_KEY: ResourceKey<Registry<Timeline>> = ResourceKey.createRegistryKey(GameMaster.id("timeline"))

    val contexts: MutableMap<ResourceLocation, TimelineContext> = Collections.synchronizedMap(mutableMapOf())

    fun init() {
        DynamicRegistries.register(REGISTRY_KEY, Timeline.CODEC)
    }

    fun start(server: MinecraftServer, key: ResourceKey<Timeline>) {
        if (this.contexts.contains(key.location())) {
            throw IllegalArgumentException("This timeline is currently already running!")
        }

        val timeline = server.registryAccess().lookupOrThrow(REGISTRY_KEY).getValueOrThrow(key)

        for (prerequisite in timeline.prerequisites) {
            if (!prerequisite.meetsPrerequisite) {
                throw IllegalStateException("Prerequisite not met! ${prerequisite.prerequisiteFailedMessage.string}")
            }
        }

        val context = TimelineContext(server, timeline)
        this.contexts[key.location()] = context

        // Load all the timeline's keypoints into the queue
        context.pushNewKeypoints()
        context.remainingKeypoints.addAll(timeline.keypoints)

        runBlocking {
            coroutineScope {
                launch {
                    while (!context.keypointsQueue.isEmpty) {
                        context.checkPause()

                        if (context.remainingKeypoints.isEmpty()) {
                            context.popKeypoints()
                            continue
                        }

                        val keypoint = context.remainingKeypoints.peek()

                        if (context.timeline.type == Timeline.Type.CONFIRMATIONAL) {
                            AwaitGameMasterKeypoint(Component.empty()).execute(context)
                        }

                        keypoint.execute(context)
                        context.remainingKeypoints.pop()
                    }

                    contexts.remove(key.location())
                }
            }
        }
    }
}