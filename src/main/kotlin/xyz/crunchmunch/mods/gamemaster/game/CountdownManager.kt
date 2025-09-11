package xyz.crunchmunch.mods.gamemaster.game

import net.minecraft.server.MinecraftServer

open class CountdownManager {
    open fun init(server: MinecraftServer, game: CustomGame? = null) {}
    open fun update(ticks: Int, game: CustomGame? = null) {}
    open fun setVisibility(isVisible: Boolean, game: CustomGame? = null) {}
}