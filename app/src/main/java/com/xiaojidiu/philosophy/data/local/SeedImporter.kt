package com.xiaojidiu.philosophy.data.local

import android.content.Context
import androidx.room.withTransaction
import com.xiaojidiu.philosophy.data.local.database.ThoughtDatabase
import com.xiaojidiu.philosophy.data.local.entity.*
import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SeedImporter(private val context: Context, private val db: ThoughtDatabase, private val preferences: PreferencesRepository) {
    suspend fun import() = withContext(Dispatchers.IO) {
        val version = context.assets.open("seed/version.txt").bufferedReader().use { it.readText().trim().toInt() }
        if ((db.contentDao().seedVersion() ?: 0) >= version) return@withContext
        fun read(path: String) = context.assets.open("seed/$path").bufferedReader().use { it.readText() }
        val questions = Json.decodeFromString<List<Question>>(read("questions.json"))
        val philosophers = Json.decodeFromString<List<Philosopher>>(read("philosophers.json"))
        val schools = Json.decodeFromString<List<School>>(read("schools.json"))
        require(questions.size >= 50 && philosophers.size >= 20 && questions.map { it.id }.distinct().size == questions.size)
        require(questions.all { q -> q.options.size in 2..5 && q.options.map { it.id }.distinct().size == q.options.size && q.options.all { it.weights.values.all { w -> w in -3..3 } } })
        require(questions.all { q -> q.philosopherIds.all { id -> philosophers.any { it.id == id } } && q.schoolIds.all { id -> schools.any { it.id == id } } })
        db.withTransaction {
            val dao = db.contentDao()
            dao.putQuestions(questions.map { QuestionEntity(it.id,it.title,it.scenario,it.category,it.difficulty,it.background,it.sourceType,it.source,it.createdAt,it.perspectives,it.philosopherIds,it.schoolIds) })
            dao.putOptions(questions.flatMap { q -> q.options.map { QuestionOptionEntity(it.id,q.id,it.text,it.order,it.tendencies) } })
            dao.putWeights(questions.flatMap { q -> q.options.flatMap { c -> c.weights.map { OptionDimensionWeightEntity(c.id,it.key,it.value) } } })
            dao.putPhilosophers(philosophers.map { PhilosopherEntity(it.id,it.name,it.years,it.region,it.intro,it.focus,it.ideas,it.works,it.source) })
            dao.putSchools(schools.map { PhilosophySchoolEntity(it.id,it.name,it.intro,it.coreQuestion,it.ideas,it.misconception,it.philosopherIds,it.source) })
            dao.putRelations(philosophers.flatMap { p -> p.schoolIds.map { PhilosopherSchoolCrossRef(p.id,it) } })
            dao.putSeed(SeedMetadataEntity(version = version))
        }
        preferences.setSeedVersion(version)
    }
}
