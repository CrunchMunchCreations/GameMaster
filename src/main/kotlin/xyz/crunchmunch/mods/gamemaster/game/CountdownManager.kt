package xyz.crunchmunch.mods.gamemaster.game

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer

open class CountdownManager {
    init {
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            init(server, null)
        }
    }

    open fun init(server: MinecraftServer, game: CustomGame<*, *, *>? = null) {}
    open fun update(ticks: Int, game: CustomGame<*, *, *>? = null) {}
    open fun setVisibility(isVisible: Boolean, game: CustomGame<*, *, *>? = null) {}
}