package xyz.crunchmunch.mods.gamemaster.game

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

object GameEvents {
    private fun genericGameEvent(): Event<GenericGameCallback> {
        return EventFactory.createArrayBacked(GenericGameCallback::class.java) {
            GenericGameCallback { game ->
                for (event in it) {
                    event.onGameEvent(game)
                }
            }
        }
    }

    @JvmField val BEGIN_PREPARE = genericGameEvent()
    @JvmField val PREPARE = genericGameEvent()
    @JvmField val START = genericGameEvent()
    @JvmField val TICK = genericGameEvent()
    @JvmField val STOP = genericGameEvent()
    @JvmField val SOFT_RESET = genericGameEvent()
    @JvmField val RESET = genericGameEvent()
    @JvmField val PAUSE = genericGameEvent()
    @JvmField val UNPAUSE = genericGameEvent()

    fun interface GenericGameCallback {
        fun onGameEvent(game: CustomGame<*, *, *>)
    }
}