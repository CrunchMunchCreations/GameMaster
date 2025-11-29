package xyz.crunchmunch.mods.gamemaster.utils

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.*
import net.minecraft.resources.ResourceLocation

fun mergeComponents(vararg components: Component): Component {
    return ComponentUtils.formatList(components.toList(), Component.empty())
}

fun String.toVanilla(): MutableComponent {
    return Component.literal(this)
}

fun textComponent(text: String, color: ChatFormatting = ChatFormatting.RESET, bold: Boolean = false, italic: Boolean = false, font: ResourceLocation? = null, shadow: Int? = null): MutableComponent {
    return Component.literal(text)
        .withStyle {
            it.withColor(color)
                .withBold(bold)
                .withItalic(italic)
                .withFont(if (font != null) FontDescription.Resource(font) else FontDescription.DEFAULT)
                .run {
                    if (shadow != null)
                        withShadowColor(shadow)
                    else this
                }
        }
}

fun textComponent(text: String, color: Int, bold: Boolean = false, italic: Boolean = false, font: ResourceLocation? = null): MutableComponent {
    return Component.literal(text)
        .withStyle {
            it.withColor(color)
                .withBold(bold)
                .withItalic(italic)
                .withFont(if (font != null) FontDescription.Resource(font) else FontDescription.DEFAULT)
        }
}

fun textComponent(text: String, color: String, bold: Boolean = false, italic: Boolean = false, font: ResourceLocation? = null): MutableComponent {
    return Component.literal(text)
        .withStyle {
            it
                .withBold(bold)
                .withItalic(italic)
                .withFont(if (font != null) FontDescription.Resource(font) else FontDescription.DEFAULT)
                .run {
                    this.withColor(TextColor.parseColor(color).orThrow)
                }
        }
}

fun charComponent(char: Char, font: ResourceLocation? = null): MutableComponent {
    return textComponent(
        text = "$char",
        font = font
    )
}

