package xyz.crunchmunch.mods.gamemaster.animator

import com.mojang.serialization.Codec
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.ResourceKey
import net.minecraft.util.ExtraCodecs
import org.joml.Matrix4f
import xyz.crunchmunch.mods.gamemaster.GameMaster

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

    @JvmField val START_TICK = register("animation/start_tick") {
        persistent(Codec.INT)
        initializer { 0 }
    }

    @JvmField val END_TICK = register("animation/end_tick") {
        persistent(Codec.INT)
        initializer { 0 }
    }

    @JvmField val LOCAL_TRANSFORMS = register("transform/local_transforms") {
        persistent(ExtraCodecs.MATRIX4F)
        initializer { Matrix4f() }
    }

    @JvmField val PREV_LOCAL_TRANSFORMS = register("transform/previous_local_transforms") {
        persistent(ExtraCodecs.MATRIX4F)
        initializer { Matrix4f() }
    }

    private fun <A> register(name: String, builder: AttachmentRegistry.Builder<A>.() -> Unit): AttachmentType<A> {
        return AttachmentRegistry.create<A>(GameMaster.id("animator/$name")) {
            builder.invoke(it)
        }
    }

    fun init() {}
}