package com.xiaojidiu.philosophy

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaojidiu.philosophy.data.local.database.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class MeetingMigrationTest {
    @get:Rule val helper=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),ThoughtDatabase::class.java)
    @Test fun versionOneConversationAndMessagesArePreserved() {
        helper.createDatabase("meeting-migration-test",1).apply {
            execSQL("INSERT INTO RoundTableConversationEntity VALUES ('old','旧话题','[\"zhuangzi\"]',1,1)")
            execSQL("INSERT INTO RoundTableMessageEntity VALUES ('m','old','zhuangzi','庄子','原来的完整发言',NULL,'argument',0)")
            close()
        }
        helper.runMigrationsAndValidate("meeting-migration-test",2,true,ThoughtMigrations.FROM_1_TO_2).apply {
            query("SELECT content FROM RoundTableMessageEntity WHERE id='m'").use {assertTrue(it.moveToFirst());assertEquals("原来的完整发言",it.getString(0))}
            execSQL("INSERT INTO MeetingThoughtEntity VALUES ('old','zhuangzi','改变了看法','我的结论')")
            query("SELECT COUNT(*) FROM RoundTableConversationEntity").use {it.moveToFirst();assertEquals(1,it.getInt(0))}
            close()
        }
    }
}
