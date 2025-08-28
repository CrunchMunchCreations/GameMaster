package xyz.crunchmunch.mods.gamemaster.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import xyz.crunchmunch.mods.gamemaster.team.Team
import xyz.crunchmunch.mods.gamemaster.team.TeamPlayer

object TeamEvents {
    private fun genericTeamEvent(): Event<GenericTeamCallback> {
        return EventFactory.createArrayBacked(GenericTeamCallback::class.java) { callbacks ->
            GenericTeamCallback { team ->
                for (callback in callbacks) {
                    callback.onTeamEvent(team)
                }
            }
        }
    }

    private fun genericTeamPlayerEvent(): Event<GenericTeamPlayerCallback> {
        return EventFactory.createArrayBacked(GenericTeamPlayerCallback::class.java) { callbacks ->
            GenericTeamPlayerCallback { team, player ->
                for (callback in callbacks) {
                    callback.onTeamPlayerEvent(team, player)
                }
            }
        }
    }

    private fun genericTeamPointsEvent(): Event<GenericTeamPointsCallback> {
        return EventFactory.createArrayBacked(GenericTeamPointsCallback::class.java) { callbacks ->
            GenericTeamPointsCallback { team, player, points ->
                for (callback in callbacks) {
                    callback.onTeamPointsEvent(team, player, points)
                }
            }
        }
    }

    /**
     * Called when a team is added to the team manager.
     */
    @JvmField val TEAM_ADDED = genericTeamEvent()

    /**
     * Called every time a team is updated, including its players.
     */
    @JvmField val TEAM_UPDATED = genericTeamEvent()

    /**
     * Called when a team is removed from the team manager.
     */
    @JvmField val TEAM_REMOVED = genericTeamEvent()

    /**
     * Called when a player is added to a team.
     */
    @JvmField val TEAM_PLAYER_ADDED = genericTeamPlayerEvent()

    /**
     * Called when a player is removed from a team.
     */
    @JvmField val TEAM_PLAYER_REMOVED = genericTeamPlayerEvent()

    /**
     * Called when points are awarded to a player. These points are stored transiently,
     * and must be committed before they are saved.
     */
    @JvmField val TEAM_PLAYER_POINTS_AWARDED = genericTeamPointsEvent()

    /**
     * Called when a player's points are now stored onto the player themselves.
     */
    @JvmField val TEAM_PLAYER_POINTS_COMMITTED = genericTeamPointsEvent()


    fun interface GenericTeamCallback {
        fun onTeamEvent(team: Team)
    }

    fun interface GenericTeamPlayerCallback {
        fun onTeamPlayerEvent(team: Team, player: TeamPlayer)
    }

    fun interface GenericTeamPointsCallback {
        fun onTeamPointsEvent(team: Team, player: TeamPlayer, points: Int)
    }
}