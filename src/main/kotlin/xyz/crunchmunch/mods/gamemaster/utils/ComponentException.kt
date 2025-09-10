package xyz.crunchmunch.mods.gamemaster.utils

import net.minecraft.network.chat.Component

class ComponentException(val text: Component) : Exception(text.string)