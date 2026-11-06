package xyz.crunchmunch.mods.gamemaster.game.timeline.keypoints

import xyz.crunchmunch.mods.gamemaster.game.timeline.TimelineContext

abstract class MainThreadKeypoint<T : MainThreadKeypoint<T>>(type: Type<T>) : TimelineKeypoint<T>(type) {
    final override suspend fun execute(context: TimelineContext) {
        context.server.execute {
            executeOnMainThread(context)
        }
    }

    protected abstract fun executeOnMainThread(context: TimelineContext)
}
