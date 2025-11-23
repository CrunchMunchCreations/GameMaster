package xyz.crunchmunch.mods.gamemaster.commands

import de.phyrone.brig.wrapper.DSLCommandNode
import de.phyrone.brig.wrapper.executesNoResult
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.*
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.util.ProblemReporter
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import xyz.crunchmunch.mods.gamemaster.game.marker.GameMarkerManager
import xyz.crunchmunch.mods.gamemaster.utils.sendSuccess

fun DSLCommandNode<CommandSourceStack>.gameMarkerCommands() {
    literal("markers") {
        literal("count") {
            argument("type", ResourceLocationArgument.id()) {
                suggest { SharedSuggestionProvider.suggestResource(GameMarkerManager.TYPE_REGISTRY.keySet(), this) }

                executes { ctx ->
                    val typeId = ResourceLocationArgument.getId(ctx, "type")
                    val type = GameMarkerManager.TYPE_REGISTRY.getValue(typeId)

                    if (type == null) {
                        sendFailure(Component.literal("No game markers exist by the ID $typeId!"))
                        return@executes 0
                    }

                    val markers = GameMarkerManager.getMarkersByType(type)

                    sendSystemMessage(Component.literal("Game Markers for $typeId (${markers.size}):"))
                    markers.groupBy { it.entity.level().dimension().location() }
                        .forEach { (dimensionId, levelGameMarkers) ->
                            sendSystemMessage(Component.literal("$dimensionId (${levelGameMarkers.size} loaded)"))
                        }

                    markers.size
                }
            }
        }

        literal("refresh") {
            argument("level", DimensionArgument.dimension()) {
                executesNoResult { ctx ->
                    val level = DimensionArgument.getDimension(ctx, "level")

                    val currentMarkers = GameMarkerManager.gameMarkers.filter { it.entity.level() == level }
                    GameMarkerManager.refreshGameMarkers(level)
                    val newMarkers = GameMarkerManager.gameMarkers.filter { it.entity.level() == level }

                    val unique = newMarkers.filter { currentMarkers.none { b -> b.entity.uuid == it.entity.uuid } }
                    val removed = currentMarkers.filter { newMarkers.none { b -> b.entity.uuid == it.entity.uuid } }

                    sendSystemMessage(Component.literal("Reloaded game markers in level ${level.dimension().location()}! (previous: ${currentMarkers.size}, current: ${newMarkers.size}, added: ${unique.size}, unloaded: ${removed.size})"))
                }
            }

            executesNoResult { ctx ->
                val currentMarkers = GameMarkerManager.gameMarkers.toList()
                GameMarkerManager.refreshGameMarkers(level)
                val newMarkers = GameMarkerManager.gameMarkers.toList()

                val unique = newMarkers.filter { currentMarkers.none { b -> b.entity.uuid == it.entity.uuid } }
                val removed = currentMarkers.filter { newMarkers.none { b -> b.entity.uuid == it.entity.uuid } }

                sendSystemMessage(Component.literal("Reloaded all game markers! (previous: ${currentMarkers.size}, current: ${newMarkers.size}, added: ${unique.size}, unloaded: ${removed.size})"))
            }
        }

        literal("loadnew") {
            argument("level", DimensionArgument.dimension()) {
                executesNoResult { ctx ->
                    val level = DimensionArgument.getDimension(ctx, "level")

                    val loaded = GameMarkerManager.loadNewGameMarkers(level)
                    sendSystemMessage(Component.literal("Loaded ${loaded.first.size} new game markers, with ${loaded.second} failed."))
                }
            }

            executesNoResult { ctx ->
                val loaded = GameMarkerManager.loadNewGameMarkers()
                sendSystemMessage(Component.literal("Loaded ${loaded.first.size} new game markers, with ${loaded.second} failed."))
            }
        }

        literal("list") {
            argument("type", ResourceLocationArgument.id()) {
                suggest { SharedSuggestionProvider.suggestResource(GameMarkerManager.TYPE_REGISTRY.keySet(), this) }

                executes { ctx ->
                    val typeId = ResourceLocationArgument.getId(ctx, "type")
                    val type = GameMarkerManager.TYPE_REGISTRY.getValue(typeId)

                    if (type == null) {
                        sendFailure(Component.literal("No game markers exist by the ID $typeId!"))
                        return@executes 0
                    }

                    val markers = GameMarkerManager.getMarkersByType(type)

                    sendSystemMessage(Component.literal("Game Markers for $typeId (${markers.size}):"))
                    markers.groupBy { it.entity.level().dimension().location() }
                        .forEach { (dimensionId, levelGameMarkers) ->
                            sendSystemMessage(Component.literal("$dimensionId (${levelGameMarkers.size} loaded):"))

                            for (gameMarker in levelGameMarkers) {
                                sendSystemMessage(Component.literal(" - ${gameMarker.entity.position()} (${gameMarker})")
                                    .withStyle {
                                        it.withHoverEvent(HoverEvent.ShowText(Component.literal(gameMarker.entity.uuid.toString())))
                                            .withClickEvent(ClickEvent.SuggestCommand("/tp @s ${gameMarker.entity.uuid}"))
                                    })
                            }
                        }

                    markers.size
                }
            }
        }

        literal("swapentity") {
            argument("entities", EntityArgument.entities()) {
                argument("entity_type", ResourceKeyArgument.key(Registries.ENTITY_TYPE)) {
                    executesNoResult { ctx ->
                        val entities = EntityArgument.getEntities(ctx, "entities")
                        val entityType = ResourceArgument.getEntityType(ctx, "entity_type")

                        for (entity in entities) {
                            val newEntity = entityType.value().create(entity.level(), EntitySpawnReason.CONVERSION)
                                ?: throw IllegalStateException("Failed to create replacement entity for ${entity.uuid}!")

                            newEntity.snapTo(entity.position())

                            val value = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ctx.source.registryAccess())
                            entity.saveWithoutId(value)
                            entity.remove(Entity.RemovalReason.DISCARDED)
                            newEntity.load(TagValueInput.create(ProblemReporter.DISCARDING, ctx.source.registryAccess(), value.buildResult()))

                            entity.level().addFreshEntity(newEntity)
                        }

                        ctx.source.sendSuccess(Component.literal("Swapped ${entities.size} entities to ${entityType.registeredName}"), true)
                    }
                }
            }
        }
    }
}