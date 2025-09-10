package xyz.crunchmunch.mods.gamemaster.team

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.world.entity.player.Player
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.events.TeamEvents
import xyz.crunchmunch.mods.gamemaster.game.GameEvents
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.io.path.*

open class TeamManager(dataPath: Path) : Iterable<Team> {
    private val file = dataPath.resolve("teams.json")
    private val teams = Collections.synchronizedList(mutableListOf<Team>())

    private var isDirty = false

    init {
        ServerLifecycleEvents.SERVER_STOPPING.register {
            // Ensure that if the server is stopping while the data is dirty (usually crashing), data gets saved regardless.
            if (isDirty)
                forceSaveSynchronously()
        }

        GameEvents.STOP.register {
            this.save()
        }

        // Automatically adds the players to their designated teams
        ServerPlayConnectionEvents.JOIN.register { listener, sender, server ->
            val playerTeam = this.getPlayerTeam(listener.getPlayer()) ?: return@register
            listener.getPlayer().scoreboard.addPlayerToTeam(listener.getPlayer().scoreboardName, playerTeam.mcTeam)
        }
    }

    fun addNewTeam(team: Team) {
        if (teams.any { it.id == team.id })
            throw IllegalArgumentException("A team already exists with that ID!")

        this.teams.add(team)
        TeamEvents.TEAM_ADDED.invoker().onTeamEvent(team)
        save()
    }

    fun removeTeam(team: Team) {
        this.teams.remove(team)
        TeamEvents.TEAM_REMOVED.invoker().onTeamEvent(team)
        save()
    }

    fun getTeam(id: String): Team {
        return this.teams.first { it.id == id }
    }

    fun hasTeam(id: String): Boolean {
        return this.teams.any { it.id == id }
    }

    fun getPlayerTeam(player: Player): Team? {
        return getPlayerTeam(player.uuid)
    }

    fun getPlayerTeam(player: TeamPlayer): Team? {
        return getPlayerTeam(player.id)
    }

    fun getPlayerTeam(uuid: UUID): Team? {
        return this.teams.firstOrNull { it.players.any { p -> p.id == uuid } }
    }

    fun getTeamPlayer(player: Player): TeamPlayer? {
        val team = this.getPlayerTeam(player) ?: return null
        return team.players.firstOrNull { it.id == player.uuid }
    }

    fun addPlayerToTeam(player: TeamPlayer, team: Team) {
        removePlayerFromTeam(player) // remove from initial team
        team.players.add(player)
        GameMaster.server.scoreboard.addPlayerToTeam(player.username, team.mcTeam)

        TeamEvents.TEAM_PLAYER_ADDED.invoker().onTeamPlayerEvent(team, player)
        TeamEvents.TEAM_UPDATED.invoker().onTeamEvent(team)

        save()
    }

    fun removePlayerFromTeam(player: TeamPlayer) {
        for (team in teams) {
            if (team.players.removeIf { it.id == player.id }) {
                TeamEvents.TEAM_PLAYER_REMOVED.invoker().onTeamPlayerEvent(team, player)
                TeamEvents.TEAM_UPDATED.invoker().onTeamEvent(team)
                team.mcTeam.players.remove(player.username)
            }
        }

        save()
    }

    fun awardPoints(player: Player, points: Int) {
        val teamPlayer = this.getTeamPlayer(player) ?: return
        teamPlayer.transientPoints += points
    }

    fun commitPoints(player: Player) {
        val teamPlayer = this.getTeamPlayer(player) ?: return
        commitPoints(teamPlayer)
    }

    open fun commitPoints(teamPlayer: TeamPlayer) {
        teamPlayer.points += teamPlayer.transientPoints
        teamPlayer.transientPoints = 0
        save()
    }


    /**
     * Queues a data save to occur later. If multiple have been queued at the same time, only one will ever go through.
     */
    fun save() {
        isDirty = true

        // Submits the save to occur on the next server tick.
        GameMaster.server.submit {
            if (isDirty)
                forceSaveSynchronously()
        }
    }

    /**
     * This saves the data directly to the disk. Should avoid being called directly where possible.
     */
    fun forceSaveSynchronously() {
        if (!file.parent.exists())
            file.createParentDirectories()

        if (!file.exists())
            file.createFile()

        synchronized(teams) {
            val json = CODEC.encodeStart(JsonOps.INSTANCE, teams).orThrow
            file.writeText(GSON.toJson(json), Charsets.UTF_8, StandardOpenOption.WRITE)

            isDirty = false
        }
    }

    /**
     * Directly loads the team data from the disk. This immediately clears the old team data.
     */
    fun load() {
        teams.clear()

        if (!file.exists())
            return

        val json = file.reader(Charsets.UTF_8).use { JsonParser.parseReader(it) }
        teams.addAll(CODEC.decode(JsonOps.INSTANCE, json).orThrow.first)
    }

    override fun iterator(): Iterator<Team> {
        return this.teams.iterator()
    }

    override fun spliterator(): Spliterator<Team> {
        return this.teams.spliterator()
    }

    companion object {
        private val GSON = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
        private val CODEC = Team.CODEC.listOf()
    }
}