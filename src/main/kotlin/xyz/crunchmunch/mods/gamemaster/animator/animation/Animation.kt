package xyz.crunchmunch.mods.gamemaster.animator.animation

import org.joml.Matrix4f

data class Animation(
    val parts: Map<String, Map<Int, Matrix4f>>,
    val loop: LoopType,
    val maxLength: Int,
    val loopDelay: Int
) {
    fun getPartsAtTick(tick: Int): Map<String, Matrix4f> {
        val map = mutableMapOf<String, Matrix4f>()

        for ((partName, frameMap) in parts) {
            if (!frameMap.contains(tick))
                continue

            map[partName] = frameMap[tick]!!
        }

        return map
    }

    fun getInterpolationDuration(tick: Int, partName: String): Int {
        val part = parts[partName] ?: return 0
        return ((part.keys.sorted().firstOrNull { it > tick }) ?: return 0) - tick
    }

    fun getKeyframeAfterTick(tick: Int, partName: String): Int {
        val part = parts[partName] ?: return 0
        return ((part.keys.sorted().firstOrNull { it > tick }) ?: return 0)
    }

    fun getPartsAfterTick(tick: Int): Map<String, Pair<Int, Matrix4f>> {
        val map = mutableMapOf<String, Pair<Int, Matrix4f>>()

        for ((partName, frameMap) in parts) {
            val nextFrame = (frameMap.keys.sorted().firstOrNull { it > tick }) ?: continue

            map[partName] = Pair(nextFrame, frameMap[nextFrame]!!)
        }

        return map
    }

    fun getKeyframePriorOrAtTick(tick: Int, partName: String): Int {
        val part = parts[partName] ?: return 0
        return ((part.keys.sorted().lastOrNull { it <= tick })) ?: 0
    }

    fun getPartPriorOrAtTick(tick: Int, partName: String): Matrix4f? {
        val keyframe = getKeyframePriorOrAtTick(tick, partName)
        return parts[partName]?.get(keyframe)
    }
}
