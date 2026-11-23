package xyz.crunchmunch.mods.gamemaster.server.camera.keyframes

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.minecraft.core.PositionAndRotation
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.Mth
import xyz.crunchmunch.mods.gamemaster.server.camera.CameraTrackContext
import xyz.crunchmunch.mods.gamemaster.utils.GameMasterCodecs
import xyz.crunchmunch.mods.gamemaster.utils.teleportTo

@JvmRecord
data class PathCameraKeyframe(
    override val sustainTicks: Int,
    val to: PositionAndRotation,
) : CameraKeyframe {
    override val codec: MapCodec<out CameraKeyframe>
        get() = CODEC

    override fun tick(context: CameraTrackContext) {
        for (player in context.activePlayers) {
            @Suppress("UsePropertyAccessSyntax")
            player.setCamera(context.camera)
        }

        if (context.currentFrameTick % 59 == 0) {
            val totalSegments = Mth.ceil(this.sustainTicks / 59.0)
            val currentSegment = Mth.floor(context.currentFrameTick / 59.0)

            println("Segment: ${currentSegment + 1} : $totalSegments")
            if (currentSegment < totalSegments) {
                val remainingTicks = Mth.clamp(this.sustainTicks - context.currentFrameTick, 0, 59)
                context.camera.posRotInterpolationDuration = remainingTicks

                // forcefully sync the entity data IMMEDIATELY. otherwise they will fucking die.
                for (player in PlayerLookup.tracking(context.camera)) {
                    player.connection.send(ClientboundSetEntityDataPacket(context.camera.id, context.camera.entityData.packDirty() ?: emptyList()))
                }

                val delta = remainingTicks / (this.sustainTicks - context.currentFrameTick).toDouble() // this was annoying to figure out.
                val endPos = context.camera.position().lerp(this.to.position(), delta)
                val endYaw = Mth.rotLerp(delta.toFloat(), context.camera.yRot, this.to.yRot())
                val endPitch = Mth.rotLerp(delta.toFloat(), context.camera.xRot, this.to.xRot())

                println("Syncing from ${context.camera.position()} (${context.camera.yRot}, ${context.camera.xRot}) -> $endPos ($endYaw, $endPitch) over ${context.camera.posRotInterpolationDuration} ticks (delta: $delta)")

                context.camera.teleportTo(context.level, endPos.x, endPos.y, endPos.z, endYaw, endPitch)
                context.camera.needsSync = true
            }
        }
    }

    // let's just make sure we actually reach where we're supposed to.
    override fun stop(context: CameraTrackContext) {
        context.camera.snapTo(this.to.position(), this.to.yRot(), this.to.xRot())
    }

    companion object {
        @JvmField val CODEC: MapCodec<PathCameraKeyframe> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                ExtraCodecs.POSITIVE_INT.fieldOf("interpolation_ticks")
                    .forGetter(PathCameraKeyframe::sustainTicks),
                GameMasterCodecs.POSITION_AND_ROTATION.fieldOf("to")
                    .forGetter(PathCameraKeyframe::to),
            )
                .apply(instance, ::PathCameraKeyframe)
        }
    }
}
