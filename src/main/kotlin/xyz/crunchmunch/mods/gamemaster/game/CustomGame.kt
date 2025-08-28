package xyz.crunchmunch.mods.gamemaster.game

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import org.jetbrains.annotations.ApiStatus
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.game.metadata.CustomGameMetadata
import xyz.crunchmunch.mods.gamemaster.game.metadata.CustomGameProperties
import java.util.function.BooleanSupplier

/**
 * Represents a custom game created under GameMaster.
 */
abstract class CustomGame(
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

    var hasPreReset = false

    /**
     * Runs preparations for the game before the game begins. This includes teleporting players to
     * the designated level.
     */
    internal fun doPrepareStart() {
        if (level == null)
            throw IllegalStateException("The game level does not exist!")

        if (this.properties.doesPreReset && !this.hasPreReset)
            throw IllegalStateException("The game has not been reset yet!")

        GameEvents.BEGIN_PREPARE.invoker().onGameEvent(this)
        this.isActive = true


    }

    @ApiStatus.OverrideOnly
    protected open fun prepareStart() {}


}