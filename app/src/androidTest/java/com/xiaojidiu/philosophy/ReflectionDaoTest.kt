package com.xiaojidiu.philosophy
import com.xiaojidiu.philosophy.data.local.entity.*
import com.xiaojidiu.philosophy.data.repository.LocalRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
class ReflectionDaoTest : DatabaseTestBase() {
    @Test fun savesEditsAndCascades() = runBlocking {
        val id=db.answerDao().insert(UserAnswerEntity(questionId="q",optionId="a",answeredAt=1))
        val repo=LocalRepository(db); repo.reflect(id,"最初的念头"); repo.reflect(id,"现在的想法")
        assertEquals("现在的想法",db.reflectionDao().get(id)!!.content)
        assertEquals(1,db.reflectionDao().observe().first().size)
        db.answerDao().clear(); assertNull(db.reflectionDao().get(id))
    }
    @Test fun favoritesToggleWithoutDuplicates() = runBlocking {
        val repo=LocalRepository(db); repo.favorite("question","q",true); repo.favorite("question","q",true)
        assertEquals(1,db.journalDao().favorites().first().size)
        repo.favorite("question","q",false); assertTrue(db.journalDao().favorites().first().isEmpty())
    }
}
