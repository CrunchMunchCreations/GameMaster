package xyz.crunchmunch.mods.gamemaster.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Marker
import net.minecraft.world.item.component.CustomData

var Marker.customData: CustomData
    get() {
        return this.get(DataComponents.CUSTOM_DATA) ?: CustomData.EMPTY
    }
    set(value) {
        this.setComponent(DataComponents.CUSTOM_DATA, value)
    }