package xyz.crunchmunch.mods.gamemaster.utils

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import net.minecraft.network.chat.Component

object DynamicCommandExceptions {
    val RESOURCE_KEY = DynamicCommandExceptionType { value ->
        Component.literal("Unknown resource key $value!")
    }
}
