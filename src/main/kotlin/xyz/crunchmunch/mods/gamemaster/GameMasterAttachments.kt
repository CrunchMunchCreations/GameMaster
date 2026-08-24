package xyz.crunchmunch.mods.gamemaster

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.core.UUIDUtil
import xyz.crunchmunch.mods.gamemaster.animator.AnimatableEntity
import xyz.crunchmunch.mods.gamemaster.animator.AnimatableEntityType
import java.util.*

object GameMasterAttachments {
    @JvmField val HAS_PASSENGERS: AttachmentType<Unit> = AttachmentRegistry.create(GameMaster.id("has_passengers"))
    @JvmField val ASSOCIATED_MARKER: AttachmentType<UUID> = AttachmentRegistry.create(GameMaster.id("associated_marker")) {
        it.persistent(UUIDUtil.CODEC)
    }

    @JvmField val ANIMATABLE_ENTITY_TYPE: AttachmentType<AnimatableEntityType<*>> = AttachmentRegistry.create(GameMaster.id("animatable_entity/type")) {
        it.persistent(GameMasterRegistries.ANIMATABLE_ENTITY.byNameCodec())
    }

    @JvmField val ANIMATABLE_ENTITY: AttachmentType<AnimatableEntity<*>> = AttachmentRegistry.create(GameMaster.id("animatable_entity"))

    fun init() {}
}
