package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.game

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import xyz.crunchmunch.mods.gamemaster.GameMasterRegistryKeys
import xyz.crunchmunch.mods.gamemaster.game.CustomGameManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.KeypointManager
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.TimelineKeypoint

class StartGameKeypoint(
    val game: ResourceKey<CustomGameManager.CustomGameInitializer>
) : TimelineKeypoint<StartGameKeypoint>(KeypointManager.START_GAME) {
    override suspend fun execute(server: MinecraftServer) {

    }

    companion object {
        val CODEC: MapCodec<StartGameKeypoint> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                ResourceKey.codec(GameMasterRegistryKeys.CUSTOM_GAME)
                    .fieldOf("game")
                    .forGetter(StartGameKeypoint::game)
            )
                .apply(instance, ::StartGameKeypoint)
        }
    }
}