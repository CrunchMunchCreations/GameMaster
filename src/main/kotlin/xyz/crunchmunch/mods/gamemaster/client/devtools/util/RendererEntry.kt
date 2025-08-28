package xyz.crunchmunch.mods.gamemaster.client.devtools.util

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component

class RendererEntry(val renderer: Renderer) {
    var enabled = false

    fun render(poseStack: PoseStack, bufferSource: MultiBufferSource, cameraX: Double, cameraY: Double, cameraZ: Double) {
        if (enabled) {
            this.renderer.render(poseStack, bufferSource, cameraX, cameraY, cameraZ)
        }
    }

    fun toggle() {
        enabled = !enabled
    }

    val enabledText: Component
        get() {
            return if (enabled) ON else OFF
        }

    companion object {
        private val ON = Component.literal("On").withColor(0x74FF46)
        private val OFF = Component.literal("Off").withColor(0xC2C2C2)
    }

    fun interface Renderer {
        fun render(poseStack: PoseStack, bufferSource: MultiBufferSource, cameraX: Double, cameraY: Double, cameraZ: Double)
    }
}