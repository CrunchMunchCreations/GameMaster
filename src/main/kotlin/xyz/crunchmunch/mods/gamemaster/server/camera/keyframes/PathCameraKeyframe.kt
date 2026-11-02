package xyz.crunchmunch.mods.gamemaster.server.camera.keyframes

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.PositionAndRotation
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.entity.PositionPath
import xyz.crunchmunch.mods.gamemaster.server.camera.CameraTrackContext
import xyz.crunchmunch.mods.gamemaster.utils.GameMasterCodecs

@JvmRecord
data class PathCameraKeyframe(
    override val sustainTicks: Int,
    val to: PositionAndRotation,
) : CameraKeyframe {
    override val codec: MapCodec<out CameraKeyframe>
        get() = CODEC

    override fun setup(context: CameraTrackContext) {
        // 59 in the save code might just be an arbitrary limit, actually?? it might not actually be enforced in practice???
        context.camera.posRotInterpolationDuration = this.sustainTicks
        context.camera.moveOrInterpolateTo(PositionPath.Linear(this.to.position()), this.to.yRot(), this.to.xRot())
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
