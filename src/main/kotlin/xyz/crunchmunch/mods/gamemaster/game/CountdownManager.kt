package xyz.crunchmunch.mods.gamemaster.game

import net.minecraft.server.MinecraftServer

open class CountdownManager {
    open fun init(server: MinecraftServer, game: CustomGame) {}
    open fun update(game: CustomGame, ticks: Int) {}
    open fun setVisibility(game: CustomGame, isVisible: Boolean) {}
}