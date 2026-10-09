package com.xiaojidiu.philosophy.data.repository

import androidx.room.withTransaction
import com.xiaojidiu.philosophy.data.local.database.ThoughtDatabase
import com.xiaojidiu.philosophy.data.local.entity.*
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import com.xiaojidiu.philosophy.domain.usecase.PhilosophyProfileCalculator
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class QuestionRepositoryImpl(override val questions: Flow<List<Question>>) : QuestionRepository {
    override fun today(items: List<Question>, date: LocalDate): Question? = items.sortedBy { it.id }.let { if (it.isEmpty()) null else it[Math.floorMod(date.toEpochDay(), it.size.toLong()).toInt()] }
    override fun another(items: List<Question>, current: String?) = items.filter { it.id != current }.randomOrNull()
}
class LocalRepository(private val db: ThoughtDatabase) {
    private val content = db.contentDao()
    val questions = combine(content.questions(),content.options(),content.weights()) { qs, os, ws ->
        qs.map { q -> Question(q.id,q.title,q.scenario,q.category,q.difficulty,q.background,q.sourceType,q.source,q.createdAt,
            os.filter { it.questionId == q.id }.map { o -> Choice(o.id,o.text,o.order,ws.filter { it.optionId == o.id }.associate { it.dimension to it.weight },o.tendencies) },q.perspectives,q.philosopherIds,q.schoolIds) }
    }
    val questionRepository: QuestionRepository = QuestionRepositoryImpl(questions)
    val library: Flow<Library> = combine(questions,content.philosophers(),content.schools(),content.relations()) { qs, ps, ss, relations ->
        Library(qs,ps.map { p -> Philosopher(p.id,p.name,p.years,p.region,p.intro,p.focus,p.ideas,p.works,relations.filter { it.philosopherId == p.id }.map { it.schoolId },p.source) },ss.map { School(it.id,it.name,it.intro,it.coreQuestion,it.ideas,it.misconception,it.philosopherIds,it.source) })
    }
    val journal: Flow<Journal> = combine(db.answerDao().observe(),db.reflectionDao().observe(),db.journalDao().favorites(),db.journalDao().snapshots()) { answers, reflections, favorites, snapshots ->
        Journal(answers.map { AnswerRecord(it.id,it.questionId,it.optionId,it.answeredAt,reflections.find { r -> r.answerId == it.id }?.content.orEmpty()) },favorites.map { Favorite(it.type,it.targetId) },snapshots.map { ProfileSnapshot(it.id,it.createdAt,it.answerCount,it.scores) })
    }
    val conversations: Flow<List<Conversation>> = combine(db.journalDao().conversations(),db.journalDao().messages(),db.journalDao().meetingThoughts(),db.journalDao().meetingInsights()) { cs,ms,notes,insights ->
        cs.map {c->Conversation(c.id,c.topic,c.philosopherIds,c.createdAt,c.demo,ms.filter {it.conversationId==c.id}.map {DebateMessage(it.id,it.philosopherId,it.displayName,it.content,it.responseTo,it.type)},notes.find {it.conversationId==c.id}?.let {MeetingThought(it.favoredPhilosopherId,it.changedThought,it.conclusion)},insights.filter {it.conversationId==c.id}.map {MeetingInsight(it.id,it.messageCount,Json.decodeFromString<DiscussionSummary>(it.summaryJson))})}
    }
    suspend fun saveConversation(topic: String, people: List<String>, demo: Boolean, newMessages: List<DebateMessage>, existing: Conversation? = null): Conversation {
        val id=existing?.id ?: UUID.randomUUID().toString();val time=existing?.createdAt ?: System.currentTimeMillis()
        val messages=newMessages.map {it.copy(id="$id:${it.id}",responseTo=it.responseTo?.let {r->if(existing?.messages?.any {it.id==r}==true) r else "$id:$r"})}
        db.withTransaction {
            db.journalDao().conversation(RoundTableConversationEntity(id,topic,people,time,demo))
            db.journalDao().messages(messages.mapIndexed {index,m->RoundTableMessageEntity(m.id,id,m.philosopherId,m.displayName,m.content,m.responseTo,m.type,(existing?.messages?.size ?: 0)+index)})
        }
        return Conversation(id,topic,people,time,demo,(existing?.messages ?: emptyList())+messages,existing?.thought,existing?.insights ?: emptyList())
    }
    suspend fun saveMeetingThought(id: String, thought: MeetingThought) {
        val c=db.journalDao().conversationById(id) ?: error("找不到这场讨论")
        require(thought.favoredPhilosopherId==null || thought.favoredPhilosopherId in c.philosopherIds)
        require(thought.changedThought.length<=2000 && thought.conclusion.length<=2000)
        db.journalDao().meetingThought(MeetingThoughtEntity(id,thought.favoredPhilosopherId,thought.changedThought.trim(),thought.conclusion.trim()))
    }
    suspend fun saveMeetingInsight(conversation: Conversation, summary: DiscussionSummary): MeetingInsight {
        val insight=MeetingInsight(UUID.randomUUID().toString(),conversation.messages.size,summary)
        db.journalDao().meetingInsight(MeetingInsightEntity(insight.id,conversation.id,insight.messageCount,Json.encodeToString(summary),System.currentTimeMillis()))
        return insight
    }
    suspend fun answer(question: Question, optionId: String, requestId: String=UUID.randomUUID().toString()): Long {
        require(question.options.any { it.id == optionId })
        val catalog=questions.first()
        return db.withTransaction {
            db.answerDao().byRequestId(requestId)?.let {return@withTransaction it.id}
            val time=System.currentTimeMillis()
            val id=db.answerDao().insert(UserAnswerEntity(questionId = question.id, optionId = optionId, answeredAt = time, requestId=requestId))
            val profile=PhilosophyProfileCalculator().calculate(catalog,db.answerDao().all().map {AnswerRecord(it.id,it.questionId,it.optionId,it.answeredAt)})
            db.journalDao().snapshot(ProfileSnapshotEntity(createdAt=time,answerCount=profile.answerCount,scores=profile.scores))
            id
        }
    }
    suspend fun reflect(answerId: Long, content: String) {
        require(db.answerDao().get(answerId) != null) { "找不到这次回答" }
        db.withTransaction { val old = db.reflectionDao().get(answerId)
            if (content.isBlank()) db.reflectionDao().remove(answerId) else db.reflectionDao().put(ReflectionEntity(old?.id ?: 0,answerId,content.trim(),System.currentTimeMillis())) }
    }
    suspend fun favorite(type: String, id: String, enabled: Boolean) {
        if (enabled) db.journalDao().favorite(FavoriteEntity(type,id,System.currentTimeMillis())) else db.journalDao().unfavorite(type,id)
    }
    suspend fun clearAnswers() { db.withTransaction { db.answerDao().clear(); db.journalDao().clearSnapshots() } }
    suspend fun clearUserData() { db.withTransaction { db.answerDao().clear(); db.journalDao().clearSnapshots(); db.journalDao().clearFavorites(); db.journalDao().clearConversations(); db.tipUnderstandingDao().clear() } }
}
