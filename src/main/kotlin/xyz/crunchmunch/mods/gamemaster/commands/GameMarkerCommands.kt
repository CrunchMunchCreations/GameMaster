package xyz.crunchmunch.mods.gamemaster.commands

import de.phyrone.brig.wrapper.DSLCommandNode
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import xyz.crunchmunch.mods.gamemaster.game.marker.GameMarkerManager

fun DSLCommandNode<CommandSourceStack>.gameMarkerCommands() {
    literal("markers") {
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
                    markers.groupBy { it.marker.level().dimension().location() }
                        .forEach { (dimensionId, levelGameMarkers) ->
                            sendSystemMessage(Component.literal("$dimensionId (${levelGameMarkers.size} loaded):"))

                            for (gameMarker in levelGameMarkers) {
                                sendSystemMessage(Component.literal(" - ${gameMarker.marker.position()} (${gameMarker.javaClass.simpleName})")
                                    .withStyle {
                                        it.withHoverEvent(HoverEvent.ShowText(Component.literal(gameMarker.marker.uuid.toString())))
                                            .withClickEvent(ClickEvent.SuggestCommand("/tp @s ${gameMarker.marker.uuid}"))
                                    })
                            }
                        }

                    markers.size
                }
            }
        }
    }
}