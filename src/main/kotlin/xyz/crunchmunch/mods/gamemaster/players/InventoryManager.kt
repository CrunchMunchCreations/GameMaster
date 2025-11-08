package xyz.crunchmunch.mods.gamemaster.players

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ProblemReporter
import net.minecraft.world.ItemStackWithSlot
import net.minecraft.world.entity.EntityEquipment
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.mixin.accessors.LivingEntityAccessor
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.io.path.*

object InventoryManager {
    const val DEFAULT_STORAGE_DIR = "hub"

    private val dir = GameMaster.resolvePath("inventory")
    private val alreadyStored = mutableMapOf<String, MutableList<UUID>>()
    private val storedCodec: Codec<Map<String, MutableList<UUID>>> = Codec.unboundedMap(
        Codec.STRING, UUIDUtil.STRING_CODEC.listOf().xmap({ it.toMutableList() }, { it.toList() })
    )

    private val existingStoredPath = dir.resolve("stored.json")

    private var isDirty = false

    init {
        // load existing
        if (existingStoredPath.exists()) {
            val json = JsonParser.parseReader(existingStoredPath.reader(options = arrayOf(StandardOpenOption.READ)))
            alreadyStored.putAll(storedCodec.decode(JsonOps.INSTANCE, json).orThrow.first)
        }

        // save cycles
        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (server.tickCount % 100 == 0) {
                saveStored()
            }
        }

        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            saveStored()
        }
    }

    fun getStorageKeys(uuid: UUID): Collection<String> {
        return this.alreadyStored.filter { it.value.contains(uuid) }.keys
    }

    private fun saveStored() {
        if (!isDirty)
            return

        if (!existingStoredPath.parent.exists())
            existingStoredPath.createParentDirectories()

        val data = storedCodec
            .encodeStart(JsonOps.INSTANCE, this.alreadyStored)
            .orThrow

        existingStoredPath.writeText(data.toString(), options = arrayOf(StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE))
    }

    /**
     *
     */
    fun saveAndClearPlayerInventory(player: ServerPlayer, storageDir: String = DEFAULT_STORAGE_DIR) {
        for ((_, players) in alreadyStored) {
            if (players.contains(player.uuid)) {
                player.inventory.clearContent()

                return
            }
        }

        val file = dir.resolve("$storageDir/${player.uuid}.dat")

        if (!file.exists()) {
            if (!file.parent.exists())
                file.parent.createDirectories()

            file.createFile()
        }

        // save NBT data
        val tag = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess())
        player.inventory.save(tag.list("Items", ItemStackWithSlot.CODEC))
        tag.store("equipment", EntityEquipment.CODEC, (player as LivingEntityAccessor).equipment)
        tag.store("carried", ItemStack.OPTIONAL_CODEC, player.inventoryMenu.carried)
        tag.store("craft", ItemStack.OPTIONAL_CODEC.listOf(), player.inventoryMenu.craftSlots.toList())

        NbtIo.writeCompressed(tag.buildResult(), file)

        (player as LivingEntityAccessor).equipment.clear()
        player.inventory.clearContent()

        // anti-Simon measures
        player.inventoryMenu.carried = ItemStack.EMPTY

        // anti-U.N.Own measures
        player.inventoryMenu.craftSlots.clearContent()
        player.inventoryMenu.craftSlots.setChanged()

        player.inventoryMenu.sendAllDataToRemote()

        alreadyStored.computeIfAbsent(storageDir) { mutableListOf() }
            .add(player.uuid)
        isDirty = true
    }

    fun loadPreviousPlayerInventory(player: ServerPlayer, storageDir: String = DEFAULT_STORAGE_DIR) {
        val file = dir.resolve("$storageDir/${player.uuid}.dat")

        if (!file.exists()) {
            if (!file.parent.exists())
                file.parent.createDirectories()

            return
        }

        val tag = TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()))
        val items = tag.listOrEmpty("Items", ItemStackWithSlot.CODEC)

        (player as LivingEntityAccessor).equipment.clear()
        player.inventory.clearContent()
        player.inventory.load(items)
        tag.read("equipment", EntityEquipment.CODEC)
            .ifPresent { (player as LivingEntityAccessor).equipment.setAll(it) }

        player.inventoryMenu.carried = tag.read("carried", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY)
        tag.read("craft", ItemStack.OPTIONAL_CODEC.listOf())
            .ifPresent { items ->
                for ((index, stack) in items.withIndex()) {
                    player.inventoryMenu.craftSlots.setItem(index, stack)
                }
            }
        player.inventoryMenu.craftSlots.setChanged()
        player.inventoryMenu.sendAllDataToRemote()

        alreadyStored.computeIfAbsent(storageDir) { mutableListOf() }
            .remove(player.uuid)
        isDirty = true
    }
}