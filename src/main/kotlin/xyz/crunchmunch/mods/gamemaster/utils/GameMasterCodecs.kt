package xyz.crunchmunch.mods.gamemaster.utils

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.function.Function
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object GameMasterCodecs {
    @JvmField
    val AABB_CODEC: Codec<AABB> = RecordCodecBuilder.create { instance ->
        instance.group(
            Vec3.CODEC
                .fieldOf("start")
                .forGetter(AABB::getMinPosition),
            Vec3.CODEC
                .fieldOf("to")
                .forGetter(AABB::getMaxPosition)
        )
            .apply(instance, ::AABB)
    }

    @JvmField
    val INT_RANGE: Codec<IntRange> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.INT
                .fieldOf("from")
                .forGetter(IntRange::start),
            Codec.INT
                .fieldOf("to")
                .forGetter(IntRange::endInclusive)
        )
            .apply(instance, ::IntRange)
    }

    @JvmField
    val TIME_DURATION: Codec<Duration> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.LONG
                .optionalFieldOf("hours", 0)
                .forGetter { it.inWholeHours },
            Codec.LONG
                .optionalFieldOf("minutes", 0)
                .forGetter { (it.inWholeMinutes - (it.inWholeHours * 60)) % 60 },
            Codec.LONG
                .optionalFieldOf("seconds", 0)
                .forGetter { (it.inWholeSeconds - (it.inWholeMinutes * 60)) % 60 },
            Codec.LONG
                .optionalFieldOf("milliseconds", 0)
                .forGetter { (it.inWholeMilliseconds - (it.inWholeSeconds * 1000)) % 1000 }
        )
            .apply(instance) { hours, minutes, seconds, ms ->
                hours.hours + minutes.minutes + seconds.seconds + ms.milliseconds
            }
    }

    @JvmField
    val TICK_DURATION: Codec<Duration> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.LONG
                .optionalFieldOf("ticks", 0)
                .forGetter { it.ticks.toLong() }
        )
            .apply(instance) { ticks ->
                ticks.ticks
            }
    }

    @JvmField
    val TIME_STRING_DURATION: Codec<Duration> = Codec.STRING
        .comapFlatMap({
            // format: HH:MM:SS.MS
            if (it.isBlank())
                return@comapFlatMap DataResult.error { "Time string cannot be empty!" }

            if (!it.contains(":") && !it.contains("."))
                return@comapFlatMap DataResult.error { "Time string is missing colons and/or periods!" }

            val colons = it.split(":").reversed()
            val periods = colons.first().split(".")

            val milliseconds = if (periods.size > 1) periods[1].toIntOrNull()
                ?: return@comapFlatMap DataResult.error { "Failed to read milliseconds in time string as int!" }
            else 0

            val seconds = periods[0].toIntOrNull()
                ?: return@comapFlatMap DataResult.error { "Failed to read seconds in time string as int!" }

            val minutes = if (colons.size >= 2) colons[1].toIntOrNull()
                ?: return@comapFlatMap DataResult.error { "Failed to read minutes in time string as int!" }
            else 0

            val hours = if (colons.size >= 3) colons[2].toIntOrNull()
                ?: return@comapFlatMap DataResult.error { "Failed to read hours in time string as int!" }
            else 0

            return@comapFlatMap DataResult.success(hours.hours + minutes.minutes + seconds.seconds + milliseconds.milliseconds)
        }, { it.getTimeString() })

    @JvmField
    val DURATION: Codec<Duration> = Codec.withAlternative(
        TICK_DURATION, Codec.withAlternative(
            TIME_DURATION,
            TIME_STRING_DURATION
        )
    )

    fun <T> Codec<T>.mutableListOf(): Codec<MutableList<T>> {
        return this.listOf().xmap({ it.toMutableList() }, Function.identity())
    }
}