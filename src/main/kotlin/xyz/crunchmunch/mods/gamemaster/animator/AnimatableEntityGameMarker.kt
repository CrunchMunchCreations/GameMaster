package xyz.crunchmunch.mods.gamemaster.animator

import net.minecraft.world.entity.Entity
import xyz.crunchmunch.mods.gamemaster.game.marker.GameMarker
import xyz.crunchmunch.mods.gamemaster.game.marker.GameMarkerType

abstract class AnimatableEntityGameMarker<D : AnimatableMarkerData>(type: GameMarkerType<out AnimatableEntityGameMarker<D>, D>, entity: Entity, data: D) : GameMarker<D>(type, entity, data) {

}