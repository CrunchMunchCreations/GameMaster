package xyz.crunchmunch.mods.gamemaster.game.timeline

import com.google.common.collect.Queues
import it.unimi.dsi.fastutil.Stack
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import xyz.crunchmunch.mods.gamemaster.game.GameManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.MultiKeypointHolder
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.TimelineKeypoint
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.timeline.CategorizeTimelineKeypoint
import java.util.*

data class TimelineContext(
    val server: MinecraftServer,
    val timeline: Timeline,
    val gameManager: GameManager<*, *, *>,
    val parent: TimelineContext? = null,
) {
    private val isPaused = atomic(false)
    private val channels: MutableList<Channel<Unit>> = Collections.synchronizedList(mutableListOf())

    private val isActive = atomic(true)

    val isRunning: Boolean
        get() = this.isActive.value

    val keypointsQueue: Stack<Deque<TimelineKeypoint<*>>>
        field = ObjectArrayList()
    val remainingKeypoints: Deque<TimelineKeypoint<*>>
        get() = this.keypointsQueue.top()

    val skippedKeypoints: List<TimelineKeypoint<*>>
        field = mutableListOf()

    private val categorizedKeypoints = this.timeline.run {
        val categorized = mutableMapOf<String, CategorizeTimelineKeypoint>()

        fun readKeypoints(keypoints: List<TimelineKeypoint<*>>) {
            for (keypoint in keypoints) {
                if (keypoint is CategorizeTimelineKeypoint) {
                    if (categorized.contains(keypoint.category))
                        throw IllegalStateException("Multiple keypoint categories share the name ${keypoint.category}!")

                    categorized[keypoint.category] = keypoint
                }

                if (keypoint is MultiKeypointHolder) {
                    readKeypoints(keypoint.keypoints)
                }
            }
        }

        readKeypoints(this.keypoints)

        categorized
    }

    fun isPaused(): Boolean = this.isPaused.value

    fun pushNewKeypoints() {
        this.keypointsQueue.push(Queues.synchronizedDeque(Queues.newArrayDeque()))
    }

    fun popKeypoints() {
        this.keypointsQueue.pop()
    }

    fun broadcastToGameMasters(executable: (ServerPlayer) -> Unit) {
        for (player in this.server.playerList.players) {
            if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                executable.invoke(player)
            }
        }
    }

    fun broadcastToGameMasters(message: Component) {
        broadcastToGameMasters { player -> player.sendSystemMessage(message, false) }
    }

    suspend fun checkPause() {
        this.parent?.checkPause()

        if (isPaused.value) {
            val channel = Channel<Unit>()
            this.channels.add(channel)

            channel.receive()
        }
    }

    fun pause() {
        this.isPaused.lazySet(true)
    }

    fun resume() {
        this.isPaused.lazySet(false)

        synchronized(this.channels) {
            runBlocking {
                for (channel in channels) {
                    channel.send(Unit)
                }
            }

            this.channels.clear()
        }
    }

    fun stop() {
        this.pause()
        this.broadcastToGameMasters(Component.literal("Active timeline has been stopped!"))
        this.isActive.lazySet(false)
    }

    val categories: Collection<String> = this.categorizedKeypoints.keys

    fun pushCategory(categoryId: String) {
        val category = this.categorizedKeypoints[categoryId] ?: throw IllegalArgumentException("Unknown category ID $categoryId!")

        if (!this.isPaused())
            throw IllegalStateException("Cannot push category while timeline is actively running!")

        this.pushNewKeypoints()
        this.remainingKeypoints.addAll(category.keypoints)
    }

    fun skipCategory(categoryId: String) {
        val category = this.categorizedKeypoints[categoryId] ?: throw IllegalArgumentException("Unknown category ID $categoryId!")
        this.skippedKeypoints.add(category)
    }

    fun fork(): TimelineContext {
        return TimelineContext(server, timeline, gameManager, this)
    }

    val root: TimelineContext
        get() {
            var root = this

            while (root.parent != null) {
                root = root.parent
            }

            return root
        }
}
