package xyz.crunchmunch.mods.gamemaster.game

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.level.GameType
import org.jetbrains.annotations.ApiStatus
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.game.metadata.CustomGameMetadata
import xyz.crunchmunch.mods.gamemaster.game.metadata.CustomGameProperties
import xyz.crunchmunch.mods.gamemaster.players.FreezeManager
import xyz.crunchmunch.mods.gamemaster.players.InventoryManager
import xyz.crunchmunch.mods.gamemaster.team.Team
import xyz.crunchmunch.mods.gamemaster.utils.sendTitle
import xyz.crunchmunch.mods.gamemaster.utils.setTitleAnimationTimes
import xyz.crunchmunch.mods.gamemaster.utils.teleportTo
import xyz.crunchmunch.mods.gamemaster.utils.ticks
import xyz.crunchmunch.spectatorapi.SpectatorAPI.Companion.disableCustomSpectator
import xyz.crunchmunch.spectatorapi.SpectatorAPI.Companion.enableCustomSpectator
import xyz.crunchmunch.spectatorapi.SpectatorAPI.Companion.isCustomSpectator
import java.util.function.BooleanSupplier
import kotlin.time.Duration.Companion.seconds

/**
 * Represents a custom game created under GameMaster.
 */
abstract class CustomGame(
    val gameManager: GameManager,
    val id: ResourceLocation,
    val settings: CustomGameMetadata,
    val properties: CustomGameProperties = CustomGameProperties()
) {
    val level: ServerLevel?
        get() {
            if (FabricLoader.getInstance().isDevelopmentEnvironment)
                return GameMaster.server.overworld()

            val key = GameMaster.server.levelKeys().firstOrNull { it.location() == this.settings.worldId } ?: return null
            return GameMaster.server.getLevel(key)
        }

    /**
     * The current state of the game.
     */
    var state = GameState.STOPPED

    /**
     * Is the game active?
     */
    var isActive = false

    private val queuedTicks = mutableListOf<BooleanSupplier>()

    /**
     * The current round of the game.
     */
    var currentRound = 1

    protected var hasPreReset = false

    /**
     * The remaining ticks in a round, or in a countdown in general.
     */
    var remainingTicks = 0

    /**
     * The remaining ticks before a pause occurred.
     */
    private var pauseRemainingTicks = 0

    /**
     * Represents the list of players who are in this game.
     */
    open val players: Collection<ServerPlayer>
        get() {
            return GameMaster.server.playerList.players
        }

    /**
     * Runs preparations for the game before the game begins. This includes teleporting players to
     * the designated level.
     */
    internal fun prepareStart() {
        if (level == null)
            throw IllegalStateException("The game level does not exist!")

        if (this.properties.doesPreReset && !this.hasPreReset)
            throw IllegalStateException("The game has not been reset yet!")

        GameEvents.BEGIN_PREPARE.invoker().onGameEvent(this)
        this.isActive = true

        val spawnSettings = this.settings.spawnSettings

        for (player in players) {
            // Don't affect anyone not playing/spectating.
            val team = this.gameManager.teamManager.getPlayerTeam(player) ?: continue

            if (team.type == Team.Type.NONE)
                continue

            // Only disable a player team's spectator mode.
            if (team.type == Team.Type.PLAYER)
                player.disableCustomSpectator()

            InventoryManager.saveAndClearPlayerInventory(player, "${this.id.namespace}/${this.id.path}")

            // Teleport players to their respective locations
            player.teleportTo(level!!, spawnSettings.pos.x, spawnSettings.pos.y, spawnSettings.pos.z, spawnSettings.yaw, 0f)
            // Set a default respawn position
            player.setRespawnPosition(ServerPlayer.RespawnConfig(level!!.dimension(), BlockPos.containing(spawnSettings.pos), spawnSettings.yaw, false), false)

            if (team.type == Team.Type.PLAYER) {
                // Players should be in adventure mode by default.
                player.setGameMode(GameType.ADVENTURE)

                // Players should not have any exp whatsoever.
                player.setExperienceLevels(0)
                player.setExperiencePoints(0)

                // Give the player a default kit.
                setPlayerKit(player)
            } else {
                // Set the player into spectator immediately.
                player.enableCustomSpectator(enableFlight = this.properties.canSpectatorFly)
            }
        }

        // Set the sidebar manager as having changed.
        this.gameManager.sidebarManager.markDirty()

        GameEvents.PREPARE.invoker().onGameEvent(this)

        // Reset the game after all that is set up.
        if (!this.properties.doesPreReset)
            this.reset()

        // Starts the countdown for the game.
        this.beginCountdown()

        // Run any custom preparations set by the game.
        this.prepareStartGame()
    }

    open fun beginCountdown() {
        this.state = GameState.STARTING
        remainingTicks = 30.seconds.ticks

        this.gameManager.countdownManager.setVisibility(this, true)
        this.gameManager.countdownManager.update(this, remainingTicks)
    }

    @ApiStatus.OverrideOnly
    protected open fun prepareStartGame() {}

    internal fun start() {
        this.remainingTicks = this.settings.maxSecondsPerRound.seconds.ticks

        GameEvents.START.invoker().onGameEvent(this)
        startGame()
    }

    @ApiStatus.OverrideOnly
    protected open fun startGame() {
    }

    fun softReset() {
        this.queuedTicks.clear()
        this.softResetGame()
    }

    /**
     * Runs basic reset tasks that should take virtually no time to reset.
     */
    @ApiStatus.OverrideOnly
    protected open fun softResetGame() {

    }

    fun reset() {
        resetGame()
        this.hasPreReset = true

        softReset()
    }

    @ApiStatus.OverrideOnly
    protected open fun resetGame() {

    }

    fun tick() {
        val toRemove = mutableListOf<BooleanSupplier>()

        // Run any ticks that have been queued, if the supplier returns true then
        // we can remove it from the queue.
        for (queued in queuedTicks) {
            if (queued.asBoolean) {
                // Add to a "remove queue", because if we remove at the same time
                // as iterating, we'll run into a CME.
                toRemove.add(queued)
            }
        }

        // Remove the queued ticks as required.
        if (toRemove.isNotEmpty())
            queuedTicks.removeAll(toRemove)

        // We shouldn't do anything past this point if the game is paused.
        if (state == GameState.PAUSED)
            return

        remainingTicks--

        // only update every second, otherwise if we update every tick
        // we would screw over people's bandwidths.
        if (remainingTicks % 20L == 0L) {
            this.gameManager.countdownManager.update(this, remainingTicks)

            if (state == GameState.STARTING || state == GameState.UNPAUSING) {
                val seconds = remainingTicks / 20
                val colors = listOf(ChatFormatting.RED, ChatFormatting.YELLOW, ChatFormatting.GREEN)

                when (seconds) {
                    5, 4 -> {
                        for (player in players) {
                            player.setTitleAnimationTimes(0, 20, 5)
                            player.sendTitle(Component.literal("$seconds").withStyle(ChatFormatting.GREEN))
                        }
                    }

                    3, 2, 1 -> {
                        for (player in players) {
                            player.setTitleAnimationTimes(0, 20, 5)
                            player.sendTitle(Component.literal("$seconds").withStyle(colors[seconds - 1]))
                            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.VOICE, 0.6f, 0.6f)
                        }
                    }

                    0 -> {
                        for (player in players) {
                            player.setTitleAnimationTimes(0, 20, 5)
                            player.sendTitle(Component.literal("GO!").withStyle(ChatFormatting.GREEN))
                            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.VOICE, 0.6f, 1.3f)
                        }
                    }
                }
            } else if (state == GameState.PAUSING) {
                val seconds = remainingTicks / 20

                when (seconds) {
                    3, 2, 1 -> {
                        for (player in players) {
                            player.setTitleAnimationTimes(0, 20, 5)
                            player.sendTitle(Component.literal("Pausing in $seconds seconds...").withStyle(ChatFormatting.RED))
                            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.VOICE, 0.6f, 1.35f)
                        }
                    }

                    0 -> {
                        for (player in players) {
                            player.setTitleAnimationTimes(0, 20, 5)
                            player.sendTitle(Component.literal("Game paused!").withStyle(ChatFormatting.RED))
                            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.VOICE, 0.6f, 0.6f)
                        }
                    }
                }
            }
        }

        if (remainingTicks <= 0) {
            when (state) {
                GameState.STARTING -> this.start()
                GameState.STARTED -> this.stop()
                GameState.STOPPING -> this.tryFullStop()
                GameState.PAUSING -> this.actuallyPause()
                GameState.UNPAUSING -> this.actuallyUnpause()
                else -> {}
            }

            return
        }

        if (state == GameState.STARTED) {
            this.checkShouldFinishGame()
        }

        tickGame()
    }

    @ApiStatus.OverrideOnly
    protected open fun tickGame() {

    }

    fun pause() {
        this.state = GameState.PAUSING
        this.pauseRemainingTicks = this.remainingTicks

        // 3 second warning for pauses for the player to get ready, otherwise the player may get stuck in a
        // predicament that makes them unable to continue playing.
        this.remainingTicks = 3.seconds.ticks
    }

    protected fun actuallyPause() {
        GameEvents.PAUSE.invoker().onGameEvent(this)

        for (player in players) {
            FreezeManager.freezePlayer(player)
        }

        pauseGame()
    }

    @ApiStatus.OverrideOnly
    protected open fun pauseGame() {
    }

    fun unpause() {
        GameEvents.UNPAUSE.invoker().onGameEvent(this)
        this.state = GameState.UNPAUSING

        // Give a 3 second buffer before unpausing so the player can get ready to unpause.
        this.remainingTicks = 3.seconds.ticks
    }

    protected fun actuallyUnpause() {
        GameEvents.UNPAUSE.invoker().onGameEvent(this)

        for (player in players) {
            FreezeManager.unfreezePlayer(player)
        }

        // Set the remaining ticks back to what they're supposed to be.
        this.remainingTicks = this.pauseRemainingTicks
        this.state = GameState.STARTED

        unpauseGame()
    }

    @ApiStatus.OverrideOnly
    protected open fun unpauseGame() {
    }

    fun stop() {
        this.state = GameState.STOPPING
        this.remainingTicks = 15.seconds.ticks

        for (player in players) {
            // Modify player points, typically applies modifiers onto the points or even resets it to zero.
            this.modifyPlayerPoints(player)

            player.setTitleAnimationTimes(0, 20, 5)
            player.sendTitle(Component.literal("GAME OVER!").withStyle(ChatFormatting.RED))

            // Throw the players straight into spectator, but also no compass.
            player.enableCustomSpectator(enableFlight = this.properties.canSpectatorFly, giveCompass = false)

            // Clear the player state
            player.inventory.clearContent()
            player.setExperienceLevels(0)
            player.setExperienceLevels(0)
        }

        this.queuedTicks.clear()

        stopGame()
    }

    @ApiStatus.OverrideOnly
    protected open fun modifyPlayerPoints(player: ServerPlayer) {
    }

    @ApiStatus.OverrideOnly
    protected open fun stopGame() {
    }

    // Determines whether the game should fully stop after the 15 second buffer has passed,
    // or if it should automatically progress into a new round.
    open fun tryFullStop() {
        if (this.currentRound >= this.settings.rounds) {
            // Fully stop the game if we're past the rounds we have.
            this.fullStop()
        } else {
            // If the rounds automatically progress, let's handle that.
            if (this.properties.autoProgressRounds) {
                this.currentRound++
                this.prepareStart()

                for (player in players) {
                    val team = this.gameManager.teamManager.getPlayerTeam(player) ?: continue

                    // If the player was turned into a spectator, turn them back.
                    if (team.type == Team.Type.PLAYER)
                        player.disableCustomSpectator()

                    player.setTitleAnimationTimes(0, 20, 5)
                    player.sendTitle(Component.literal("Round $currentRound").withStyle(ChatFormatting.GREEN))
                }
            } else {
                // If we end up in this position, just full-stop the entire thing I guess.
                this.fullStop()
            }
        }
    }

    /**
     * Runs every "started" tick to check if the game should be marked as finished.
     */
    open fun checkShouldFinishGame() {
        if (!this.properties.autoStopWhenAllPlayersSpectator)
            return

        // If all players are spectators, automatically stop the game.
        if (players.all { player ->
            player.isCustomSpectator() || player.isSpectator || this.gameManager.teamManager.getPlayerTeam(player)?.type != Team.Type.PLAYER
        }) {
            this.stop()
        }
    }

    fun fullStop() {
        this.state = GameState.STOPPED
        this.gameManager.countdownManager.setVisibility(this, false)
        this.isActive = false
        this.currentRound = 1

        val hubWorld = GameMaster.server.overworld()
        val spawnPos = hubWorld.sharedSpawnPos
        val spawnYaw = hubWorld.sharedSpawnAngle

        GameEvents.STOP.invoker().onGameEvent(this)

        for (player in players) {
            // Commit the points directly to the player.
            this.commitPoints(player)

            // Revert the player state back to how they were previously.
            player.disableCustomSpectator()
            InventoryManager.loadPreviousPlayerInventory(player, "${this.id.namespace}/${this.id.path}")

            player.teleportTo(hubWorld, spawnPos.x + 0.5, spawnPos.y.toDouble(), spawnPos.z + 0.5, spawnYaw, 0f)
        }

        // Mark the sidebar as changed.
        this.gameManager.sidebarManager.markDirty()

        fullStopGame()
    }

    open fun commitPoints(player: ServerPlayer) {
        this.gameManager.teamManager.commitPoints(player)
    }

    /**
     * Runs when the game has fully stopped and no new rounds are to be played.
     * This is typically used to clean up the state and teleport the players back to
     * the hub.
     */
    protected open fun fullStopGame() {}

    open fun respawnPlayer(player: ServerPlayer) {
    }

    open fun setPlayerKit(player: ServerPlayer) {
    }

    open fun shouldCancelDamage(player: ServerPlayer, source: DamageSource, amount: Float): Boolean {
        return false
    }
}