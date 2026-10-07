package com.xiaojidiu.philosophy
import com.xiaojidiu.philosophy.data.local.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
class UserAnswerDaoTest : DatabaseTestBase() {
    @Test fun savesAndPreservesRepeatedAnswers() = runBlocking {
        val first=db.answerDao().insert(UserAnswerEntity(questionId="q",optionId="a",answeredAt=1))
        val second=db.answerDao().insert(UserAnswerEntity(questionId="q",optionId="b",answeredAt=2))
        assertNotEquals(first,second); assertEquals("a",db.answerDao().get(first)!!.optionId); assertEquals(listOf("b","a"),db.answerDao().all().map { it.optionId })
    }
    @Test fun foreignKeyRejectsUnknownOption() = runBlocking {
        try { db.answerDao().insert(UserAnswerEntity(questionId="q",optionId="missing",answeredAt=1)); fail("必须拒绝未知选项") } catch (_: android.database.sqlite.SQLiteConstraintException) { }
    }
}
