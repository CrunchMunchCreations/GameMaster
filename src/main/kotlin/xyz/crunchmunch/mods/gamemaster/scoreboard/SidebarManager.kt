package xyz.crunchmunch.mods.gamemaster.scoreboard

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.numbers.BlankFormat
import net.minecraft.network.chat.numbers.NumberFormat
import net.minecraft.network.protocol.game.ClientboundResetScorePacket
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import org.jetbrains.annotations.ApiStatus
import xyz.crunchmunch.mods.gamemaster.GameMaster
import java.util.*

/**
 * Handles sidebars, also known as scoreboards.
 */
abstract class SidebarManager(
    displayName: Lazy<Component>,
    renderType: ObjectiveCriteria.RenderType = ObjectiveCriteria.RenderType.INTEGER,
    displayAutoUpdate: Boolean = true,
    private val numberFormat: NumberFormat = BlankFormat.INSTANCE
) {
    private val internalScoreboard = Scoreboard()
    private val internalObjective = lazy {
        internalScoreboard.addObjective("gamemaster_internal_sidebar",
            ObjectiveCriteria.DUMMY,
            displayName.value, renderType, displayAutoUpdate, numberFormat
        )
    }

    protected val objective: Objective
        get() = internalObjective.value

    /**
     * The list of players that should be receiving this sidebar.
     */
    open val playerList: Collection<ServerPlayer>
        get() = GameMaster.server.playerList.players

    /**
     * The players currently being tracked by the sidebar. Mainly used internally.
     */
    protected val trackedPlayers = Collections.synchronizedSet(mutableSetOf<UUID>())

    /**
     * All the globally available lines in this sidebar.
     */
    val lines = SidebarLines(this)

    /**
     * All the available per-player lines.
     */
    val additionalLines = Collections.synchronizedMap(mutableMapOf<UUID, SidebarLines>())

    private var isDirty = false

    /**
     * Marks the sidebar as visible for the players.
     */
    var isVisible = false
        set(value) {
            field = value

            val packet = ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, if (value) objective else null)

            for (player in playerList) {
                player.connection.send(packet)
            }
        }

    init {
        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            // Removing directly from the set, just in case.
            trackedPlayers.remove(handler.player.uuid)
        }

        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in playerList) {
                if (!trackedPlayers.contains(player.uuid)) {
                    addDisplayTo(player)
                    trackedPlayers.add(player.uuid)
                }
            }

            if (isDirty && isVisible) {
                this.doUpdateAll(true)
            }
        }
    }

    fun getAdditionalLinesFor(uuid: UUID): SidebarLines {
        return this.additionalLines.computeIfAbsent(uuid) { SidebarLines(this) }
    }

    /**
     * Adds the sidebar display to the player's client.
     */
    fun addDisplayTo(player: ServerPlayer) {
        val scoreboardPacket = ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD)
        player.connection.send(scoreboardPacket)

        if (isVisible) {
            val displayPacket = ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective)
            player.connection.send(displayPacket)

            this.doUpdatePlayer(player, true)
        }
    }

    /**
     * Removes the sidebar display from the player's client. Must be called before switching to
     * another sidebar, as otherwise it may conflict.
     */
    fun removeDisplayFrom(player: ServerPlayer) {
        // Make the display invisible, just to be safe...
        val displayPacket = ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, null)
        player.connection.send(displayPacket)

        // Then remove the objective on the client's side.
        val scoreboardPacket = ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_REMOVE)
        player.connection.send(scoreboardPacket)
    }

    /**
     * Marks this sidebar as having been modified. The sidebar will then be updated in the next
     * tick.
     */
    fun markDirty() {
        this.isDirty = true
    }

    /**
     * Updates all lines for players.
     * Override [updateAll] if you want to do line updates.
     */
    fun doUpdateAll(forceAll: Boolean = false) {
        updateAll(forceAll)

        for (player in playerList) {
            this.doUpdatePlayer(player, forceAll)
        }

        this.lines.markAllUpdated()
    }

    /**
     * Called by [doUpdateAll] for running global line updates to players.
     * You may mutate [lines] for adding/modifying global lines.
     */
    @ApiStatus.OverrideOnly
    protected open fun updateAll(force: Boolean = false) {}

    /**
     * Runs and sends sidebar line updates to a player.
     * Override [updatePlayer] if you want to add your own lines.
     */
    fun doUpdatePlayer(player: ServerPlayer, force: Boolean = false) {
        if (!isDirty && !force)
            return

        val lines = this.additionalLines.computeIfAbsent(player.uuid) { SidebarLines(this) }

        updatePlayer(player, force, lines)

        this.lines.sendToClient(player, force)
        lines.sendToClient(player, force)
        lines.markAllUpdated()
    }

    /**
     * Called by [doUpdatePlayer] for running and sending line updates to a player.
     * You may mutate [lines] for adding/updating lines for specific players.
     */
    @ApiStatus.OverrideOnly
    protected open fun updatePlayer(player: ServerPlayer, force: Boolean = false, lines: SidebarLines) {}

    /**
     * Sends line updates to players, if the lines have been marked as dirty, or if [force] is marked as true.
     */
    protected fun SidebarLines.sendToClient(player: ServerPlayer, force: Boolean = false) {
        for (line in this) {
            if (!line.visible && (line.isDirty || force)) {
                player.connection.send(ClientboundResetScorePacket(line.name, objective.name))
            } else if (line.isDirty || force) {
                val score = ClientboundSetScorePacket(line.name, objective.name, line.priority, Optional.of(line.text), Optional.of(numberFormat))
                player.connection.send(score)
            }
        }
    }
}