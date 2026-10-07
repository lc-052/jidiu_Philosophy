package com.xiaojidiu.philosophy.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable data class ChatMessage(val role: String, val content: String? = null)
@Serializable data class JsonOutputFormat(val type: String)
@Serializable data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean,
    @SerialName("max_tokens") val maxTokens: Int,
    @SerialName("response_format") val responseFormat: JsonOutputFormat? = null,
    @SerialName("enable_thinking") val enableThinking: Boolean? = null
)
@Serializable data class ChatChoice(val message: ChatMessage, @SerialName("finish_reason") val finishReason: String? = null)
@Serializable data class ChatCompletionResponse(val choices: List<ChatChoice> = emptyList())
