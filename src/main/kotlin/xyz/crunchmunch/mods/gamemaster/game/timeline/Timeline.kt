package xyz.crunchmunch.mods.gamemaster.game.timeline

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.StringRepresentable
import xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints.TimelineKeypoint
import xyz.crunchmunch.mods.gamemaster.game.timeline.prerequisite.TimelinePrerequisite

/**
 * A timeline represents a sequence of events that can occur over a period of time, which also runs independently of the
 * main thread.
 *
 * Very useful for minigame events.
 */
data class Timeline(
    val keypoints: List<TimelineKeypoint<*>>,
    val prerequisites: List<TimelinePrerequisite> = listOf(),
    val type: Type = Type.CONTINUOUS
) {
    companion object {
        val CODEC: Codec<Timeline> = RecordCodecBuilder.create { instance ->
            instance.group(
                TimelineKeypoint.CODEC.listOf()
                    .optionalFieldOf("keypoints", emptyList())
                    .forGetter(Timeline::keypoints),

                TimelinePrerequisite.CODEC.listOf()
                    .optionalFieldOf("prerequisites", emptyList())
                    .forGetter(Timeline::prerequisites),

                Type.CODEC
                    .optionalFieldOf("type", Type.CONTINUOUS)
                    .forGetter(Timeline::type)
            )
                .apply(instance, ::Timeline)
        }
    }

    enum class Type(private val serialized: String) : StringRepresentable {
        /**
         * Represents a fully automated timeline, only capable of being paused and stopped directly by a game master.
         */
        CONTINUOUS("continuous"),

        /**
         * Represents a semi-automatic timeline, requesting for confirmation from a game master before proceeding towards the next keypoint.
         */
        CONFIRMATIONAL("confirmational"),

        /**
         * Represents a manually controlled timeline, requiring manual input from a game master in order to proceed through each keypoint.
         */
        MANUAL("manual");

        override fun getSerializedName(): String {
            return serialized
        }

        companion object {
            val CODEC: Codec<Type> = StringRepresentable.fromEnum(Type::values)
        }
    }
}
