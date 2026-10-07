package com.xiaojidiu.philosophy.domain.model

import kotlinx.serialization.Serializable

@Serializable data class GameSource(val title: String, val url: String, val note: String)
@Serializable data class GameTopic(val id: String, val title: String, val relation: String, val context: String, val explanation: List<String>, val caution: String, val question: String, val spoiler: Boolean=false, val philosopherIds: List<String> = emptyList(), val questionIds: List<String> = emptyList(), val sources: List<GameSource>, val entryChoices: List<String> = emptyList())
@Serializable data class GameWork(val id: String, val title: String, val originalTitle: String, val aliases: List<String>, val studio: String, val year: Int, val motif: String, val summary: String, val tags: List<String>, val topics: List<GameTopic>, val sources: List<GameSource>)
@Serializable data class GamesCatalog(val version: Int, val works: List<GameWork>)
