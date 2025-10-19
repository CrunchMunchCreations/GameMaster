package xyz.crunchmunch.mods.gamemaster.game.marker

import com.mojang.serialization.MapCodec
import net.minecraft.world.entity.Marker

data class GameMarkerType<MARKER : GameMarker<DATA>, DATA : Any>(
    /**
     * Codec for any data stored within the marker to be read.
     */
    val dataCodec: MapCodec<DATA>,

    /**
     * Initializer for creating the game marker. Only initializes based on [loadType].
     */
    val initializer: (Marker, DATA) -> MARKER
)
