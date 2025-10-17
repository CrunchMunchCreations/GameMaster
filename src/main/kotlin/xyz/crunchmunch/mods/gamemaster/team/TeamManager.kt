package xyz.crunchmunch.mods.gamemaster.team

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.events.TeamEvents
import xyz.crunchmunch.mods.gamemaster.game.CustomGame
import xyz.crunchmunch.mods.gamemaster.game.GameEvents
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.io.path.*

open class TeamManager(dataPath: Path) : Collection<Team> {
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
            server.scoreboard.addPlayerToTeam(listener.getPlayer().scoreboardName, playerTeam.mcTeam)
        }
    }

    fun addNewTeam(team: Team) {
        if (teams.any { it.id == team.id })
            throw IllegalArgumentException("A team already exists with that ID!")

        this.teams.add(team)
        TeamEvents.TEAM_ADDED.invoker().onTeamEvent(team)
        TeamEvents.TEAM_UPDATED.invoker().onTeamEvent(team)
        save()
    }

    fun removeTeam(team: Team) {
        this.teams.remove(team)
        TeamEvents.TEAM_REMOVED.invoker().onTeamEvent(team)
        TeamEvents.TEAM_UPDATED.invoker().onTeamEvent(team)
        save()
    }

    fun getTeam(id: String): Team? {
        return this.teams.firstOrNull { it.id == id }
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

    fun removePlayerFromTeam(player: TeamPlayer, team: Team) {
        if (team.players.removeIf { it.id == player.id }) {
            TeamEvents.TEAM_PLAYER_REMOVED.invoker().onTeamPlayerEvent(team, player)
            TeamEvents.TEAM_UPDATED.invoker().onTeamEvent(team)
            team.mcTeam.players.remove(player.username)
        }

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

    fun awardPoints(player: Player, points: Int, game: CustomGame<*, *, *>? = null) {
        val teamPlayer = this.getTeamPlayer(player) ?: return
        val team = this.getPlayerTeam(teamPlayer) ?: return
        teamPlayer.transientPoints += points
        TeamEvents.TEAM_PLAYER_POINTS_AWARDED.invoker().onTeamPointsEvent(game, team, teamPlayer, points)
        TeamEvents.TEAM_UPDATED.invoker().onTeamEvent(team)
    }

    fun commitPoints(player: Player, game: CustomGame<*, *, *>? = null) {
        val teamPlayer = this.getTeamPlayer(player) ?: return
        commitPoints(teamPlayer, game)
    }

    open fun commitPoints(teamPlayer: TeamPlayer, game: CustomGame<*, *, *>? = null) {
        val team = this.getPlayerTeam(teamPlayer) ?: return
        teamPlayer.points += teamPlayer.transientPoints
        teamPlayer.transientPoints = 0
        TeamEvents.TEAM_PLAYER_POINTS_COMMITTED.invoker().onTeamPointsEvent(game, team, teamPlayer, teamPlayer.transientPoints)
        TeamEvents.TEAM_UPDATED.invoker().onTeamEvent(team)
        save()
    }

    /**
     * If [transient] is true, the placement is based on the player's transient points, rather than
     * their total collected points.
     *
     * @return The current individual placement of the player, or -1 if the player is not a
     *         participant.
     */
    fun getIndividualPlacement(player: ServerPlayer, transient: Boolean = false): Int {
        val teamPlayer = this.getTeamPlayer(player) ?: return -1
        val index = this.getIndividualLeaderboard(transient)
            .indexOfFirst { it.first == teamPlayer }

        if (index == -1)
            return -1

        return index + 1
    }

    /**
     * If [transient] is true, the placement is based on the team's transient points, rather than
     * their total collected points.
     *
     * @return The current individual placement of the team, or -1 if the team is not participating.
     */
    fun getTeamPlacement(team: Team, transient: Boolean = false): Int {
        val index = this.getTeamLeaderboard(transient)
            .indexOfFirst { it.first == team }

        if (index == -1)
            return -1

        return index + 1
    }

    /**
     * Returns a sorted leaderboard of all the individual players, in descending order from most points to the least points.
     *
     * If [transient] is true, the points provided are all the currently transient points. Note that if it is false,
     * the transient points are NOT included in the points number.
     */
    fun getIndividualLeaderboard(transient: Boolean = false): List<Pair<TeamPlayer, Int>> {
        return this.teams
            .filter { it.type == Team.Type.PLAYER } // Only display the player teams
            .flatMap { it.players } // Flatten the teams into just their players
            .sortedByDescending { if (transient) it.transientPoints else it.points } // Sort by points in descending order
            .map { it to (if (transient) it.transientPoints else it.points) } // Then map it into a pair.
    }

    /**
     * Returns a sorted leaderboard of all the teams (that are players), in descending order from most points to the least points.
     *
     * If [transient] is true, the points provided are all the currently transient points. Note that if it is false,
     * the transient points are NOT included in the points number.
     */
    fun getTeamLeaderboard(transient: Boolean = false): List<Pair<Team, Int>> {
        return this.teams
            .filter { it.type == Team.Type.PLAYER } // Only display the player teams
            .sortedByDescending { if (transient) it.transientPoints else it.points } // Sort by points in descending order
            .map { it to (if (transient) it.transientPoints else it.points) } // Then map it into a pair.
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
            file.parent.createParentDirectories()

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

        val json = file.reader(Charsets.UTF_8, StandardOpenOption.READ).use { JsonParser.parseReader(it) }
        teams.addAll(CODEC.decode(JsonOps.INSTANCE, json).orThrow.first)
    }

    override val size: Int
        get() = teams.size

    override fun isEmpty(): Boolean = teams.isEmpty()
    override fun contains(element: Team): Boolean = teams.contains(element)
    override fun containsAll(elements: Collection<Team>): Boolean = teams.containsAll(elements)
    override fun iterator(): Iterator<Team> = teams.iterator()
    override fun spliterator(): Spliterator<Team> = teams.spliterator()

    companion object {
        private val GSON = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
        private val CODEC = Team.CODEC.listOf()
    }
}