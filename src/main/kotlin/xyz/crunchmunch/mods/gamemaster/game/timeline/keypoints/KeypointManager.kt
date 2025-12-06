package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints

import com.mojang.serialization.MapCodec
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.fabricmc.fabric.api.event.registry.RegistryAttribute
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.timeline.BranchTimelineKeypoint
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.timeline.CategorizeTimelineKeypoint

object KeypointManager {
    val REGISTRY_KEY: ResourceKey<Registry<TimelineKeypoint.Type<*>>> = ResourceKey.createRegistryKey(GameMaster.id("timeline/keypoint"))
    val REGISTRY: Registry<TimelineKeypoint.Type<*>> = FabricRegistryBuilder.createSimple(REGISTRY_KEY)
        .attribute(RegistryAttribute.MODDED)
        .attribute(RegistryAttribute.OPTIONAL)
        .buildAndRegister()

    val MULTIPLE = register("multiple", MultipleKeypoint.CODEC)
    val CATEGORIZE_TIMELINE = register("timeline/category", CategorizeTimelineKeypoint.CODEC)
    val BRANCH_TIMELINE = register("timeline/branch", BranchTimelineKeypoint.CODEC)
    val WAIT_TIME = register("wait_time", WaitTimeKeypoint.CODEC)
    val AWAIT_GAMEMASTER = register("await_gamemaster", AwaitGameMasterKeypoint.CODEC)

    private fun <T : TimelineKeypoint<T>> register(path: String, codec: MapCodec<T>): TimelineKeypoint.Type<T> {
        return Registry.register(REGISTRY, GameMaster.id(path), TimelineKeypoint.Type(codec))
    }
}