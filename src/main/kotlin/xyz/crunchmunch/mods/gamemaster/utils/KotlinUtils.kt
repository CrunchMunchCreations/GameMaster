package xyz.crunchmunch.mods.gamemaster.utils

import com.mojang.brigadier.context.CommandContext
import de.phyrone.brig.wrapper.DSLCommandNode
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.TextColor
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.dialog.Dialog
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.ItemLore
import org.joml.Quaternionf
import org.joml.Vector3f
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.mixin.accessors.DisplayAccessor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import net.kyori.adventure.text.Component as AdventureComponent
import net.minecraft.network.chat.Component as VanillaComponent

fun Long.zeroPad(): String {
    return if (this <= 9)
        "0$this"
    else
        "$this"
}

fun Int.toThString(): String {
    return "$this" + when (this) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

public inline val Duration.ticks: Int get() = (this.inWholeMilliseconds / 50).toInt()
public inline val Int.ticks: Duration get() = ((this * 50).milliseconds)

fun Duration.getTimeString(): String {
    return "${(this.inWholeMinutes - (this.inWholeHours * 60)).zeroPad()}:${(this.inWholeSeconds - (this.inWholeMinutes * 60)).zeroPad()}"
}

fun ChatFormatting.toTextColor(): TextColor {
    return TextColor.fromLegacyFormat(this)!!
}

fun range(first: Int, second: Int): IntRange {
    return if (first <= second)
        first..second
    else
        second..first
}

// Display entity helpers
var Display.translation: Vector3f
    get() {
        return this.entityData.get(DisplayAccessor.getDataTranslationId())
    }
    set(value) {
        this.entityData.set(DisplayAccessor.getDataTranslationId(), value)
    }

var Display.leftRotation: Quaternionf
    get() {
        return this.entityData.get(DisplayAccessor.getDataLeftRotationId())
    }
    set(value) {
        this.entityData.set(DisplayAccessor.getDataLeftRotationId(), value)
    }

var Display.rightRotation: Quaternionf
    get() {
        return this.entityData.get(DisplayAccessor.getDataRightRotationId())
    }
    set(value) {
        this.entityData.set(DisplayAccessor.getDataRightRotationId(), value)
    }

var Display.scale: Vector3f
    get() {
        return this.entityData.get(DisplayAccessor.getDataScaleId())
    }
    set(value) {
        this.entityData.set(DisplayAccessor.getDataScaleId(), value)
    }

// Incredibly shortened variant of Entity#teleportTo
fun Entity.teleportTo(level: ServerLevel, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
    this.teleportTo(level, x, y, z, mutableSetOf(), yaw, pitch, true)
}

// Easier command DSL structures
fun <T : CommandSourceStack> CommandContext<T>.handle(function: CommandContext<T>.() -> VanillaComponent): Int {
    try {
        val component = function.invoke(this)
        this.source.sendSuccess({ component }, false)

        return 1
    } catch (e: ComponentException) {
        this.source.sendFailure(e.text.copy().withStyle(ChatFormatting.RED))
        return 0
    } catch (e: Throwable) {
        this.source.sendFailure(textComponent(e.message ?: e.localizedMessage, color = ChatFormatting.RED))
        return 0
    }
}

fun <T : CommandSourceStack> DSLCommandNode<T>.execute(function: CommandContext<T>.() -> VanillaComponent) {
    executes e@{ ctx ->
        try {
            val component = function.invoke(ctx)
            this.sendSuccess({ component }, false)

            return@e 1
        } catch (e: ComponentException) {
            this.sendFailure(e.text.copy().withStyle(ChatFormatting.RED))
            return@e 0
        } catch (e: Throwable) {
            this.sendFailure(textComponent(e.message ?: e.localizedMessage, color = ChatFormatting.RED))
            return@e 0
        }
    }
}

fun VanillaComponent.toAdventure(): AdventureComponent {
    return GameMaster.adventure.asAdventure(this)
}

fun AdventureComponent.toVanilla(): VanillaComponent {
    return GameMaster.adventure.asNative(this)
}

fun createItemLore(lines: List<VanillaComponent>): ItemLore {
    return ItemLore(lines.map {
        VanillaComponent.empty()
            .append(it.copy().withStyle { s -> s.withColor(ChatFormatting.GRAY).withItalic(false) })
    })
}

var ItemStack.customModelData: Int
    get() {
        if (!this.has(DataComponents.CUSTOM_MODEL_DATA))
            return 0

        return this.get(DataComponents.CUSTOM_MODEL_DATA)!!.floats[0].toInt()
    }
    set(value) {
        this.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(listOf(value.toFloat()), emptyList(), emptyList(), emptyList()))
    }

var ItemStack.itemModel: ResourceLocation?
    get() {
        if (!this.has(DataComponents.ITEM_MODEL))
            return null

        return this.get(DataComponents.ITEM_MODEL)
    }
    set(value) {
        if (value == null)
            this.remove(DataComponents.ITEM_MODEL)
        else
            this.set(DataComponents.ITEM_MODEL, value)
    }

var ItemStack.customModel: String?
    get() {
        if (!this.has(DataComponents.CUSTOM_MODEL_DATA))
            return null

        return this.get(DataComponents.CUSTOM_MODEL_DATA)!!.getString(0)
    }
    set(value) {
        if (value == null)
            this.remove(DataComponents.CUSTOM_MODEL_DATA)
        else
            this.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(emptyList(), emptyList(), listOf(value), emptyList()))
    }

fun ServerPlayer.openDialog(dialog: ResourceKey<Dialog>) {
    this.openDialog(this.registryAccess().lookupOrThrow(Registries.DIALOG).getOrThrow(dialog))
}

fun ServerPlayer.setTitleAnimationTimes(fadeIn: Int = 10, stay: Int = 70, fadeOut: Int = 20) {
    this.connection.send(ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut))
}

fun ServerPlayer.sendTitle(title: VanillaComponent = VanillaComponent.empty(), subtitle: VanillaComponent = VanillaComponent.empty()) {
    this.connection.send(ClientboundSetTitleTextPacket(title))
    this.connection.send(ClientboundSetSubtitleTextPacket(subtitle))
}