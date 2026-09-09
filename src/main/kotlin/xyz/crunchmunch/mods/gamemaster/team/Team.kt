package xyz.crunchmunch.mods.gamemaster.team

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.resources.Identifier
import net.minecraft.util.StringRepresentable
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.TeamColor
import xyz.crunchmunch.mods.gamemaster.GameMaster
import net.minecraft.world.scores.Team as VanillaTeam

/**
 * The team that is participating in the event.
 */
data class Team(
    val id: String,
    var name: Component,
    /**
     * The prefix that will be appended to players' display names in the team.
     */
    var prefix: Component,
    var teamColor: TeamColor,
    var type: Type,

    val players: MutableList<TeamPlayer>,
) {
    // Similar to data attachments, per-team.
    private val properties: MutableMap<TeamPropertyType<*>, Any?> = mutableMapOf()

    /**
     * Represents the total amount of points that the entire team has.
     * This does not include the points collected in the currently active game,
     * those can be tallied manually by combining with the transient points.
     */
    val points: Int
        get() {
            return this.players.sumOf { it.points }
        }

    /**
     * Represents the points collected in the currently active game.
     * It's possible for these points to be lost when the game ends.
     */
    val transientPoints: Int
        get() {
            return this.players.sumOf { it.transientPoints }
        }

    /**
     * Represents the display name (prefix + name) of the team
     */
    val displayName: Component
        get() = Component.empty()
            .append(this.prefix)
            .append(" ")
            .append(this.name)

    // An internal "cache" for the player team.
    private lateinit var mcTeamInternal: PlayerTeam

    val mcTeam: PlayerTeam
        get() {
            if (!this::mcTeamInternal.isInitialized) {
                val scoreboard = GameMaster.server.scoreboard
                val team = scoreboard.getPlayerTeam("gamemaster_$id") ?: scoreboard.addPlayerTeam("gamemaster_$id")

                team.displayName = this.displayName
                team.setPlayerPrefix(Component.empty().append(this.prefix).append(" "))
                team.collisionRule = VanillaTeam.CollisionRule.NEVER // Prevents teammates from causing collision with one another
                team.setSeeFriendlyInvisibles(false) // Prevents teammates from being visible when in the custom spectator mode
                team.isAllowFriendlyFire = false // Disables team friendly fire

                this.mcTeamInternal = team
            }

            return this.mcTeamInternal
        }

    fun <T> hasProperty(property: TeamPropertyType<T>): Boolean {
        return this.properties[property] != null
    }

    operator fun <T> get(property: TeamPropertyType<T>): T? {
        return this.getProperty(property)
    }

    operator fun <T> set(property: TeamPropertyType<T>, value: T) {
        this.setProperty(property, value)
    }

    @JvmOverloads
    fun <T> getProperty(property: TeamPropertyType<T>, orElse: T? = property.defaultGetter?.invoke()): T? {
        return this.properties[property] as? T ?: orElse
    }

    fun <T> getPropertyOrThrow(property: TeamPropertyType<T>): T {
        return this.getProperty(property)
            ?: throw IllegalStateException("Could not get property by type ${property.id}!")
    }

    fun <T> setProperty(property: TeamPropertyType<T>, value: T): T? {
        val oldValue = this.getProperty(property)
        this.properties[property] = value
        return oldValue
    }

    fun setProperty(property: TeamPropertyType<Unit>) {
        this.setProperty(property, Unit)
    }

    fun removeProperty(property: TeamPropertyType<*>) {
        this.properties.remove(property)
    }

    fun resetAllProperties() {
        this.properties.clear()
    }

    fun resetPropertiesForGame(gameId: Identifier) {
        val keys = this.properties.keys.filter { it.gameId != null && it.gameId == gameId }
        for (key in keys) {
            this.properties.remove(key)
        }
    }

    private fun getSerializableProperties(): Map<TeamPropertyType<*>, Any?> {
        return this.properties.filterKeys { it.serializer != null }
    }

    companion object {
        val CODEC: Codec<Team> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id")
                    .forGetter(Team::id),
                ComponentSerialization.CODEC.fieldOf("name")
                    .forGetter(Team::name),
                ComponentSerialization.CODEC.fieldOf("prefix")
                    .forGetter(Team::prefix),
                TeamColor.CODEC.fieldOf("team_color")
                    .forGetter(Team::teamColor),
                Type.CODEC.fieldOf("type")
                    .forGetter(Team::type),
                TeamPlayer.CODEC.listOf().fieldOf("players")
                    .forGetter(Team::players),
                Codec.dispatchedMap(TeamPropertyType.CODEC) { propertyType -> propertyType.serializer!! }
                    .lenientOptionalFieldOf("properties", mapOf())
                    .forGetter(Team::getSerializableProperties),
            )
                .apply(instance) { id, name, prefix, color, type, players, properties ->
                    val team = Team(id, name, prefix, color, type, players.toMutableList())
                    team.properties.putAll(properties)

                    team
                }
        }
    }

    enum class Type(private val serialized: String, val shouldBeTeleported: Boolean = true) : StringRepresentable {
        /**
         * Represents a team that will be playing in the games.
         */
        PLAYER("player"),

        /**
         * Represents a team that will only be spectating the games.
         */
        SPECTATOR("spectator"),

        /**
         * Represents a team that should not be teleported to the games at all.
         */
        NONE("none", false);

        override fun getSerializedName(): String {
            return serialized
        }

        companion object {
            val CODEC: Codec<Type> = StringRepresentable.fromEnum(Type::values)
        }
    }
}
