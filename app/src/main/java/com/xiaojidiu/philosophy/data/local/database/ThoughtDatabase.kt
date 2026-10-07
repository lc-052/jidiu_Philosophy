package com.xiaojidiu.philosophy.data.local.database

import androidx.room.*
import com.xiaojidiu.philosophy.data.local.entity.*
import com.xiaojidiu.philosophy.data.local.dao.*
import com.xiaojidiu.philosophy.data.local.converter.Converters

@Database(entities = [QuestionEntity::class, QuestionOptionEntity::class, OptionDimensionWeightEntity::class, UserAnswerEntity::class, ReflectionEntity::class, PhilosopherEntity::class, PhilosophySchoolEntity::class, PhilosopherSchoolCrossRef::class, FavoriteEntity::class, ProfileSnapshotEntity::class, RoundTableConversationEntity::class, RoundTableMessageEntity::class, SeedMetadataEntity::class, MeetingThoughtEntity::class, MeetingInsightEntity::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class ThoughtDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun answerDao(): UserAnswerDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun journalDao(): JournalDao
}

object ThoughtMigrations {
    val FROM_1_TO_2 = object : androidx.room.migration.Migration(1,2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `MeetingThoughtEntity` (`conversationId` TEXT NOT NULL, `favoredPhilosopherId` TEXT, `changedThought` TEXT NOT NULL, `conclusion` TEXT NOT NULL, PRIMARY KEY(`conversationId`), FOREIGN KEY(`conversationId`) REFERENCES `RoundTableConversationEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `MeetingInsightEntity` (`id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `messageCount` INTEGER NOT NULL, `summaryJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`conversationId`) REFERENCES `RoundTableConversationEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_MeetingInsightEntity_conversationId` ON `MeetingInsightEntity` (`conversationId`)")
        }
    }
}
