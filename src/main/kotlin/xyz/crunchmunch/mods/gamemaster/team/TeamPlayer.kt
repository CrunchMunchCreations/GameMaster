package xyz.crunchmunch.mods.gamemaster.team

import com.mojang.authlib.GameProfile
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerPlayer
import xyz.crunchmunch.mods.gamemaster.GameMaster
import java.util.*

data class TeamPlayer(
    /**
     * The player's UUID.
     */
    val id: UUID,
    /**
     * The last known username of the player.
     */
    var username: String,

    /**
     * The total points collected by the player. This does not include the points
     * collected in the currently active game, you may want `transientPoints` instead.
     */
    var points: Int = 0
) {
    /**
     * The points collected by the player in the current game.
     */
    var transientPoints: Int = 0

    /**
     * The ServerPlayer in the server. May be null, as the player not be online.
     */
    val mcPlayer: ServerPlayer?
        get() {
            return GameMaster.server.playerList.getPlayer(this.id)
        }

    /**
     * The player's game profile. Note that this does NOT store the player's skin!
     */
    val gameProfile = GameProfile(id, username)

    companion object {
        val CODEC = RecordCodecBuilder.create { instance ->
            instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("id")
                    .forGetter(TeamPlayer::id),
                Codec.STRING.fieldOf("username")
                    .forGetter(TeamPlayer::username),
                Codec.INT.fieldOf("points")
                    .forGetter(TeamPlayer::points)
            )
                .apply(instance, ::TeamPlayer)
        }

        fun createFromUsername(username: String): TeamPlayer {
            val profileOptional = GameMaster.server.services().profileResolver.fetchByName(username) ?: throw IllegalStateException("Failed to get profile cache from server!")
            val profile = profileOptional.orElseThrow()

            return createFromProfile(profile)
        }

        fun createFromId(uuid: UUID): TeamPlayer {
            val result = GameMaster.server.services().sessionService().fetchProfile(uuid, false)
            return createFromProfile(result?.profile ?: throw IllegalStateException("Could not find profile for $uuid!"))
        }

        fun createFromProfile(profile: GameProfile): TeamPlayer {
            return TeamPlayer(profile.id, profile.name)
        }
    }
}
