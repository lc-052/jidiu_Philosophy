package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.data.repository.LocalRepository
import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*

class MeetingStorageTest: DatabaseTestBase() {
    @Test fun thoughtSummaryAndSourcesSurviveReloadAndClearWithConversation()=runBlocking {
        val repo=LocalRepository(db)
        val c=repo.saveConversation("努力",listOf("zhuangzi"),false,listOf(DebateMessage("a","zhuangzi","庄子","先检视尺度。",null,"argument")))
        val summary=DiscussionSummary(listOf(SummaryPoint("怎样理解有用？",listOf(c.messages.first().id))),emptyList())
        repo.saveMeetingInsight(c,summary)
        repo.saveMeetingThought(c.id,MeetingThought("zhuangzi","看见了另一把尺子。","我想先认真过好今天。"))
        val loaded=LocalRepository(db).conversations.first().single()
        assertEquals("我想先认真过好今天。",loaded.thought!!.conclusion)
        assertEquals(c.messages.first().id,loaded.insights.single().summary.disagreement.single().messageIds.single())
        val followed=repo.saveConversation(c.topic,c.philosopherIds,false,listOf(DebateMessage("b","user","我","举个例子？",c.messages.first().id,"user")),loaded)
        assertNotNull(followed.thought);assertEquals(1,followed.insights.size)
        assertEquals(c.messages.first().id,repo.conversations.first().single().messages.last().responseTo)
        repo.clearUserData();assertTrue(db.journalDao().meetingThoughts().first().isEmpty());assertTrue(db.journalDao().meetingInsights().first().isEmpty())
    }
}
