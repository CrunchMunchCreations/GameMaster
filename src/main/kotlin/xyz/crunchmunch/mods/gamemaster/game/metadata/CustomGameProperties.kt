package xyz.crunchmunch.mods.gamemaster.game.metadata

/**
 * Represents internal game properties used in checking how certain games would operate.
 */
data class CustomGameProperties(
    /**
     * Should the game automatically end if all players have entered a spectator mode?
     */
    val autoStopWhenAllPlayersSpectator: Boolean = true,

    /**
     * Should the game allow players to drop items?
     * Note that item drops are always disabled when the game is starting, paused and stopped.
     */
    val shouldAllowItemDrop: Boolean = false,

    /**
     * Should the game automatically progress to the next round?
     */
    val autoProgressRounds: Boolean = true,

    /**
     * Are spectators allowed to fly in this game?
     */
    val canSpectatorFly: Boolean = false,

    /**
     * Does the game consist of pre-resetting?
     * Pre-resetting is where the game is reset before the event even begins.
     */
    val doesPreReset: Boolean = false,
)