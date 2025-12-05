package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints

import com.mojang.serialization.MapCodec
import net.minecraft.server.MinecraftServer

class AwaitGameMasterKeypoint : TimelineKeypoint<AwaitGameMasterKeypoint>(KeypointManager.AWAIT_GAMEMASTER) {
    override suspend fun execute(server: MinecraftServer) {

    }

    companion object {
        private val instance = lazy { AwaitGameMasterKeypoint() }
        val INSTANCE: AwaitGameMasterKeypoint
            get() {
                return instance.value
            }

        val CODEC: MapCodec<AwaitGameMasterKeypoint> = MapCodec.unit { instance.value }
    }
}