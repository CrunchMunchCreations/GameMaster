package xyz.crunchmunch.mods.gamemaster.game.timeline.prerequisite

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import xyz.crunchmunch.mods.gamemaster.GameMasterRegistries

abstract class TimelinePrerequisite(val type: Type) {
    abstract fun meetsPrerequisite(server: MinecraftServer): Boolean
    abstract val prerequisiteFailedMessage: Component

    class Type(val codec: MapCodec<out TimelinePrerequisite>)

    companion object {
        val CODEC: Codec<TimelinePrerequisite> = GameMasterRegistries.PREREQUISITE.byNameCodec()
            .dispatch("type", TimelinePrerequisite::type, Type::codec)
    }
}
