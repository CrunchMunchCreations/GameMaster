package xyz.crunchmunch.mods.gamemaster.animator.util

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.joml.Vector3f
import org.joml.Vector3fc
import xyz.crunchmunch.mods.gamemaster.utils.copy

class StackingVector3f(backing: Vector3f = Vector3f()) {
    private val backing = backing.copy()
    private val stack = ObjectArrayList<Vector3fc>()

    fun peek(): Vector3f {
        return backing.copy()
    }

    fun push(vec: Vector3fc) {
        stack.push(vec)
        backing.add(vec)
    }

    fun pop(): Vector3fc {
        val vec = stack.pop()
        backing.sub(vec)
        return vec
    }
}