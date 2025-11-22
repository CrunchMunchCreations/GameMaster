package xyz.crunchmunch.mods.gamemaster

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.core.UUIDUtil
import java.util.*

object GameMasterAttachments {
    @JvmField val HAS_PASSENGERS: AttachmentType<Unit> = AttachmentRegistry.create(GameMaster.id("has_passengers"))
    @JvmField val ASSOCIATED_MARKER: AttachmentType<UUID> = AttachmentRegistry.create(GameMaster.id("associated_marker")) {
        it.persistent(UUIDUtil.CODEC)
    }

    fun init() {}
}