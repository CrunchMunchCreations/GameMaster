package xyz.crunchmunch.mods.gamemaster.server.camera.keyframes

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.minecraft.core.PositionAndRotation
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import xyz.crunchmunch.mods.gamemaster.server.camera.CameraTrackContext
import xyz.crunchmunch.mods.gamemaster.utils.GameMasterCodecs
import xyz.crunchmunch.mods.gamemaster.utils.teleportTo

@JvmRecord
data class TeleportCameraKeyframe(val to: PositionAndRotation) : CameraKeyframe {
    override val codec: MapCodec<out CameraKeyframe>
        get() = CODEC

    override fun setup(context: CameraTrackContext) {
        context.camera.posRotInterpolationDuration = 0

        // forcefully sync the entity data IMMEDIATELY. otherwise it will just interpolate.
        for (player in PlayerLookup.tracking(context.camera)) {
            player.connection.send(ClientboundSetEntityDataPacket(context.camera.id, context.camera.entityData.packDirty() ?: emptyList()))
        }

        for (player in context.activePlayers) {
            player.teleportTo(context.level, this.to.position().x, this.to.position().y, this.to.position().z, this.to.yRot(), this.to.xRot(), false)
        }

        context.camera.teleportTo(context.level, this.to.position().x, this.to.position().y, this.to.position().z, this.to.yRot(), this.to.xRot())
        context.camera.needsSync = true
    }

    companion object {
        @JvmField val CODEC: MapCodec<TeleportCameraKeyframe> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                GameMasterCodecs.POSITION_AND_ROTATION.fieldOf("to")
                    .forGetter(TeleportCameraKeyframe::to),
            )
                .apply(instance, ::TeleportCameraKeyframe)
        }
    }
}
