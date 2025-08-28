package xyz.crunchmunch.mods.gamemaster.game.metadata

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Transient
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.phys.Vec3

/**
 * Represents the metadata of the created game. If no games have been registered using the ID
 * the metadata is stored under, the metadata isn't used.
 *
 * The file location is in a datapack, under "data/chrunchy_christmas/gamemaster/custom_games/parkour.json" for example.
 */
data class CustomGameMetadata(
    val name: String,
    val rounds: Int,

    val maxSecondsPerRound: Long,

    val worldId: ResourceLocation,

    val spawnSettings: SpawnSettings
) {
    data class SpawnSettings(
        val pos: Vec3,
        val yaw: Float
    ) {
        companion object {
            val CODEC = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    Vec3.CODEC.fieldOf("pos")
                        .forGetter(SpawnSettings::pos),
                    Codec.FLOAT.fieldOf("yaw")
                        .forGetter(SpawnSettings::yaw)
                )
                    .apply(instance, ::SpawnSettings)
            }
        }
    }

    @Transient
    val displayName = MiniMessage.miniMessage().deserialize(name)

    companion object {
        val CODEC: Codec<CustomGameMetadata> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("name")
                    .forGetter(CustomGameMetadata::name),
                ExtraCodecs.POSITIVE_INT.fieldOf("rounds")
                    .forGetter(CustomGameMetadata::rounds),
                Codec.LONG.fieldOf("max_seconds_per_round")
                    .forGetter(CustomGameMetadata::maxSecondsPerRound),
                ResourceLocation.CODEC.fieldOf("world_id")
                    .forGetter(CustomGameMetadata::worldId),
                SpawnSettings.CODEC.fieldOf("spawn")
                    .forGetter(CustomGameMetadata::spawnSettings)
            )
                .apply(instance, ::CustomGameMetadata)
        }
    }
}