package com.xiaojidiu.philosophy.data.remote.dto
import kotlinx.serialization.Serializable
import com.xiaojidiu.philosophy.domain.model.DiscussionSummary
@Serializable data class BackendHealth(val status: String, val configured: Boolean, val model: String = "")
@Serializable data class RoundTableMessage(val id: String, val philosopherId: String, val displayName: String, val content: String, val responseTo: String? = null, val type: String)
@Serializable data class RoundTableResponse(val topic: String, val messages: List<RoundTableMessage>, val summary: DiscussionSummary? = null)
@Serializable data class RoundTableRequest(val topic: String, val philosopherIds: List<String>, val messages: List<RoundTableMessage> = emptyList(), val policy: String = "modern_inference_no_fabricated_quotes", val mode: String = "discussion", val speakerIds: List<String> = philosopherIds, val replyTo: String? = null, val includeSummary: Boolean = false)
