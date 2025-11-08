package xyz.crunchmunch.mods.gamemaster.game.marker

import net.minecraft.world.entity.Entity
import xyz.crunchmunch.mods.gamemaster.utils.customData

abstract class GameMarker<DATA : Any>(
    val type: GameMarkerType<out GameMarker<DATA>, DATA>,
    entity: Entity,
    val data: DATA
) {
    var entity: Entity = entity
        internal set

    protected var isUnloaded: Boolean = false
        private set

    private var dataUpdateQueuedForNextLoad = false

    open fun tick() {}

    internal fun remove() {
        onEntityUnload()
    }

    /**
     * Should be called every time the data has been updated, to save directly into the NBT data.
     */
    fun updateData() {
        if (this.isUnloaded) {
            this.dataUpdateQueuedForNextLoad = true
            return
        }

        this.entity.customData = this.entity.customData.update { tag ->
            tag.store(this.type.dataCodec, this.data)
        }

        this.dataUpdateQueuedForNextLoad = false
    }

    /**
     * Called when the game marker is removed.
     */
    protected open fun onRemove() {}

    internal fun entityLoaded() {
        this.isUnloaded = false
        onEntityLoad()

        if (this.dataUpdateQueuedForNextLoad) {
            this.updateData()
        }
    }

    internal fun entityUnloaded() {
        this.isUnloaded = true
        onEntityUnload()
    }

    /**
     * Called when the backing marker entity gets loaded. Also called when the game marker
     * is first initialized.
     */
    protected open fun onEntityLoad() {}

    /**
     * Called when the backing marker entity gets unloaded.
     */
    protected open fun onEntityUnload() {}

    override fun toString(): String {
        return "${this.javaClass.simpleName}"
    }
}