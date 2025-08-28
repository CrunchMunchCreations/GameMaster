package xyz.crunchmunch.mods.gamemaster.client.devtools.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class DebugRenderersScreen : Screen(Component.literal("Debug Renderers")) {
    protected lateinit var listWidget: DebugRendererListWidget

    override fun init() {
        this.listWidget = addRenderableWidget(DebugRendererListWidget(this.minecraft ?: Minecraft.getInstance(), this.width, this.height, 0, 22, this))
    }

    override fun isPauseScreen(): Boolean {
        return false
    }
}