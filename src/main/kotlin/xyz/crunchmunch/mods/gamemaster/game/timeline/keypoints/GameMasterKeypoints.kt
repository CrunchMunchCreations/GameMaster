package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints

import com.mojang.serialization.MapCodec
import net.minecraft.core.Registry
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.GameMasterRegistries
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.timeline.BranchTimelineKeypoint
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.timeline.CategorizeTimelineKeypoint

object GameMasterKeypoints {
    val MULTIPLE = register("multiple", MultipleKeypoint.CODEC)
    val CATEGORIZE_TIMELINE = register("timeline/category", CategorizeTimelineKeypoint.CODEC)
    val BRANCH_TIMELINE = register("timeline/branch", BranchTimelineKeypoint.CODEC)
    val WAIT_TIME = register("wait_time", WaitTimeKeypoint.CODEC)
    val AWAIT_GAMEMASTER = register("await_gamemaster", AwaitGameMasterKeypoint.CODEC)

    fun init() {}

    private fun <T : TimelineKeypoint<T>> register(path: String, codec: MapCodec<T>): TimelineKeypoint.Type<T> {
        return Registry.register(GameMasterRegistries.KEYPOINT_TYPE, GameMaster.id(path), TimelineKeypoint.Type(codec))
    }
}
