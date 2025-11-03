package xyz.crunchmunch.mods.gamemaster.server

import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.notifications.EmptyNotificationService
import xyz.crunchmunch.mods.gamemaster.events.PlayerEvents

class GameMasterNotificationService : EmptyNotificationService() {
    override fun playerJoined(player: ServerPlayer) {
        PlayerEvents.JOIN.invoker().onGenericEvent(player)
    }

    override fun playerLeft(player: ServerPlayer) {
        PlayerEvents.LEAVE.invoker().onGenericEvent(player)
    }
}