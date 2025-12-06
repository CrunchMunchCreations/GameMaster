package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.coroutines.channels.Channel
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineContext

class AwaitGameMasterKeypoint(val message: Component) : TimelineKeypoint<AwaitGameMasterKeypoint>(KeypointManager.AWAIT_GAMEMASTER) {
    override suspend fun execute(context: TimelineContext) {
        context.broadcastToGameMasters { player ->
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.UI, 0.75f, 1f)
            player.playNotifySound(SoundEvents.GENERIC_BURN, SoundSource.UI, 0.75f, 1f)
            player.displayClientMessage(Component.empty()
                .append("[GameMaster] ")
                .append(
                    Component.literal("GameMaster is awaiting confirmation before proceeding with the timeline!")
                        .withStyle(ChatFormatting.RED)
                )
                .append(
                    this.message
                ), false)
            player.displayClientMessage(Component.literal("[GameMaster] To proceed, run \"/gamemaster timeline proceed\".")
                .withStyle { style -> style.withClickEvent(ClickEvent.SuggestCommand("/gamemaster timeline proceed")) }, false)
        }

        val approver = channel.receive()

        context.broadcastToGameMasters { player ->
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.UI, 0.75f, 1f)
            player.displayClientMessage(Component.literal("[GameMaster] Timeline proceeding, approved by ")
                .append(approver.displayName ?: approver.name)
                .append("."), false)
        }
    }

    companion object {
        val CODEC: MapCodec<AwaitGameMasterKeypoint> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                ComponentSerialization.CODEC
                    .fieldOf("message")
                    .forGetter(AwaitGameMasterKeypoint::message)
            )
                .apply(instance, ::AwaitGameMasterKeypoint)
        }
        val channel = Channel<ServerPlayer>()
    }
}