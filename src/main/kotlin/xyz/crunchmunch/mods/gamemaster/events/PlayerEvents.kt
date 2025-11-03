package xyz.crunchmunch.mods.gamemaster.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack

interface PlayerEvents {
    fun interface DropItemEvent {
        fun onDropItem(player: ServerPlayer, item: ItemEntity): Boolean
    }

    fun interface CraftItemEvent {
        fun onCraftItem(player: Player, item: ItemStack, slots: CraftingContainer)
    }

    fun interface GenericPlayerEvent {
        fun onGenericEvent(player: ServerPlayer)
    }

    companion object {
        /**
         * Return true if you're cancelling the event.
         */
        @JvmField
        val DROP_ITEM: Event<DropItemEvent> = EventFactory.createArrayBacked(DropItemEvent::class.java) { callbacks ->
            DropItemEvent { player, item ->
                for (callback in callbacks) {
                    if (callback.onDropItem(player, item))
                        return@DropItemEvent true
                }

                false
            }
        }

        /**
         * This runs later than Fabric's existing [net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN] event,
         * and hooks into Vanilla's own event system.
         */
        @JvmField
        val JOIN: Event<GenericPlayerEvent> = createGenericEvent()

        /**
         * This runs later than Fabric's existing [net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT] event,
         * and hooks into Vanilla's own event system.
         */
        @JvmField
        val LEAVE: Event<GenericPlayerEvent> = createGenericEvent()

        @JvmField
        val CRAFT_ITEM: Event<CraftItemEvent> = EventFactory.createArrayBacked(CraftItemEvent::class.java) { callbacks ->
            CraftItemEvent { player, item, slots ->
                for (callback in callbacks) {
                    callback.onCraftItem(player, item, slots)
                }
            }
        }

        @JvmField
        val START_TICK: Event<GenericPlayerEvent> = createGenericEvent()

        @JvmField
        val END_TICK: Event<GenericPlayerEvent> = createGenericEvent()

        private fun createGenericEvent(): Event<GenericPlayerEvent> {
            return EventFactory.createArrayBacked(GenericPlayerEvent::class.java) { callbacks ->
                GenericPlayerEvent { player ->
                    for (callback in callbacks) {
                        callback.onGenericEvent(player)
                    }
                }
            }
        }
    }
}