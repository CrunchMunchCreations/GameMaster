package xyz.crunchmunch.mods.gamemaster.animator.animation

import xyz.crunchmunch.mods.gamemaster.animator.util.PosRotScale

data class Animation(
    val parts: Map<String, Map<Int, PosRotScale>>,
    val loop: LoopType,
    val maxLength: Int,
    val loopDelay: Int
) {
    fun getPartsAtTick(tick: Int): Map<String, PosRotScale> {
        val map = mutableMapOf<String, PosRotScale>()

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

    fun getPartsAfterTick(tick: Int): Map<String, Pair<Int, PosRotScale>> {
        val map = mutableMapOf<String, Pair<Int, PosRotScale>>()

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

    fun getPartPriorOrAtTick(tick: Int, partName: String): PosRotScale? {
        val keyframe = getKeyframePriorOrAtTick(tick, partName)
        return parts[partName]?.get(keyframe)
    }
}
