package xyz.crunchmunch.mods.gamemaster.team

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.resources.Identifier

/**
 * Represents a property that is attached to teams. May be attached for individual games, or be global attachments.
 */
@JvmRecord
data class TeamPropertyType<T>(
    val id: Identifier,
    val gameId: Identifier? = null,
    val defaultGetter: (() -> T)? = null,
    val serializer: Codec<T>? = null,
) {
    init {
        if (registered.contains(this.id))
            throw IllegalArgumentException("Team property type already exists by ID $id!")

        registered[this.id] = this
    }

    companion object {
        val CODEC: Codec<TeamPropertyType<*>> = Identifier.CODEC.comapFlatMap({ findRegistered<Any?>(it) }, TeamPropertyType<*>::id)

        private val registered: MutableMap<Identifier, TeamPropertyType<*>> = mutableMapOf()

        @JvmStatic
        private fun <T> findRegistered(id: Identifier): DataResult<TeamPropertyType<T>> {
            if (!this.registered.contains(id))
                return DataResult.error { "Tried to find registered property $id but none was found!" }

            return DataResult.success(this.registered[id]!! as TeamPropertyType<T>)
        }
    }
}
