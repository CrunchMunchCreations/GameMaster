package xyz.crunchmunch.mods.gamemaster

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType

object GameMasterAttachments {
    @JvmField val HAS_PASSENGERS: AttachmentType<Unit> = AttachmentRegistry.create(GameMaster.id("has_passengers"))

    fun init() {}
}