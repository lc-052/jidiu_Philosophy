package com.xiaojidiu.philosophy

import android.content.Context
import androidx.room.Room
import com.xiaojidiu.philosophy.data.local.*
import com.xiaojidiu.philosophy.data.local.database.ThoughtDatabase
import com.xiaojidiu.philosophy.data.repository.LocalRepository
import com.xiaojidiu.philosophy.data.repository.AiRepository
import com.xiaojidiu.philosophy.domain.model.ThoughtRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.xiaojidiu.philosophy.util.ReminderScheduler

class AppGraph(context: Context) {
    val apiKeys = ApiKeyStore(context.applicationContext)
    val preferences = PreferencesRepository(context,apiKeys)
    val bgm=com.xiaojidiu.philosophy.data.repository.BgmRepository(context.applicationContext,preferences)
    val reminders = ReminderScheduler(context.applicationContext)
    val database = Room.databaseBuilder(context,ThoughtDatabase::class.java,"little-thoughts.db").addMigrations(com.xiaojidiu.philosophy.data.local.database.ThoughtMigrations.FROM_1_TO_2,com.xiaojidiu.philosophy.data.local.database.ThoughtMigrations.FROM_2_TO_3).build()
    val tips = com.xiaojidiu.philosophy.data.repository.TipsRepository(context.applicationContext,database)
    val games = com.xiaojidiu.philosophy.data.repository.GameRepository(context.applicationContext)
    val repository = LocalRepository(database)
    val ai = AiRepository(context.applicationContext,preferences,apiKeys)
    val seed = SeedImporter(context,database,preferences)
    private val appContext = context.applicationContext
    suspend fun loadRelations(): List<ThoughtRelation> = withContext(Dispatchers.IO) { appContext.assets.open("seed/relations.json").bufferedReader().use { Json.decodeFromString(it.readText()) } }
}

