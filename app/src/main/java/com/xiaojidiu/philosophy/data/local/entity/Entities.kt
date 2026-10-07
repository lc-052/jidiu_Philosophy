package com.xiaojidiu.philosophy.data.local.entity

import androidx.room.*
import com.xiaojidiu.philosophy.domain.model.*

@Entity data class QuestionEntity(@PrimaryKey val id: String, val title: String, val scenario: String, val category: String, val difficulty: Int, val background: String, val sourceType: String, val source: String, val createdAt: Long, val perspectives: List<Perspective>, val philosopherIds: List<String>, val schoolIds: List<String>)
@Entity(foreignKeys = [ForeignKey(entity = QuestionEntity::class, parentColumns = ["id"], childColumns = ["questionId"], onDelete = ForeignKey.CASCADE)], indices = [Index("questionId")])
data class QuestionOptionEntity(@PrimaryKey val id: String, val questionId: String, val text: String, val order: Int, val tendencies: List<String>)
@Entity(primaryKeys = ["optionId", "dimension"], foreignKeys = [ForeignKey(entity = QuestionOptionEntity::class, parentColumns = ["id"], childColumns = ["optionId"], onDelete = ForeignKey.CASCADE)])
data class OptionDimensionWeightEntity(val optionId: String, val dimension: PhilosophyDimension, val weight: Int)
@Entity(foreignKeys = [ForeignKey(entity = QuestionEntity::class, parentColumns = ["id"], childColumns = ["questionId"], onDelete = ForeignKey.CASCADE), ForeignKey(entity = QuestionOptionEntity::class, parentColumns = ["id"], childColumns = ["optionId"], onDelete = ForeignKey.CASCADE)], indices = [Index("questionId"), Index("optionId"), Index("answeredAt"), Index(value=["requestId"],unique=true)])
data class UserAnswerEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val questionId: String, val optionId: String, val answeredAt: Long, val requestId: String=java.util.UUID.randomUUID().toString())
@Entity(foreignKeys = [ForeignKey(entity = UserAnswerEntity::class, parentColumns = ["id"], childColumns = ["answerId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["answerId"], unique = true)])
data class ReflectionEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val answerId: Long, val content: String, val updatedAt: Long)
@Entity data class PhilosopherEntity(@PrimaryKey val id: String, val name: String, val years: String, val region: String, val intro: String, val focus: String, val ideas: String, val works: List<String>, val source: String)
@Entity data class PhilosophySchoolEntity(@PrimaryKey val id: String, val name: String, val intro: String, val coreQuestion: String, val ideas: String, val misconception: String, val philosopherIds: List<String>, val source: String)
@Entity(primaryKeys = ["philosopherId", "schoolId"], foreignKeys = [ForeignKey(entity = PhilosopherEntity::class, parentColumns = ["id"], childColumns = ["philosopherId"], onDelete = ForeignKey.CASCADE), ForeignKey(entity = PhilosophySchoolEntity::class, parentColumns = ["id"], childColumns = ["schoolId"], onDelete = ForeignKey.CASCADE)], indices = [Index("schoolId")])
data class PhilosopherSchoolCrossRef(val philosopherId: String, val schoolId: String)
@Entity(primaryKeys = ["type", "targetId"]) data class FavoriteEntity(val type: String, val targetId: String, val createdAt: Long)
@Entity data class ProfileSnapshotEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val createdAt: Long, val answerCount: Int, val scores: Map<PhilosophyDimension, Int>)
@Entity data class RoundTableConversationEntity(@PrimaryKey val id: String, val topic: String, val philosopherIds: List<String>, val createdAt: Long, val demo: Boolean)
@Entity(foreignKeys = [ForeignKey(entity = RoundTableConversationEntity::class, parentColumns = ["id"], childColumns = ["conversationId"], onDelete = ForeignKey.CASCADE)], indices = [Index("conversationId")])
data class RoundTableMessageEntity(@PrimaryKey val id: String, val conversationId: String, val philosopherId: String, val displayName: String, val content: String, val responseTo: String?, val type: String, val order: Int)
@Entity data class SeedMetadataEntity(@PrimaryKey val id: Int = 1, val version: Int)

@Entity(foreignKeys = [ForeignKey(entity = RoundTableConversationEntity::class, parentColumns = ["id"], childColumns = ["conversationId"], onDelete = ForeignKey.CASCADE)])
data class MeetingThoughtEntity(@PrimaryKey val conversationId: String, val favoredPhilosopherId: String?, val changedThought: String, val conclusion: String)
@Entity(foreignKeys = [ForeignKey(entity = RoundTableConversationEntity::class, parentColumns = ["id"], childColumns = ["conversationId"], onDelete = ForeignKey.CASCADE)], indices = [Index("conversationId")])
data class MeetingInsightEntity(@PrimaryKey val id: String, val conversationId: String, val messageCount: Int, val summaryJson: String, val createdAt: Long)
