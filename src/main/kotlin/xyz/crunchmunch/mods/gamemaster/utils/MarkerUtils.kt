package xyz.crunchmunch.mods.gamemaster.utils

import net.minecraft.world.entity.Marker
import net.minecraft.world.item.component.CustomData
import xyz.crunchmunch.mods.gamemaster.mixin.accessors.EntityAccessor

val Marker.customData: CustomData
    get() {
        return (this as EntityAccessor).customData
    }