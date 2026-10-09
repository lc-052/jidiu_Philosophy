package com.xiaojidiu.philosophy

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaojidiu.philosophy.data.local.database.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TipsMigrationTest {
    @get:Rule val helper=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),ThoughtDatabase::class.java)
    @Test fun versionTwoNotesAndConversationSurviveNewDictionaryTable() {
        helper.createDatabase("tips-v2-migration",2).apply {
            execSQL("INSERT INTO RoundTableConversationEntity VALUES ('old','旧话题','[\"zhuangzi\"]',1,1)")
            execSQL("INSERT INTO MeetingThoughtEntity VALUES ('old','zhuangzi','改变了看法','自己的结论')")
            execSQL("INSERT INTO FavoriteEntity VALUES ('conversation','old',1)");close()
        }
        helper.runMigrationsAndValidate("tips-v2-migration",3,true,ThoughtMigrations.FROM_2_TO_3).apply {
            query("SELECT conclusion FROM MeetingThoughtEntity").use {assertTrue(it.moveToFirst());assertEquals("自己的结论",it.getString(0))}
            execSQL("INSERT INTO TipUnderstandingEntity VALUES ('metaphysics','我的理解',123)")
            query("SELECT COUNT(*) FROM FavoriteEntity").use {it.moveToFirst();assertEquals(1,it.getInt(0))};close()
        }
    }
    @Test fun originalVersionOneCanUpgradeDirectlyWithoutLosingMessages() {
        helper.createDatabase("tips-v1-migration",1).apply {
            execSQL("INSERT INTO RoundTableConversationEntity VALUES ('old','原始会议','[\"zhuangzi\"]',1,1)")
            execSQL("INSERT INTO RoundTableMessageEntity VALUES ('m','old','zhuangzi','庄子','原来的发言',NULL,'argument',0)");close()
        }
        helper.runMigrationsAndValidate("tips-v1-migration",3,true,ThoughtMigrations.FROM_1_TO_2,ThoughtMigrations.FROM_2_TO_3).apply {
            query("SELECT content FROM RoundTableMessageEntity").use {assertTrue(it.moveToFirst());assertEquals("原来的发言",it.getString(0))}
            query("SELECT COUNT(*) FROM TipUnderstandingEntity").use {it.moveToFirst();assertEquals(0,it.getInt(0))};close()
        }
    }
}
