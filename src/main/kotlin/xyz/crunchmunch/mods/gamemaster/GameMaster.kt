package xyz.crunchmunch.mods.gamemaster

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import java.nio.file.Path

// A more common variant of the minigame system used in Chrunchy Christmas for being used
// with other projects, without forcing a "dependency" on Chrunchy Christmas' central management
// system.
class GameMaster : ModInitializer {
    override fun onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            GameMaster.server = server
        }
    }

    companion object {
        const val MOD_ID = "gamemaster"

        lateinit var server: MinecraftServer
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