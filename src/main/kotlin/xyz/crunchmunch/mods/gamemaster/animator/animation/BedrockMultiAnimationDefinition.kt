package xyz.crunchmunch.mods.gamemaster.animator.animation

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.Mth
import org.joml.Matrix4f
import org.joml.Vector3f
import xyz.crunchmunch.mods.gamemaster.utils.copy

// Bedrock's animation format, adapted for use within GameMaster.
data class BedrockMultiAnimationDefinition(
    override val animations: Map<String, Animation>,
    val formatVersion: String
) : MultiAnimationDefinition() {
    override val type: AnimationDefinitionType<*>
        get() = AnimationDefinitionTypes.BEDROCK

    companion object {
        private data class BoneAnimationMapping(val position: Map<Int, Vector3f>, val rotation: Map<Int, Vector3f>, val scale: Map<Int, Vector3f>)
        private val TRANSFORMS_CODEC = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.withAlternative(
                    Codec.unboundedMap(
                        TICKS_AS_SECONDS_STRING_CODEC,
                        ExtraCodecs.VECTOR3F.xmap({ it.copy().div(16f, 16f, 16f) }, { it.copy().mul(16f, 16f, 16f) })
                    ),
                    ExtraCodecs.VECTOR3F.xmap({ it.copy().div(16f, 16f, 16f) }, { it.copy().mul(16f, 16f, 16f) })
                        .flatComapMap({ mapOf(0 to it) }, { DataResult.success(it.values.firstOrNull() ?: return@flatComapMap DataResult.error { "Expected at least one value, but got empty!" }) })
                )
                    .optionalFieldOf("position", mapOf())
                    .forGetter(BoneAnimationMapping::position),

                Codec.withAlternative(
                    Codec.unboundedMap(
                        TICKS_AS_SECONDS_STRING_CODEC,
                        ExtraCodecs.VECTOR3F
                            .xmap({ it.mul(1f, 1f, -1f) }, { it.mul(1f, 1f, -1f) })
                    ),
                    ExtraCodecs.VECTOR3F
                        .xmap({ it.mul(1f, 1f, -1f) }, { it.mul(1f, 1f, -1f) })
                        .flatComapMap({ mapOf(0 to it) }, { DataResult.success(it.values.firstOrNull() ?: return@flatComapMap DataResult.error { "Expected at least one value, but got empty!" }) })
                )
                    .optionalFieldOf("rotation", mapOf())
                    .forGetter(BoneAnimationMapping::rotation),

                Codec.withAlternative(
                    Codec.unboundedMap(
                        TICKS_AS_SECONDS_STRING_CODEC,
                        ExtraCodecs.VECTOR3F
                    ),
                    ExtraCodecs.VECTOR3F
                        .flatComapMap({ mapOf(0 to it) }, { DataResult.success(it.values.firstOrNull() ?: return@flatComapMap DataResult.error { "Expected at least one value, but got empty!" }) })
                )
                    .optionalFieldOf("scale", mapOf())
                    .forGetter(BoneAnimationMapping::rotation)
            )
                .apply(instance, ::BoneAnimationMapping)
        }

        val BEDROCK_ANIMATION_CODEC = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.unboundedMap(Codec.STRING, TRANSFORMS_CODEC)
                    .xmap({ mappings ->
                        val parts = mutableMapOf<String, Map<Int, Matrix4f>>()
                        for ((partName, mapping) in mappings) {
                            val transforms = mutableMapOf<Int, Matrix4f>()

                            for ((tick, rotation) in mapping.rotation) {
                                transforms.computeIfAbsent(tick) { Matrix4f() }
                                    .rotateXYZ(rotation.copy().mul(Mth.DEG_TO_RAD))
                            }

                            for ((tick, scale) in mapping.scale) {
                                transforms.computeIfAbsent(tick) { Matrix4f() }
                                    .scale(scale)
                            }

                            for ((tick, position) in mapping.position) {
                                transforms.computeIfAbsent(tick) { Matrix4f() }
                                    .translate(position)
                            }

                            parts[partName] = transforms
                        }

                        parts.toMap()
                    }, { parts ->
                        val mappings = mutableMapOf<String, BoneAnimationMapping>()
                        for ((partName, partInfo) in parts) {
                            mappings[partName] = BoneAnimationMapping(
                                partInfo.mapValues { it.value.getTranslation(Vector3f()) },
                                partInfo.mapValues { it.value.getEulerAnglesXYZ(Vector3f()).mul(Mth.DEG_TO_RAD) },
                                partInfo.mapValues { it.value.getScale(Vector3f()) }
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
                TICKS_AS_SECONDS_FLOAT_CODEC
                    .fieldOf("animation_length")
                    .forGetter(Animation::maxLength),
                TICKS_AS_SECONDS_FLOAT_CODEC
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
