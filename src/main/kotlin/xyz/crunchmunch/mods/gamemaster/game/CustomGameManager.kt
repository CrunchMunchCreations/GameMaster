package xyz.crunchmunch.mods.gamemaster.game

import net.fabricmc.fabric.api.event.registry.DynamicRegistries
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.fabricmc.fabric.api.event.registry.RegistryAttribute
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.game.metadata.CustomGameMetadata

object CustomGameManager {
    val GAME_REGISTRY_KEY: ResourceKey<Registry<CustomGameInitializer>> = ResourceKey.createRegistryKey<CustomGameInitializer>(GameMaster.id("games"))
    val GAME_REGISTRY: Registry<CustomGameInitializer> = FabricRegistryBuilder.createSimple(GAME_REGISTRY_KEY)
        .attribute(RegistryAttribute.OPTIONAL)
        .buildAndRegister()

    val GAME_METADATA_REGISTRY_KEY: ResourceKey<Registry<CustomGameMetadata>> = ResourceKey.createRegistryKey(GameMaster.id("custom_games"))
    /*val GAME_METADATA_REGISTRY: Registry<CustomGameMetadata> = FabricRegistryBuilder.createSimple(GAME_METADATA_REGISTRY_KEY)
        .attribute(RegistryAttribute.OPTIONAL)
        .buildAndRegister()*/

    init {
        DynamicRegistries.register(GAME_METADATA_REGISTRY_KEY, CustomGameMetadata.CODEC)
    }

    fun interface CustomGameInitializer {
        fun create(manager: GameManager<*, *, *>, gameId: ResourceLocation, metadata: CustomGameMetadata): CustomGame<*, *, *>
    }
}