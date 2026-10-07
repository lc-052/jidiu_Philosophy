package com.xiaojidiu.philosophy
import com.xiaojidiu.philosophy.data.repository.LocalRepository
import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
class RepositoryIntegrationTest: DatabaseTestBase() {
    @Test fun answerAndSnapshotAreAtomicAndRequestCanBeReplayed() = runBlocking {
        val repo=LocalRepository(db);val q=repo.questions.first().first()
        val first=repo.answer(q,"a","request-one");val replay=repo.answer(q,"a","request-one");assertEquals(first,replay)
        repo.reflect(first,"我在意的是责任");repo.answer(q,"b","request-two")
        val journal=repo.journal.first();assertEquals(2,journal.answers.size);assertEquals(2,journal.snapshots.size);assertEquals("我在意的是责任",journal.answers.last().reflection)
        assertEquals(1,journal.snapshots.first().answerCount)
    }
    @Test fun discussionContextAndFavoritesSurviveReload() = runBlocking {
        val repo=LocalRepository(db)
        val c=repo.saveConversation("努力",listOf("zhuangzi"),true,listOf(DebateMessage("a","zhuangzi","庄子","先问什么是有用。",null,"argument")))
        repo.saveConversation(c.topic,c.philosopherIds,true,listOf(DebateMessage("b","user","您","我还在想。",c.messages.first().id,"user")),c)
        repo.favorite("conversation",c.id,true)
        assertEquals(2,LocalRepository(db).conversations.first().first().messages.size);assertEquals(1,repo.journal.first().favorites.size)
        repo.clearAnswers();assertEquals(1,repo.conversations.first().size)
        repo.clearUserData();assertTrue(repo.conversations.first().isEmpty());assertTrue(repo.journal.first().favorites.isEmpty())
    }
}
