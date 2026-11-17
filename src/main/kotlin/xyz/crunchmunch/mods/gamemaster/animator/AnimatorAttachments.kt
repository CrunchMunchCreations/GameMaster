package xyz.crunchmunch.mods.gamemaster.animator

import com.mojang.serialization.Codec
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.ResourceKey
import net.minecraft.util.ExtraCodecs
import org.joml.Vector3f
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.animator.util.InterpolatingTransforms

object AnimatorAttachments {
    @JvmField val MODEL_PART_ID = register("model/part_id") {
        persistent(Codec.STRING)
    }

    @JvmField val MODEL_KEY = register("model/id") {
        persistent(ResourceKey.codec(AnimatableManager.MODEL_REGISTRY_KEY))
    }

    @JvmField val ANIMATIONS_KEY = register("animation/id") {
        persistent(ResourceKey.codec(AnimatableManager.ANIMATION_REGISTRY_KEY))
    }

    @JvmField val ASSOCIATED_ENTITY = register("associated_entity") {
        persistent(UUIDUtil.CODEC)
    }

    @JvmField val LOCAL_TRANSFORMS = register("transform/local_transforms") {
        persistent(InterpolatingTransforms.CODEC)
        initializer { InterpolatingTransforms() }
    }

    @JvmField val OFFSET_ROTATIONS = register("transform/offset_rotations") {
        persistent(ExtraCodecs.VECTOR3F)
        initializer { Vector3f() }
    }

    @JvmField val PREV_OFFSET_ROTATIONS = register("transform/prev_offset_rotations") {
        persistent(ExtraCodecs.VECTOR3F)
        initializer { Vector3f() }
    }

    @JvmField val RESCALE = register("transform/rescale") {
        persistent(Codec.FLOAT)
        initializer { 1f }
    }

    private fun <A : Any> register(name: String, builder: AttachmentRegistry.Builder<A>.() -> Unit): AttachmentType<A> {
        return AttachmentRegistry.create(GameMaster.id("animator/$name")) {
            builder.invoke(it)
        }
    }

    fun init() {}
}
