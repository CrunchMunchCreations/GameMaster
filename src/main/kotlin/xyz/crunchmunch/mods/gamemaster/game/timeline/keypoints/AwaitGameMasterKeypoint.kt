package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints

import com.mojang.serialization.MapCodec
import net.minecraft.server.MinecraftServer

object AwaitGameMasterKeypoint : TimelineKeypoint<AwaitGameMasterKeypoint>(KeypointManager.AWAIT_GAMEMASTER) {
    val CODEC: MapCodec<AwaitGameMasterKeypoint> = MapCodec.unit(AwaitGameMasterKeypoint)

    override suspend fun execute(server: MinecraftServer) {

    }
}