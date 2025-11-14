package xyz.crunchmunch.mods.gamemaster.animator.animation

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
import org.joml.Vector3f
import xyz.crunchmunch.mods.gamemaster.animator.util.PositionAndRotation
import xyz.crunchmunch.mods.gamemaster.utils.copy

// Bedrock's animation format, adapted for use within GameMaster.
data class BedrockMultiAnimationDefinition(
    override val animations: Map<String, Animation>,
    val formatVersion: String
) : MultiAnimationDefinition() {
    override val type: AnimationDefinitionType<*>
        get() = AnimationDefinitionTypes.BEDROCK

    companion object {
        private data class BoneAnimationMapping(val position: Map<Int, Vector3f>, val rotation: Map<Int, Vector3f>)
        private val POS_ROT_CODEC = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.withAlternative(
                    Codec.unboundedMap(
                        Codec.STRING.xmap({ (it.toFloat() * 20).toInt() }, { (it.toFloat() / 20f).toString() }),
                        ExtraCodecs.VECTOR3F.xmap({ it.copy().div(16f, 16f, 16f) }, { it.copy().mul(16f, 16f, 16f) })
                    ),
                    ExtraCodecs.VECTOR3F.xmap({ it.copy().div(16f, 16f, 16f) }, { it.copy().mul(16f, 16f, 16f) })
                        .flatComapMap({ mapOf(0 to it) }, { DataResult.success(it.values.firstOrNull() ?: return@flatComapMap DataResult.error { "Expected at least one value, but got empty!" }) })
                )
                    .optionalFieldOf("position", mapOf())
                    .forGetter(BoneAnimationMapping::position),

                Codec.withAlternative(
                    Codec.unboundedMap(
                        Codec.STRING.xmap({ (it.toFloat() * 20).toInt() }, { (it.toFloat() / 20f).toString() }),
                        ExtraCodecs.VECTOR3F
                    ),
                    ExtraCodecs.VECTOR3F
                        .flatComapMap({ mapOf(0 to it) }, { DataResult.success(it.values.firstOrNull() ?: return@flatComapMap DataResult.error { "Expected at least one value, but got empty!" }) })
                )
                    .optionalFieldOf("rotation", mapOf())
                    .forGetter(BoneAnimationMapping::rotation)
            )
                .apply(instance, ::BoneAnimationMapping)
        }

        val BEDROCK_ANIMATION_CODEC = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.unboundedMap(Codec.STRING, POS_ROT_CODEC)
                    .xmap({ mappings ->
                        val parts = mutableMapOf<String, Map<Int, PositionAndRotation>>()
                        for ((partName, mapping) in mappings) {
                            val positionsAndRotations = mutableMapOf<Int, PositionAndRotation>()

                            for ((tick, position) in mapping.position) {
                                positionsAndRotations.computeIfAbsent(tick) { PositionAndRotation() }
                                    .position.set(position)
                            }

                            for ((tick, rotation) in mapping.rotation) {
                                positionsAndRotations.computeIfAbsent(tick) { PositionAndRotation() }
                                    .rotation.set(rotation)
                            }

                            parts[partName] = positionsAndRotations
                        }

                        parts.toMap()
                    }, { parts ->
                        val mappings = mutableMapOf<String, BoneAnimationMapping>()
                        for ((partName, partInfo) in parts) {
                            mappings[partName] = BoneAnimationMapping(
                                partInfo.mapValues { it.value.position },
                                partInfo.mapValues { it.value.rotation }
                            )
                        }

                        mappings
                    })
                    .fieldOf("bones")
                    .forGetter(Animation::parts),

                Codec.withAlternative(
                    LoopType.CODEC,
                    Codec.BOOL.xmap({ if (it) LoopType.LOOP else LoopType.PLAY_ONCE }, { it == LoopType.LOOP })
                )
                    .optionalFieldOf("loop", LoopType.PLAY_ONCE)
                    .forGetter(Animation::loop),
                Codec.FLOAT
                    .xmap({ (it * 20).toInt() }, { (it.toFloat() / 20f) })
                    .fieldOf("animation_length")
                    .forGetter(Animation::maxLength),
                Codec.FLOAT
                    .comapFlatMap({
                        DataResult.success(((it * 20).toInt()))
                    }, { (it.toFloat() / 20f) })
                    .fieldOf("loop_delay")
                    .forGetter(Animation::loopDelay)
            )
                .apply(instance, ::Animation)
        }

        val CODEC = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.unboundedMap(
                    Codec.STRING,
                    BEDROCK_ANIMATION_CODEC
                )
                    .fieldOf("animations")
                    .forGetter(BedrockMultiAnimationDefinition::animations),
                Codec.STRING
                    .fieldOf("format_version")
                    .forGetter(BedrockMultiAnimationDefinition::formatVersion)
            )
                .apply(instance, ::BedrockMultiAnimationDefinition)
        }
    }
}
