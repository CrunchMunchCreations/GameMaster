package xyz.crunchmunch.mods.gamemaster.scoreboard

import net.minecraft.network.chat.Component
import java.util.*

class SidebarLines(private val manager: SidebarManager) : MutableList<SidebarLine> {
    private val internal: MutableList<SidebarLine> = Collections.synchronizedList(mutableListOf<SidebarLine>())

    fun addLine(name: String, text: Component, priority: Int? = null) {
        val actualPriority = priority ?: ((this.minOfOrNull { it.priority } ?: 25) - 1)
        this.add(SidebarLine(this.manager, name, text, actualPriority).apply {
            this.isDirty = true
        })
    }

    fun addSeparator(priority: Int) {
        addOrModifyLine("separator_${priority}", Component.empty(), priority)
    }

    fun removeLine(name: String) {
        this.setLineVisibility(false, name)
    }

    fun modifyLine(name: String, text: Component) {
        val line = this.first { it.name == name }

        if (line.text != text) {
            line.text = text
            line.isDirty = true
        }

        if (!line.visible) {
            line.visible = true
            line.isDirty = true
        }
    }

    fun addOrModifyLine(name: String, text: Component, priority: Int) {
        if (this.none { it.name == name })
            this.addLine(name, text, priority)
        else
            this.modifyLine(name, text)
    }

    fun setLineVisibility(visible: Boolean, vararg names: String) {
        for (name in names) {
            val line = this.firstOrNull { it.name == name }
            line?.visible = visible
            line?.isDirty = true
        }

        this.manager.markDirty()
    }

    fun markAllUpdated() {
        for (line in this) {
            line.isDirty = false
        }
    }
    
    // Wrap all calls to the internal mutable list.
    override val size: Int
        get() = internal.size
    override fun add(element: SidebarLine): Boolean = internal.add(element)
    override fun remove(element: SidebarLine): Boolean = internal.remove(element)
    override fun addAll(elements: Collection<SidebarLine>): Boolean = internal.addAll(elements)
    override fun addAll(index: Int, elements: Collection<SidebarLine>): Boolean = internal.addAll(index, elements)
    override fun removeAll(elements: Collection<SidebarLine>): Boolean = internal.removeAll(elements)
    override fun retainAll(elements: Collection<SidebarLine>): Boolean = internal.retainAll(elements)
    override fun clear() = internal.clear()
    override fun set(index: Int, element: SidebarLine): SidebarLine = internal.set(index, element)
    override fun add(index: Int, element: SidebarLine) = internal.add(index, element)
    override fun removeAt(index: Int): SidebarLine = internal.removeAt(index)
    override fun listIterator(): MutableListIterator<SidebarLine> = internal.listIterator()
    override fun listIterator(index: Int): MutableListIterator<SidebarLine> = internal.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<SidebarLine> = internal.subList(fromIndex, toIndex)
    override fun isEmpty(): Boolean = internal.isEmpty()
    override fun contains(element: SidebarLine): Boolean = internal.contains(element)
    override fun containsAll(elements: Collection<SidebarLine>): Boolean = internal.containsAll(elements)
    override fun get(index: Int): SidebarLine = internal[index]
    override fun indexOf(element: SidebarLine): Int = internal.indexOf(element)
    override fun lastIndexOf(element: SidebarLine): Int = internal.lastIndexOf(element)
    override fun iterator(): MutableIterator<SidebarLine> = internal.iterator()
}