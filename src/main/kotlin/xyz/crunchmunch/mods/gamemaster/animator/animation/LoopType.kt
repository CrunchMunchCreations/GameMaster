package xyz.crunchmunch.mods.gamemaster.animator.animation

import net.minecraft.util.StringRepresentable

enum class LoopType(private val serialized: String) : StringRepresentable {
    PLAY_ONCE("play_once"),
    HOLD_ON_LAST_FRAME("hold_on_last_frame"),
    LOOP("loop");

    override fun getSerializedName(): String {
        return serialized
    }

    companion object {
        val CODEC = StringRepresentable.fromEnum(LoopType::values)
    }
}