package xyz.crunchmunch.mods.gamemaster.animator

import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.ResourceKey
import xyz.crunchmunch.mods.gamemaster.animator.animation.MultiAnimationDefinition

open class AnimatableMarkerData(
    val model: ResourceKey<ModelDefinition>,
    val animations: ResourceKey<MultiAnimationDefinition>
) {
    companion object {
        val CODEC = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                ResourceKey.codec(AnimatableManager.MODEL_REGISTRY_KEY)
                    .fieldOf("model")
                    .forGetter(AnimatableMarkerData::model),
                ResourceKey.codec(AnimatableManager.ANIMATION_REGISTRY_KEY)
                    .fieldOf("animations")
                    .forGetter(AnimatableMarkerData::animations)
            )
                .apply(instance, ::AnimatableMarkerData)
        }
    }
}