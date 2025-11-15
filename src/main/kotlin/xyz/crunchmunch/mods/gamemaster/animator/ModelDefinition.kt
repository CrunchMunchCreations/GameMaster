package xyz.crunchmunch.mods.gamemaster.animator

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.item.Item
import org.joml.Vector3f
import java.util.*

data class ModelDefinition(
    val definitions: Map<String, ItemDefinition>,
    val parts: List<ModelPart>
) {
    fun collectAllParts(parts: List<ModelPart> = this.parts): List<ModelPart> {
        val list = mutableListOf<ModelPart>()

        for (part in parts) {
            list.addAll(collectAllParts(part.children))
            list.add(part)
        }

        return list
    }

    data class ItemDefinition(
        val item: Holder<Item>,
        val model: Optional<ResourceLocation>
    ) {
        companion object {
            val CODEC = RecordCodecBuilder.create { instance ->
                instance.group(
                    Item.CODEC.fieldOf("item")
                        .forGetter(ItemDefinition::item),
                    ResourceLocation.CODEC.optionalFieldOf("model")
                        .forGetter(ItemDefinition::model)
                )
                    .apply(instance, ::ItemDefinition)
            }
        }
    }

    data class ModelPart(
        val id: String,
        val origin: Vector3f,
        val children: List<ModelPart>
    ) {
        companion object {
            val CODEC = Codec.recursive("model_part") { codec ->
                RecordCodecBuilder.create { instance ->
                    instance.group(
                        Codec.STRING.fieldOf("id")
                            .forGetter(ModelPart::id),
                        ExtraCodecs.VECTOR3F.fieldOf("origin")
                            .xmap({ it.div(16f) }, { it.mul(16f) })
                            .forGetter(ModelPart::origin),
                        codec.listOf().optionalFieldOf("children", listOf())
                            .forGetter(ModelPart::children)
                    )
                        .apply(instance, ::ModelPart)
                }
            }
        }
    }

    companion object {
        val CODEC = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.unboundedMap(
                    Codec.STRING,
                    ItemDefinition.CODEC
                )
                    .fieldOf("definitions")
                    .forGetter(ModelDefinition::definitions),
                ModelPart.CODEC.listOf()
                    .fieldOf("model")
                    .forGetter(ModelDefinition::parts)
            )
                .apply(instance, ::ModelDefinition)
        }
    }
}