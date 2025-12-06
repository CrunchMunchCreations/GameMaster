package xyz.crunchmunch.mods.gamemaster.game.timeline

import com.google.common.collect.Queues
import it.unimi.dsi.fastutil.Stack
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.TimelineKeypoint
import java.util.*

data class TimelineContext(
    val server: MinecraftServer,
    val timeline: Timeline,
) {
    private val isPaused = atomic(false)
    private val channels: MutableList<Channel<Unit>> = Collections.synchronizedList(mutableListOf())

    val keypointsQueue: Stack<Deque<TimelineKeypoint<*>>> = ObjectArrayList()
    val remainingKeypoints: Deque<TimelineKeypoint<*>>
        get() = this.keypointsQueue.top()

    val passedKeypoints: MutableList<TimelineKeypoint<*>> = Collections.synchronizedList(mutableListOf())

    fun pushNewKeypoints() {
        this.keypointsQueue.push(Queues.synchronizedDeque(Queues.newArrayDeque()))
    }

    fun popKeypoints() {
        this.keypointsQueue.pop()
    }

    fun broadcastToGameMasters(executable: (ServerPlayer) -> Unit) {
        for (player in this.server.playerList.players) {
            if (player.hasPermissions(Commands.LEVEL_GAMEMASTERS)) {
                executable.invoke(player)
            }
        }
    }

    fun broadcastToGameMasters(message: Component) {
        broadcastToGameMasters { player -> player.displayClientMessage(message, false) }
    }

    suspend fun checkPause() {
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
}
