package com.xiaojidiu.philosophy.domain.model

import kotlinx.serialization.Serializable

@Serializable data class SummaryPoint(val text: String, val messageIds: List<String>)
@Serializable data class DiscussionSummary(val disagreement: List<SummaryPoint>, val commonGround: List<SummaryPoint>)
data class DebateMessage(val id: String,val philosopherId: String,val displayName: String,val content: String,val responseTo: String?,val type: String)
data class MeetingThought(val favoredPhilosopherId: String? = null, val changedThought: String = "", val conclusion: String = "")
data class MeetingInsight(val id: String, val messageCount: Int, val summary: DiscussionSummary)
data class Conversation(val id: String,val topic: String,val philosopherIds: List<String>,val createdAt: Long,val demo: Boolean,val messages: List<DebateMessage>,val thought: MeetingThought? = null,val insights: List<MeetingInsight> = emptyList())
