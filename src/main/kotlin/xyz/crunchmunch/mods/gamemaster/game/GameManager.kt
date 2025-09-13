package xyz.crunchmunch.mods.gamemaster.game

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.core.HolderLookup
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.scoreboard.SidebarManager
import xyz.crunchmunch.mods.gamemaster.team.TeamManager
import java.util.*

/**
 * The central game manager of custom games. Games may be created via [createGame], as long as
 * the metadata has been provided in the respective data locations.
 */
open class GameManager<S : SidebarManager, T : TeamManager, C : CountdownManager>(
    open val sidebarManager: S,
    open val teamManager: T,
    open val countdownManager: C
) {
    private val gamesInternal = mutableSetOf<CustomGame<*, *, *>>()

    val games: Collection<CustomGame<*, *, *>> = Collections.unmodifiableSet(gamesInternal)
    val activeGames: Collection<CustomGame<*, *, *>>
        get() {
            return this.games.filter { it.isActive }
        }

    init {
        ServerTickEvents.END_WORLD_TICK.register { level ->
            for (game in this.activeGames) {
                if (game.settings.worldId == level.dimension().location() || game.level == level) {
                    game.tick()
                }
            }
        }
    }

    /**
     * Gets a game by its respective game ID.
     */
    fun <T : CustomGame<*, *, *>> getGameById(id: ResourceLocation): T? {
        return this.games.firstOrNull { it.id == id } as? T?
    }

    @Deprecated("This method is not recommended for public usage.")
    fun <T : CustomGame<*, *, *>> getOrCreateGameById(id: ResourceLocation): T {
        return this.getGameById(id) ?: this.createGame(GameMaster.server.registryAccess(), id)
    }

    /**
     * Creates a game by a game ID, assuming a metadata entry exists for it.
     */
    fun <T : CustomGame<*, *, *>> createGame(registry: HolderLookup.Provider, id: ResourceLocation): T {
        if (this.getGameById<CustomGame<*, *, *>>(id) != null) {
            throw IllegalArgumentException("A game by the ID $id already exists!")
        }

        val initializer = CustomGameManager.GAME_REGISTRY.getValue(id) as CustomGameManager.CustomGameInitializer
        val metadata = registry.lookupOrThrow(CustomGameManager.GAME_METADATA_REGISTRY_KEY)
            .getOrThrow(ResourceKey.create(CustomGameManager.GAME_METADATA_REGISTRY_KEY, id))

        return initializer.create(this, id, metadata.value()).apply {
            gamesInternal.add(this)
        } as T
    }

    companion object {
        private val managersInternal = mutableListOf<GameManager<*, *, *>>()

        /**
         * Displays all game managers that have been created, for commands to access.
         */
        val managers: List<GameManager<*, *, *>> = Collections.unmodifiableList(managersInternal)
    }
}