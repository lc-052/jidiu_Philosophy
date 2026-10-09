package com.xiaojidiu.philosophy.data.local.dao

import androidx.room.*
import com.xiaojidiu.philosophy.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao interface ContentDao {
    @Query("SELECT * FROM QuestionEntity ORDER BY id") fun questions(): Flow<List<QuestionEntity>>
    @Query("SELECT * FROM QuestionOptionEntity ORDER BY `order`") fun options(): Flow<List<QuestionOptionEntity>>
    @Query("SELECT * FROM OptionDimensionWeightEntity") fun weights(): Flow<List<OptionDimensionWeightEntity>>
    @Query("SELECT * FROM PhilosopherEntity") fun philosophers(): Flow<List<PhilosopherEntity>>
    @Query("SELECT * FROM PhilosophySchoolEntity") fun schools(): Flow<List<PhilosophySchoolEntity>>
    @Query("SELECT * FROM PhilosopherSchoolCrossRef") fun relations(): Flow<List<PhilosopherSchoolCrossRef>>
    @Query("SELECT version FROM SeedMetadataEntity WHERE id = 1") suspend fun seedVersion(): Int?
    @Upsert suspend fun putQuestions(items: List<QuestionEntity>)
    @Upsert suspend fun putOptions(items: List<QuestionOptionEntity>)
    @Upsert suspend fun putWeights(items: List<OptionDimensionWeightEntity>)
    @Upsert suspend fun putPhilosophers(items: List<PhilosopherEntity>)
    @Upsert suspend fun putSchools(items: List<PhilosophySchoolEntity>)
    @Upsert suspend fun putRelations(items: List<PhilosopherSchoolCrossRef>)
    @Upsert suspend fun putSeed(item: SeedMetadataEntity)
}
@Dao interface UserAnswerDao {
    @Query("SELECT * FROM UserAnswerEntity ORDER BY answeredAt DESC, id DESC") fun observe(): Flow<List<UserAnswerEntity>>
    @Query("SELECT * FROM UserAnswerEntity WHERE id = :id") suspend fun get(id: Long): UserAnswerEntity?
    @Query("SELECT * FROM UserAnswerEntity WHERE requestId = :requestId") suspend fun byRequestId(requestId: String): UserAnswerEntity?
    @Query("SELECT * FROM UserAnswerEntity ORDER BY answeredAt DESC, id DESC") suspend fun all(): List<UserAnswerEntity>
    @Insert suspend fun insert(answer: UserAnswerEntity): Long
    @Query("DELETE FROM UserAnswerEntity") suspend fun clear()
}
@Dao interface ReflectionDao {
    @Query("SELECT * FROM ReflectionEntity") fun observe(): Flow<List<ReflectionEntity>>
    @Query("SELECT * FROM ReflectionEntity WHERE answerId = :answerId") suspend fun get(answerId: Long): ReflectionEntity?
    @Upsert suspend fun put(reflection: ReflectionEntity)
    @Query("DELETE FROM ReflectionEntity WHERE answerId = :answerId") suspend fun remove(answerId: Long)
}
@Dao interface JournalDao {
    @Query("SELECT * FROM FavoriteEntity ORDER BY createdAt DESC") fun favorites(): Flow<List<FavoriteEntity>>
    @Upsert suspend fun favorite(item: FavoriteEntity)
    @Query("DELETE FROM FavoriteEntity WHERE type = :type AND targetId = :id") suspend fun unfavorite(type: String, id: String)
    @Query("DELETE FROM FavoriteEntity") suspend fun clearFavorites()
    @Query("SELECT * FROM ProfileSnapshotEntity ORDER BY createdAt DESC, id DESC") fun snapshots(): Flow<List<ProfileSnapshotEntity>>
    @Insert suspend fun snapshot(item: ProfileSnapshotEntity)
    @Query("DELETE FROM ProfileSnapshotEntity") suspend fun clearSnapshots()
    @Query("SELECT * FROM RoundTableConversationEntity ORDER BY createdAt DESC") fun conversations(): Flow<List<RoundTableConversationEntity>>
    @Query("SELECT * FROM RoundTableConversationEntity WHERE id = :id") suspend fun conversationById(id: String): RoundTableConversationEntity?
    @Query("SELECT * FROM RoundTableMessageEntity ORDER BY `order`") fun messages(): Flow<List<RoundTableMessageEntity>>
    @Upsert suspend fun conversation(item: RoundTableConversationEntity)
    @Insert suspend fun messages(items: List<RoundTableMessageEntity>)
    @Query("DELETE FROM RoundTableConversationEntity") suspend fun clearConversations()
    @Query("SELECT * FROM MeetingThoughtEntity") fun meetingThoughts(): Flow<List<MeetingThoughtEntity>>
    @Query("SELECT * FROM MeetingInsightEntity ORDER BY createdAt") fun meetingInsights(): Flow<List<MeetingInsightEntity>>
    @Upsert suspend fun meetingThought(item: MeetingThoughtEntity)
    @Insert suspend fun meetingInsight(item: MeetingInsightEntity)
}


@Dao interface TipUnderstandingDao {
    @Query("SELECT * FROM TipUnderstandingEntity ORDER BY updatedAt DESC, termId") fun observe(): Flow<List<TipUnderstandingEntity>>
    @Query("SELECT * FROM TipUnderstandingEntity WHERE termId = :termId") fun observeTerm(termId: String): Flow<TipUnderstandingEntity?>
    @Upsert suspend fun put(note: TipUnderstandingEntity)
    @Query("DELETE FROM TipUnderstandingEntity WHERE termId = :termId") suspend fun remove(termId: String)
    @Query("DELETE FROM TipUnderstandingEntity") suspend fun clear()
}
