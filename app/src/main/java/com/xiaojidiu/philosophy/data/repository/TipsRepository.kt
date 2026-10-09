package com.xiaojidiu.philosophy.data.repository

import android.content.Context
import com.xiaojidiu.philosophy.data.local.database.ThoughtDatabase
import com.xiaojidiu.philosophy.data.local.entity.TipUnderstandingEntity
import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class TipsRepository(private val context: Context, private val db: ThoughtDatabase) {
    private val lock=Mutex()
    private var catalog: TipsCatalog?=null
    val understandings=db.tipUnderstandingDao().observe().map {list->list.map {TipUnderstanding(it.termId,it.content,it.updatedAt)}}
    suspend fun load(): List<TipTerm> = lock.withLock {
        if(catalog==null) catalog=withContext(Dispatchers.IO) {
            context.assets.open("seed/tips.json").bufferedReader().use {Json.decodeFromString<TipsCatalog>(it.readText())}.also {data->
                require(data.version>0 && data.terms.isNotEmpty())
                require(data.terms.map {it.id}.distinct().size==data.terms.size)
            }
        }
        requireNotNull(catalog).terms
    }
    fun observe(termId: String)=db.tipUnderstandingDao().observeTerm(termId).map {it?.let {n->TipUnderstanding(n.termId,n.content,n.updatedAt)}}
    suspend fun save(termId: String, content: String): TipUnderstanding {
        require(load().any {it.id==termId}) {"词语未收录"}
        require(content.isNotBlank() && content.length<=4000) {"请写下 1 至 4000 字的理解"}
        val note=TipUnderstanding(termId,content.trim(),System.currentTimeMillis())
        db.tipUnderstandingDao().put(TipUnderstandingEntity(note.termId,note.content,note.updatedAt))
        return note
    }
    suspend fun remove(termId: String) {db.tipUnderstandingDao().remove(termId)}
}
