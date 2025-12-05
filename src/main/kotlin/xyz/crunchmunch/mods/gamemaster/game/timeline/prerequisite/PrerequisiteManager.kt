package xyz.crunchmunch.mods.gamemaster.game.timeline.prerequisite

import com.mojang.serialization.MapCodec
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.fabricmc.fabric.api.event.registry.RegistryAttribute
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import xyz.crunchmunch.mods.gamemaster.GameMaster

object PrerequisiteManager {
    val REGISTRY_KEY: ResourceKey<Registry<TimelinePrerequisite.Type>> = ResourceKey.createRegistryKey(GameMaster.id("timeline/prerequisite"))
    val REGISTRY: Registry<TimelinePrerequisite.Type> = FabricRegistryBuilder.createSimple(REGISTRY_KEY)
        .attribute(RegistryAttribute.MODDED)
        .attribute(RegistryAttribute.OPTIONAL)
        .buildAndRegister()

    private fun <T : TimelinePrerequisite> register(path: String, codec: MapCodec<T>): TimelinePrerequisite.Type {
        return Registry.register(REGISTRY, GameMaster.id(path), TimelinePrerequisite.Type(codec))
    }
}