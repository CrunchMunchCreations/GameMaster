package xyz.crunchmunch.mods.gamemaster.server.camera

import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.context.ContextKey

interface CameraTrackContext {
    val activeScene: ResourceKey<CameraScene>
    val level: ServerLevel
    val camera: CameraEntity
    val currentFrameTick: Int

    val activePlayers: Collection<ServerPlayer>
        get() = this.level.players().filter { it.getAttached(CameraManager.ACTIVE_SCENE_ATTACHMENT) == this.activeScene }

    fun <T : Any> hasData(key: ContextKey<T>): Boolean = this.getData(key) != null
    fun <T : Any> getData(key: ContextKey<T>): T?
    fun <T : Any> setData(key: ContextKey<T>, value: T?)
    fun <T : Any> removeData(key: ContextKey<T>) = this.setData(key, null)

    class Mutable(
        override val activeScene: ResourceKey<CameraScene>,
        override val level: ServerLevel,
        override val camera: CameraEntity,
    ) : CameraTrackContext {
        private val dataSet = mutableMapOf<ContextKey<*>, Any>()
        override var currentFrameTick: Int = 0

        override fun <T : Any> getData(key: ContextKey<T>): T? {
            return this.dataSet[key] as? T
        }

        override fun <T : Any> setData(key: ContextKey<T>, value: T?) {
            if (value != null)
                this.dataSet[key] = value
            else
                this.dataSet.remove(key)
        }

        fun reset() {
            this.currentFrameTick = 0
            this.dataSet.clear()
        }
    }
}
