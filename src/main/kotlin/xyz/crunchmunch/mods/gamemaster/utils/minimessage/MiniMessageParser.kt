package xyz.crunchmunch.mods.gamemaster.utils.minimessage

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.StringReader
import com.mojang.datafixers.util.Either
import net.minecraft.commands.arguments.selector.EntitySelector
import net.minecraft.core.ClientAsset
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.TagParser
import net.minecraft.network.chat.*
import net.minecraft.network.chat.contents.NbtContents
import net.minecraft.network.chat.contents.data.BlockDataSource
import net.minecraft.network.chat.contents.data.EntityDataSource
import net.minecraft.network.chat.contents.data.StorageDataSource
import net.minecraft.network.chat.contents.objects.AtlasSprite
import net.minecraft.network.chat.contents.objects.PlayerSprite
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.util.ARGB
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.component.ResolvableProfile
import xyz.crunchmunch.mods.gamemaster.utils.readBooleanOr
import xyz.crunchmunch.mods.gamemaster.utils.readStringUntilOrEnd
import java.awt.Color
import java.net.URI
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A MiniMessage-based parser, completely disconnected from Adventure and is mostly cleanroom written, only referencing the documentation and the MiniMessage Viewer,
 * because I wanted to try to figure out how to make this properly work.
 * The only aspect that isn't is the regex, that's it.
 */
object MiniMessageParser {
    private class TagElement(
        val tag: String,
        val args: String?,
        val isInvert: Boolean,
        lookup: HolderLookup.Provider,
    ) {
        private val style: Style = run {
            val tagReader = StringReader(args ?: "")

            when (tag) {
                "color", "colour", "c" -> {
                    val color = parseColor(tagReader.remaining) ?: throw IllegalArgumentException("Invalid color ${tagReader.remaining}!")
                    Style.EMPTY.withColor(color)
                }
                "reset" -> {
                    Style.EMPTY
                }
                "shadow" -> {
                    if (isInvert)
                        Style.EMPTY.withoutShadow()
                    else {
                        var component = tagReader.readStringUntilOrEnd(':')
                        var alpha = tagReader.readFloat()

                        if (component.startsWith('#') && component.length == 9) { // #RRGGBBAA
                            alpha = component.takeLast(2).toInt() / 255f
                            component = component.take(7)
                        }

                        val color = parseColor(component) ?: throw IllegalArgumentException("Invalid shadow color ${component}!")
                        Style.EMPTY.withShadowColor(ARGB.multiplyAlpha(ARGB.opaque(color.value), alpha))
                    }
                }

                "bold", "b" -> Style.EMPTY.withBold(tagReader.readBooleanOr(!isInvert))
                "italic", "em", "i" -> Style.EMPTY.withItalic(tagReader.readBooleanOr(!isInvert))
                "underlined", "u" -> Style.EMPTY.withUnderlined(tagReader.readBooleanOr(!isInvert))
                "strikethrough", "st" -> Style.EMPTY.withStrikethrough(tagReader.readBooleanOr(!isInvert))
                "obfuscated", "obf" -> Style.EMPTY.withObfuscated(tagReader.readBooleanOr(!isInvert))

                "click" -> {
                    val eventType = tagReader.readStringUntilOrEnd(':')
                    val clickEvent = when (eventType.lowercase()) {
                        "change_page" -> ClickEvent.ChangePage(tagReader.readInt())
                        "copy_to_clipboard" -> ClickEvent.CopyToClipboard(tagReader.remaining)
                        "open_file" -> ClickEvent.OpenFile(tagReader.remaining)
                        "open_url" -> ClickEvent.OpenUrl(URI.create(tagReader.remaining))
                        "run_command" -> ClickEvent.RunCommand(tagReader.remaining)
                        "show_dialog" -> {
                            val id = tagReader.readId()
                            ClickEvent.ShowDialog(lookup.getOrThrow(ResourceKey.create(Registries.DIALOG, id)))
                        }
                        "suggest_command" -> ClickEvent.SuggestCommand(tagReader.remaining)
                        "custom" -> {
                            val id = tagReader.readId()
                            val payloadString = if (tagReader.canRead())
                                tagReader.remaining
                            else ""

                            val payload = if (payloadString.isBlank())
                                Optional.empty()
                            else
                                Optional.of(TagParser.create(NbtOps.INSTANCE).parseFully(payloadString))

                            ClickEvent.Custom(id, payload)
                        }
                        else -> throw IllegalArgumentException("Unknown click event type $eventType!")
                    }

                    Style.EMPTY.withClickEvent(clickEvent)
                }
                "hover" -> {
                    val eventType = tagReader.readStringUntilOrEnd(':')
                    val hoverEvent = when (eventType.lowercase()) {
                        "show_text" -> {
                            val message = tagReader.readOptionallyQuotedString()
                            HoverEvent.ShowText(parse(message, lookup))
                        }

                        "show_item" -> {
                            val itemId = tagReader.readId()
                            val item = lookup.getOrThrow(ResourceKey.create(Registries.ITEM, itemId))

                            val count = if (tagReader.canRead()) tagReader.readInt() else null
                            if (count != null) {
                                var currentKey: Identifier? = null
                                var isValue = false
                                val componentBuilder = DataComponentPatch.builder()
                                while (tagReader.canRead()) {
                                    if (!isValue) {
                                        currentKey = tagReader.readId()
                                        isValue = true
                                    } else {
                                        val valueSnbt = tagReader.readOptionallyQuotedString()
                                        val tag = TagParser.create(NbtOps.INSTANCE).parseFully(valueSnbt)
                                        val componentType = lookup.getOrThrow(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, currentKey!!))
                                        val component: DataComponentType<Any> = componentType.value() as DataComponentType<Any>

                                        componentBuilder.set(component, component.codec()?.parse(NbtOps.INSTANCE, tag)!!)
                                        isValue = false
                                    }
                                }

                                if (isValue)
                                    throw IllegalStateException("Tag $tag does not have a valid closing item component value!")

                                HoverEvent.ShowItem(ItemStackTemplate(item, 1, componentBuilder.build()))
                            } else {
                                HoverEvent.ShowItem(ItemStackTemplate(item, 1, DataComponentPatch.EMPTY))
                            }
                        }

                        "show_entity" -> {
                            val entityId = tagReader.readId()
                            val uuid = UUID.fromString(tagReader.readStringUntilOrEnd(':'))
                            val name = if (tagReader.canRead())
                                parse(tagReader.remaining, lookup)
                            else null

                            HoverEvent.ShowEntity(HoverEvent.EntityTooltipInfo(lookup.getOrThrow(ResourceKey.create(Registries.ENTITY_TYPE, entityId)).value(), uuid, name))
                        }

                        else -> throw IllegalArgumentException("Unknown hover event type $eventType!")
                    }

                    Style.EMPTY.withHoverEvent(hoverEvent)
                }

                "insert" -> Style.EMPTY.withInsertion(tagReader.remaining)

                "rainbow", "gradient" -> Style.EMPTY
                "transition" -> {
                    var phase = 0f
                    val colors = mutableListOf<TextColor>()

                    val split = args?.split(":") ?: listOf()

                    for ((index, string) in split.withIndex()) {
                        val color = parseColor(string)
                        if (color != null) {
                            colors.add(color)
                        } else if (index == split.lastIndex) {
                            phase = string.toFloat()
                            if (phase !in 0.0..1.0)
                                throw IllegalArgumentException("Invalid phase $phase!")
                        }
                    }

                    if (colors.isEmpty())
                        return@run Style.EMPTY

                    val actualPhase = phase * colors.size.toFloat()
                    val nearestLowerIndex = floor(actualPhase).toInt().coerceAtLeast(0)
                    val nearestHigherIndex = ceil(actualPhase).toInt().coerceAtMost(colors.size - 1)

                    Style.EMPTY.withColor(ARGB.srgbLerp(actualPhase - nearestLowerIndex, colors[nearestLowerIndex].value, colors[nearestHigherIndex].value))
                }

                else -> {
                    val color = parseColor(tagReader.remaining)
                    if (color != null) {
                        Style.EMPTY.withColor(color)
                    } else {
                        throw IllegalArgumentException("Unknown tag ${this.tag}!")
                    }
                }
            }
        }

        fun prepareStyle(phase: Float): Style {
            if (tag.lowercase() == "rainbow") {
                val isInvert = args?.startsWith('!') == true
                val additionalPhase = args?.removePrefix("!")?.toFloat() ?: 0f
                var hue = phase + additionalPhase
                hue -= floor(hue)
                if (isInvert)
                    hue = 1f - hue

                val color = Color.getHSBColor(hue, 1f, 1f)
                return this.style.withColor(color.rgb)
            } else if (tag.lowercase() == "gradient") {
                val colors = mutableListOf<TextColor>()
                val split = args?.split(":") ?: listOf()
                var additionalPhase = 0f

                for ((index, string) in split.withIndex()) {
                    val color = parseColor(string)
                    if (color != null) {
                        colors.add(color)
                    } else if (index == split.lastIndex) {
                        additionalPhase = string.toFloat()
                        if (additionalPhase !in -1.0..1.0)
                            throw IllegalArgumentException("Invalid phase $phase!")
                    }
                }

                if (colors.isEmpty()) {
                    colors.add(TextColor.WHITE)
                    colors.add(TextColor.BLACK)
                } else if (colors.size == 1) {
                    throw IllegalArgumentException("Not enough colours to form a gradient!")
                }

                if (additionalPhase < 0) {
                    additionalPhase += 1f
                    colors.reverse()
                }

                val actualPhase = ((phase + additionalPhase) * colors.size.toFloat()) % colors.size.toFloat()
                val nearestLowerIndex = floor(actualPhase).toInt().coerceAtLeast(0)
                val nearestHigherIndex = ceil(actualPhase).toInt().coerceAtMost(colors.size - 1)

                return this.style.withColor(ARGB.srgbLerp(actualPhase - nearestLowerIndex, colors[nearestLowerIndex].value, colors[nearestHigherIndex].value))
            }

            return this.style
        }
    }

    private class ElementStack {
        private val stack = mutableListOf<TagElement>()

        fun peekElement(): TagElement? {
            return this.stack.lastOrNull()
        }

        fun peek(phase: Float): Style {
            var current = Style.EMPTY
            for (element in this.stack) {
                current = element.prepareStyle(phase).applyTo(current)
            }

            return current
        }

        fun push(element: TagElement) {
            this.stack.add(element)
        }

        fun pop(tag: String, args: String?): Boolean {
            val index = this.stack.indexOfLast { it.tag == tag && it.args == args }
            if (index == -1)
                return false

            this.stack.removeAt(index)
            return true
        }

        fun reset() {
            this.stack.clear()
        }
    }

    fun parse(text: String, lookup: HolderLookup.Provider, isStrict: Boolean = false): Component {
        val elementStack = ElementStack()
        val reader = StringReader(text)
        val current = Component.empty()
        var currentString = ""

        val gradientCursor = mutableListOf<Int>()
        var currentGradient = -1

        fun phase(): Float = if (currentGradient >= 0)
            gradientCursor[currentGradient] / currentString.length.toFloat()
        else 0f

        fun appendCurrent() {
            val style = elementStack.peek(phase())
            current.append(Component.literal(currentString).withStyle(style))
            currentString = ""
        }

        while (reader.canRead()) {
            val char = reader.read()
            if (char == '<') {
                appendCurrent()

                val tag = reader.readStringUntil('>')
                val tagReader = StringReader(tag)

                val start = tagReader.cursor
                val isInvert = tagReader.peek() == '!'
                val isClosingTag = tagReader.peek() == '/'
                val type = tagReader.readStringUntilOrEnd(':')
                val isAutoClosingTag = type.lowercase().trim().endsWith("/")
                val args = tagReader.remaining

                // General closing tag
                if (isClosingTag) {
                    val typeWithoutSlash = type.lowercase().removePrefix("/")
                    if (typeWithoutSlash == "gradient" || typeWithoutSlash == "rainbow") {
                        currentGradient--
                        gradientCursor.removeLast()
                    }

                    if (elementStack.pop(typeWithoutSlash, args.ifBlank { null }))
                        continue

                    tagReader.cursor = start
                } else {
                    when (type.lowercase().trim().removePrefix("/").removeSuffix("/")) {
                        "key" -> {
                            current.append(Component.keybind(tagReader.readOptionallyQuotedString()).withStyle(elementStack.peek(phase())))
                            continue
                        }

                        "lang", "tr", "translate" -> {
                            val key = tagReader.readOptionallyQuotedString()
                            val values = if (tagReader.canRead()) {
                                val values = mutableListOf<Component>()
                                while (tagReader.canRead()) {
                                    values.add(parse(tagReader.readOptionallyQuotedString(), lookup))
                                }

                                values
                            } else listOf()

                            current.append(Component.translatable(key, *values.toTypedArray()).withStyle(elementStack.peek(phase())))
                            continue
                        }

                        "lang_or", "tr_or", "translate_or" -> {
                            val key = tagReader.readOptionallyQuotedString()
                            val fallback = tagReader.readOptionallyQuotedString()
                            val values = if (tagReader.canRead()) {
                                val values = mutableListOf<Component>()
                                while (tagReader.canRead()) {
                                    values.add(parse(tagReader.readOptionallyQuotedString(), lookup))
                                }

                                values
                            } else listOf()

                            current.append(Component.translatableWithFallback(key, fallback, *values.toTypedArray()).withStyle(elementStack.peek(phase())))
                            continue
                        }

                        "reset" -> {
                            if (isStrict)
                                throw IllegalArgumentException("Cannot use reset in strict mode!")

                            elementStack.reset()
                            continue
                        }

                        "newline", "br" -> {
                            current.append(Component.literal("\n"))
                            continue
                        }

                        "selector", "sel" -> {
                            val selectorStr = tagReader.readOptionallyQuotedString()
                            val separator = if (tagReader.canRead()) tagReader.readOptionallyQuotedString() else null
                            val selector = EntitySelector.COMPILABLE_CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf(selectorStr)).orThrow

                            current.append(Component.selector(selector, Optional.ofNullable(separator).map { parse(it, lookup) }).withStyle(elementStack.peek(phase())))

                            continue
                        }

                        "score" -> {
                            val selectorStr = tagReader.readOptionallyQuotedString()
                            val objectiveName = tagReader.readOptionallyQuotedString()

                            if (selectorStr.startsWith('@')) {
                                val selector = EntitySelector.COMPILABLE_CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf(selectorStr)).orThrow
                                current.append(Component.score(selector, objectiveName).withStyle(elementStack.peek(phase())))
                            } else {
                                current.append(Component.score(selectorStr, objectiveName).withStyle(elementStack.peek(phase())))
                            }

                            continue
                        }

                        "nbt", "data" -> {
                            val sourceType = tagReader.readOptionallyQuotedString()
                            val id = tagReader.readOptionallyQuotedString()
                            val path = tagReader.readOptionallyQuotedString()
                            val separator = if (tagReader.canRead()) tagReader.readOptionallyQuotedString() else null
                            val interpret = if (tagReader.canRead()) tagReader.readOptionallyQuotedString().lowercase() == "interpret" else false

                            val source = when (sourceType.lowercase()) {
                                "block" -> BlockDataSource(BlockDataSource.BLOCK_POS_CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf(id)).orThrow)
                                "entity" -> EntityDataSource(EntitySelector.COMPILABLE_CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf(id)).orThrow)
                                "storage" -> StorageDataSource(Identifier.parse(id))
                                else -> throw IllegalArgumentException("Unknown source type ${sourceType}!")
                            }

                            val nbtPath = NbtContents.NBT_PATH_CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf(path)).orThrow
                            current.append(Component.nbt(nbtPath, interpret, false,
                                Optional.ofNullable(separator).map { parse(it, lookup).copy().withStyle(elementStack.peek(phase())) },
                                source
                            ))
                            continue
                        }

                        "pride" -> {
                            // TODO: impl
                            continue
                        }

                        "sprite" -> {
                            val spriteOrAtlas = tagReader.readId()

                            val sprite = if (tagReader.canRead()) tagReader.readId() else spriteOrAtlas
                            val atlas = if (sprite === spriteOrAtlas) Identifier.withDefaultNamespace("blocks") else spriteOrAtlas

                            current.append(Component.`object`(AtlasSprite(atlas, sprite)))
                            continue
                        }

                        "head" -> {
                            val path = tagReader.readOptionallyQuotedString()
                            val outerLayer = tagReader.readBooleanOr(true)

                            val profile = try {
                                ResolvableProfile.createUnresolved(UUID.fromString(path))
                            } catch (_: Throwable) {
                                if (Regex("^[!-~]{0,16}$").matches(path)) {
                                    ResolvableProfile.createUnresolved(path)
                                } else {
                                    ResolvableProfile.create(Either.left(GameProfile(UUID.randomUUID(), "MiniMessageChar")),
                                        PlayerSkin.Patch.create(Optional.of(ClientAsset.ResourceTexture(Identifier.parse(path))), Optional.empty(), Optional.empty(), Optional.empty()))
                                }
                            }

                            current.append(Component.`object`(PlayerSprite(profile, outerLayer)))
                        }

                        else -> {
                            if (isAutoClosingTag)
                                continue

                            try {
                                if (type.lowercase() == "gradient" || type.lowercase() == "rainbow") {
                                    currentGradient++
                                    gradientCursor[currentGradient] = 0
                                }

                                elementStack.push(TagElement(type, args, isInvert, lookup))
                                continue
                            } catch (_: Throwable) {
                                tagReader.cursor = start
                            }
                        }
                    }
                }
            }

            if (currentGradient != -1)
                gradientCursor[currentGradient]++

            currentString += char
        }

        if (isStrict && elementStack.peekElement() != null) {
            throw IllegalStateException("Element stack is not empty!")
        }

        appendCurrent()
        return current
    }

    private fun StringReader.readOptionallyQuotedString(): String = if (this.peek() == '\'' || this.peek() == '"') {
        this.readQuotedString()
    } else {
        this.readStringUntilOrEnd(':')
    }

    private fun StringReader.readId(): Identifier = Identifier.parse(this.readOptionallyQuotedString())

    private fun parseColor(color: String): TextColor? {
        val parsed = TextColor.parseColor(color.lowercase().replace("grey", "gray"))
        if (parsed.isSuccess)
            return parsed.orThrow

        return null
    }
}
