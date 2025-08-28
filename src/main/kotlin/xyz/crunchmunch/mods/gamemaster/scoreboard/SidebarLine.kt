package xyz.crunchmunch.mods.gamemaster.scoreboard

import net.minecraft.network.chat.Component

data class SidebarLine(
    val manager: SidebarManager,
    val name: String,
    var text: Component,
    val priority: Int,
    var visible: Boolean = true
) {
    var isDirty: Boolean = false
        set(value) {
            field = value

            if (value)
                manager.markDirty()
        }
}