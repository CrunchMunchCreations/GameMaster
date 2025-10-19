package xyz.crunchmunch.mods.gamemaster.game.marker

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.fabricmc.fabric.api.event.registry.RegistryAttribute
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Marker
import net.minecraft.world.phys.Vec3
import xyz.crunchmunch.mods.gamemaster.GameMaster
import xyz.crunchmunch.mods.gamemaster.utils.customData
import java.util.*

object GameMarkerManager {
    @JvmField val TYPE_REGISTRY_KEY: ResourceKey<Registry<GameMarkerType<*, *>>> = ResourceKey.createRegistryKey(GameMaster.id("game_marker_types"))
    @JvmField val TYPE_REGISTRY: Registry<GameMarkerType<*, *>> = FabricRegistryBuilder.createSimple(TYPE_REGISTRY_KEY)
        .attribute(RegistryAttribute.OPTIONAL)
        .buildAndRegister()

    private val trackedGameMarkers = Collections.synchronizedList<GameMarker<*>>(mutableListOf())

    /**
     * A list of all the game markers that are currently loaded and tracked.
     */
    val gameMarkers: List<GameMarker<*>> = Collections.unmodifiableList(trackedGameMarkers)

    init {
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity is Marker) {
                // Make sure to reclaim the game marker entity, because if the entity
                // was unloaded, our entity object would no longer synchronize correctly.
                synchronized(gameMarkers) {
                    for (gameMarker in gameMarkers) {
                        if (gameMarker.entity.uuid == entity.uuid) {
                            gameMarker.entity = entity
                            gameMarker.entityLoaded()

                            return@register
                        }
                    }
                }

                // If the reclaiming didn't happen, let's go through the entity and try to
                // create a new game marker.
                tryLoadMarker<GameMarker<Any>, Any>(entity, world)
            }
        }

        ServerEntityEvents.ENTITY_UNLOAD.register { entity, world ->
            if (entity is Marker) {
                // Make sure to notify the game marker that the marker has unloaded.
                synchronized(gameMarkers) {
                    for (gameMarker in gameMarkers) {
                        if (gameMarker.entity.uuid == entity.uuid) {
                            gameMarker.entityUnloaded()

                            // If the entity was actually deleted, we need to handle that.
                            if (entity.removalReason != null && entity.removalReason != Entity.RemovalReason.UNLOADED_TO_CHUNK && entity.removalReason != Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
                                remove(gameMarker)
                            }

                            return@register
                        }
                    }
                }
            }
        }

        ServerTickEvents.END_WORLD_TICK.register { level ->
            synchronized(gameMarkers) {
                for (gameMarker in gameMarkers) {
                    if (gameMarker.entity.level() == level) {
                        gameMarker.tick()
                    }
                }
            }
        }
    }

    /**
     * Creates a game marker at the specified position with the data provided.
     */
    fun <M : GameMarker<D>, D : Any> create(type: GameMarkerType<M, D>, data: D, level: ServerLevel, pos: Vec3): M {
        val marker = EntityType.MARKER.create(level, EntitySpawnReason.TRIGGERED)
            ?: throw IllegalStateException("Failed to load marker at ${level.dimension()}/$pos!")

        marker.snapTo(pos)
        marker.customData.update { tag ->
            tag.store("type", TYPE_REGISTRY.byNameCodec(), type)
            tag.store(type.dataCodec, data)
        }

        level.addFreshEntity(marker)

        val gameMarker = type.initializer.invoke(marker, data)
        trackedGameMarkers.add(gameMarker)

        return gameMarker
    }

    /**
     * Used for when we want to remove a game marker.
     */
    fun remove(gameMarker: GameMarker<*>) {
        GameMaster.server.submit {
            gameMarker.remove()
            this.trackedGameMarkers.remove(gameMarker)
        }
    }

    /**
     * Gets all game markers that match a specific type.
     */
    fun <M : GameMarker<D>, D : Any> getMarkersByType(type: GameMarkerType<M, D>): Collection<M> {
        return this.gameMarkers.filter { it.type == type } as Collection<M>
    }

    private fun <M : GameMarker<D>, D : Any> tryLoadMarker(marker: Marker, level: ServerLevel): M? {
        val data = marker.customData.copyTag()

        val type = (data.read("type", TYPE_REGISTRY.byNameCodec()).orElse(null)) as? GameMarkerType<M, D>
        if (type == null) {
            GameMaster.logger.error("Failed to load game marker located at ${level.dimension().location()}/${marker.position()}!")

            return null
        }

        val typeId = TYPE_REGISTRY.getKey(type)

        // Now to load the game marker.
        try {
            val data = data.read(type.dataCodec).orElseThrow()

            val gameMarker = type.initializer.invoke(marker, data)
            trackedGameMarkers.add(gameMarker)

            return gameMarker
        } catch (e: Throwable) {
            GameMaster.logger.error("Failed to initialize game marker with type $typeId, located at ${level.dimension().location()}/${marker.position()}!")
            e.printStackTrace()
        }

        return null
    }

    fun init() {}
}