package com.xiaojidiu.philosophy.data.repository

import android.content.Context
import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Read-only editorial content is loaded off the UI thread; no database migration needed. */
class GameRepository(private val context: Context) {
    suspend fun load(): List<GameWork> = withContext(Dispatchers.IO) {
        val catalog=context.assets.open("seed/games.json").bufferedReader().use {Json.decodeFromString<GamesCatalog>(it.readText().removePrefix("\uFEFF"))}
        require(catalog.version>0 && catalog.works.map {it.id}.distinct().size==catalog.works.size)
        catalog.works.forEach {work->require(work.topics.isNotEmpty() && work.topics.map {it.id}.distinct().size==work.topics.size);work.topics.forEach {require(it.sources.isNotEmpty() && it.explanation.isNotEmpty())}}
        catalog.works
    }
}
