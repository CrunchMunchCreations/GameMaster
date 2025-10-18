package xyz.crunchmunch.mods.gamemaster.players

import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ProblemReporter
import net.minecraft.world.ItemStackWithSlot
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import xyz.crunchmunch.mods.gamemaster.GameMaster
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists

object InventoryManager {
    private val dir = GameMaster.resolvePath("inventory")
    private val alreadyStored = mutableListOf<UUID>()

    /**
     *
     */
    fun saveAndClearPlayerInventory(player: ServerPlayer, storageDir: String = "hub") {
        if (alreadyStored.contains(player.uuid)) {
            player.inventory.clearContent()
            return
        }

        val file = dir.resolve("$storageDir/${player.uuid}.dat")

        if (!file.exists()) {
            if (!file.parent.exists())
                file.parent.createDirectories()

            file.createFile()
        }

        val tag = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess())
        player.inventory.save(tag.list("Items", ItemStackWithSlot.CODEC))
        NbtIo.writeCompressed(tag.buildResult(), file)

        player.inventory.clearContent()
        alreadyStored.add(player.uuid)
    }

    fun loadPreviousPlayerInventory(player: ServerPlayer, storageDir: String = "hub") {
        val file = dir.resolve("$storageDir/${player.uuid}.dat")

        if (!file.exists()) {
            if (!file.parent.exists())
                file.parent.createDirectories()

            return
        }

        val tag = TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()))
        val items = tag.listOrEmpty("Items", ItemStackWithSlot.CODEC)

        player.inventory.clearContent()
        player.inventory.load(items)
        alreadyStored.remove(player.uuid)
    }
}