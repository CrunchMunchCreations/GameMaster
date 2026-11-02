package xyz.crunchmunch.mods.gamemaster.server

import com.mojang.serialization.Codec
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.fabricmc.fabric.api.event.registry.RegistryAttribute
import net.minecraft.core.Registry
import net.minecraft.nbt.NbtOps
import net.minecraft.network.chat.ClickEvent
import net.minecraft.resources.ResourceKey
import net.minecraft.server.dialog.action.Action
import net.minecraft.server.dialog.action.StaticAction
import net.minecraft.server.level.ServerPlayer
import xyz.crunchmunch.mods.gamemaster.GameMaster
import java.util.*

/**
 *
 */
interface CustomClickHandler {
    fun handle(player: ServerPlayer)
    val codec: Codec<out CustomClickHandler>

    companion object {
        @JvmField val REGISTRY_KEY: ResourceKey<Registry<Codec<out CustomClickHandler>>> = ResourceKey.createRegistryKey(GameMaster.id("click_handler"))
        @JvmField val REGISTRY: Registry<Codec<out CustomClickHandler>> = FabricRegistryBuilder.create(REGISTRY_KEY)
            .attribute(RegistryAttribute.OPTIONAL)
            .buildAndRegister()

        @JvmStatic
        fun asClickEvent(handler: CustomClickHandler): ClickEvent {
            val id = REGISTRY.getKey(handler.codec) ?: throw IllegalArgumentException("Click handler $handler was not registered!")
            return ClickEvent.Custom(id, Optional.of((handler.codec as Codec<CustomClickHandler>)
                .encodeStart(NbtOps.INSTANCE, handler).orThrow))
        }

        @JvmStatic
        fun asDialogAction(handler: CustomClickHandler): Action {
            return StaticAction(asClickEvent(handler))
        }
    }
}
