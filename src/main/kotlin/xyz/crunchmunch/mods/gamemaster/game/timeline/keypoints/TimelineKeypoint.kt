package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.server.MinecraftServer

abstract class TimelineKeypoint<T : TimelineKeypoint<T>>(val type: Type<T>) {
    class Type<T : TimelineKeypoint<T>>(val codec: MapCodec<T>)

    abstract suspend fun execute(server: MinecraftServer)

    companion object {
        val CODEC: Codec<TimelineKeypoint<*>> = KeypointManager.REGISTRY.byNameCodec()
            .dispatch("type", TimelineKeypoint<*>::type, Type<*>::codec)
    }
}