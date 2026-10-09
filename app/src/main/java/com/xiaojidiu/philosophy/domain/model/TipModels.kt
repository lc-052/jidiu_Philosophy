package com.xiaojidiu.philosophy.domain.model

import kotlinx.serialization.Serializable

@Serializable data class TipsCatalog(val version: Int, val terms: List<TipTerm>)
@Serializable data class TipReading(val author: String, val work: String, val question: String)
@Serializable data class TipTerm(val id: String, val name: String, val english: String, val aliases: List<String>,
    val category: String, val questions: List<String>, val readings: List<TipReading>,
    val questionIds: List<String>, val philosopherIds: List<String>, val schoolIds: List<String>, val relatedTermIds: List<String>) {
    fun matches(query: String, understanding: String = ""): Boolean = query.trim().let {needle->
        needle.isEmpty() || (listOf(name,english,category,understanding)+aliases).any {it.contains(needle,true)}
    }
}
data class TipUnderstanding(val termId: String, val content: String, val updatedAt: Long)
