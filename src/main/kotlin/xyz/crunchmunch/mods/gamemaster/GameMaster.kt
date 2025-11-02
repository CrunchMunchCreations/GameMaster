package xyz.crunchmunch.mods.gamemaster

import de.phyrone.brig.wrapper.literal
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences
import net.minecraft.commands.Commands
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.crunchmunch.mods.gamemaster.commands.gameMarkerCommands
import xyz.crunchmunch.mods.gamemaster.game.CustomGameManager
import xyz.crunchmunch.mods.gamemaster.game.marker.GameMarkerManager
import java.nio.file.Path

// A more common variant of the minigame system used in Chrunchy Christmas for being used
// with other projects, without forcing a "dependency" on Chrunchy Christmas' central management
// system.
class GameMaster : ModInitializer {
    override fun onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            GameMaster.server = server
            adventure = MinecraftServerAudiences.of(server)
        }

        CustomGameManager.init()
        GameMarkerManager.init()

        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            dispatcher.literal("gamemaster") {
                require { hasPermission(Commands.LEVEL_GAMEMASTERS) }

                gameMarkerCommands()
            }
        }
    }

    companion object {
        const val MOD_ID = "gamemaster"

        internal val logger: Logger = LoggerFactory.getLogger("GameMaster")
        lateinit var server: MinecraftServer
        lateinit var adventure: MinecraftServerAudiences

        private val rootPath = FabricLoader.getInstance().gameDir.resolve("crunchmunch/gamemaster")

        @JvmStatic
        fun resolvePath(name: String): Path {
            return rootPath.resolve(name)
        }

        @JvmStatic
        fun id(path: String): ResourceLocation {
            return ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
        }
    }
}