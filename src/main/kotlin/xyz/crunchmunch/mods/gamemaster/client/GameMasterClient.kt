package xyz.crunchmunch.mods.gamemaster.client

import net.fabricmc.api.ClientModInitializer
import xyz.crunchmunch.mods.gamemaster.client.devtools.util.RendererEntry

class GameMasterClient : ClientModInitializer {
    override fun onInitializeClient() {

    }

    companion object {
        val renderers = mutableMapOf<String, RendererEntry>()
        fun addRenderer(name: String, renderer: RendererEntry.Renderer) {
            renderers[name] = RendererEntry(renderer)
        }
    }
}