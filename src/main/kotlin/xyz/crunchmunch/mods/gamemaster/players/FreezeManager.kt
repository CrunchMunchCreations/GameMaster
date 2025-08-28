package xyz.crunchmunch.mods.gamemaster.players

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.*
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import xyz.crunchmunch.mods.gamemaster.GameMaster
import java.util.*

object FreezeManager {
    private val HIGH_PRIORITY_EVENT = GameMaster.id("high_priority")

    private val FROZEN_ATTRIBUTE_ID = GameMaster.id("freeze_player")
    private val FROZEN_ATTRIBUTE_MODIFIER = AttributeModifier(FROZEN_ATTRIBUTE_ID, -1.0, AttributeModifier.Operation.ADD_VALUE)
    
    private val frozenPlayers = mutableMapOf<UUID, Vec3>()

    init {
        ServerPlayConnectionEvents.JOIN.register { listener, sender, server ->
            if (frozenPlayers.contains(listener.player.uuid))
                freezePlayer(listener.player)
        }

        ServerTickEvents.END_WORLD_TICK.register { level ->
            for (player in level.players()) {
                if (frozenPlayers.contains(player.uuid)) {
                    val lastPos = frozenPlayers[player.uuid]!!
                    player.teleportTo(lastPos.x, lastPos.y, lastPos.z)
                    player.setDeltaMovement(0.0, 0.0, 0.0)
                }
            }
        }

        // Cancel all relevant events
        UseBlockCallback.EVENT.addPhaseOrdering(HIGH_PRIORITY_EVENT, Event.DEFAULT_PHASE)
        UseBlockCallback.EVENT.register(HIGH_PRIORITY_EVENT) { player, _, _, _ ->
            if (frozenPlayers.contains(player.uuid)) {
                return@register InteractionResult.FAIL
            }

            InteractionResult.PASS
        }

        UseItemCallback.EVENT.addPhaseOrdering(HIGH_PRIORITY_EVENT, Event.DEFAULT_PHASE)
        UseItemCallback.EVENT.register(HIGH_PRIORITY_EVENT) { player, level, hand ->
            if (frozenPlayers.contains(player.uuid)) {
                return@register InteractionResult.FAIL
            }

            InteractionResult.PASS
        }

        UseEntityCallback.EVENT.addPhaseOrdering(HIGH_PRIORITY_EVENT, Event.DEFAULT_PHASE)
        UseEntityCallback.EVENT.register(HIGH_PRIORITY_EVENT) { player, _, _, _, _ ->
            if (frozenPlayers.contains(player.uuid)) {
                return@register InteractionResult.FAIL
            }

            InteractionResult.PASS
        }

        AttackEntityCallback.EVENT.addPhaseOrdering(HIGH_PRIORITY_EVENT, Event.DEFAULT_PHASE)
        AttackEntityCallback.EVENT.register(HIGH_PRIORITY_EVENT) { player, _, _, _, _ ->
            if (frozenPlayers.contains(player.uuid)) {
                return@register InteractionResult.FAIL
            }

            InteractionResult.PASS
        }

        AttackBlockCallback.EVENT.addPhaseOrdering(HIGH_PRIORITY_EVENT, Event.DEFAULT_PHASE)
        AttackBlockCallback.EVENT.register(HIGH_PRIORITY_EVENT) { player, _, _, _, _ ->
            if (frozenPlayers.contains(player.uuid)) {
                return@register InteractionResult.FAIL
            }

            InteractionResult.PASS
        }

        ServerLivingEntityEvents.ALLOW_DAMAGE.addPhaseOrdering(HIGH_PRIORITY_EVENT, Event.DEFAULT_PHASE)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(HIGH_PRIORITY_EVENT) { entity, source, amount ->
            if (entity !is ServerPlayer) {
                return@register true
            }

            if (frozenPlayers.contains(entity.uuid)) {
                return@register false
            }

            true
        }
    }

    fun isFrozen(player: Player): Boolean {
        return frozenPlayers.contains(player.uuid)
    }

    /**
     * Freezes players in place, preventing any kind of movement.
     */
    fun freezePlayer(player: Player) {
        if (player.getAttribute(Attributes.MOVEMENT_SPEED)?.hasModifier(FROZEN_ATTRIBUTE_ID) != true)
            player.getAttribute(Attributes.MOVEMENT_SPEED)?.addTransientModifier(FROZEN_ATTRIBUTE_MODIFIER)

        if (player.getAttribute(Attributes.JUMP_STRENGTH)?.hasModifier(FROZEN_ATTRIBUTE_ID) != true)
            player.getAttribute(Attributes.JUMP_STRENGTH)?.addTransientModifier(FROZEN_ATTRIBUTE_MODIFIER)

        frozenPlayers[player.uuid] = Vec3(player.x, player.y, player.z)
    }

    /**
     * Unfreezes players, allowing them to move again.
     */
    fun unfreezePlayer(player: Player) {
        player.getAttribute(Attributes.MOVEMENT_SPEED)?.removeModifier(FROZEN_ATTRIBUTE_ID)
        player.getAttribute(Attributes.JUMP_STRENGTH)?.removeModifier(FROZEN_ATTRIBUTE_ID)
        frozenPlayers.remove(player.uuid)
    }
}