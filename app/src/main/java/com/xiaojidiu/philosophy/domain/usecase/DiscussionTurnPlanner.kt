package com.xiaojidiu.philosophy.domain.usecase

import com.xiaojidiu.philosophy.domain.model.DebateMessage

/** Keeps all participants in context; invites at most three speakers for follow-up. */
object DiscussionTurnPlanner {
    fun speakers(participants: List<String>, messages: List<DebateMessage>, replyTo: String?): List<String> {
        if(messages.none {it.type=="user"}) return participants
        val target=messages.find {it.id==replyTo}?.philosopherId?.takeIf {it in participants}
        val offset=messages.count {it.type=="user"}%participants.size.coerceAtLeast(1)
        val rotated=participants.drop(offset)+participants.take(offset)
        return (listOfNotNull(target)+rotated).distinct().take(3)
    }
}
