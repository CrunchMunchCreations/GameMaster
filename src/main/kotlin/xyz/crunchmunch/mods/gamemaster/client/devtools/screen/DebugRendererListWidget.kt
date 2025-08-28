package xyz.crunchmunch.mods.gamemaster.client.devtools.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.network.chat.Component
import xyz.crunchmunch.mods.gamemaster.client.GameMasterClient
import xyz.crunchmunch.mods.gamemaster.client.devtools.util.RendererEntry

class DebugRendererListWidget(mc: Minecraft, width: Int, height: Int, y: Int, itemHeight: Int, private val screen: DebugRenderersScreen) : ContainerObjectSelectionList<DebugRendererListWidget.Entry>(mc, width, height, y, itemHeight) {
    init {
        for ((name, entry) in GameMasterClient.renderers) {
            addEntry(Entry(name, entry, screen))
        }
    }

    class Entry(name: String, val rendererEntry: RendererEntry, private val screen: DebugRenderersScreen) : ContainerObjectSelectionList.Entry<Entry>() {
        private val name = Component.literal(name)
        private val clickableWidgets = mutableListOf<AbstractWidget>()

        init {
            clickableWidgets.add(Button.builder(rendererEntry.enabledText) {
                rendererEntry.toggle()
                it.message = rendererEntry.enabledText
            }
                .width(40)
                .pos(screen.width / 2 + 70, 0)
                .build()
            )
        }

        override fun narratables(): MutableList<out NarratableEntry> {
            return clickableWidgets
        }

        override fun children(): MutableList<out GuiEventListener> {
            return clickableWidgets
        }

        override fun render(guiGraphics: GuiGraphics, index: Int, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovering: Boolean, partialTick: Float) {
            for (widget in clickableWidgets) {
                widget.y = top
                widget.render(guiGraphics, mouseX, mouseY, partialTick)
            }

            guiGraphics.drawString(screen.font, this.name, left + 50, top + 6, -1, true)
        }
    }
}