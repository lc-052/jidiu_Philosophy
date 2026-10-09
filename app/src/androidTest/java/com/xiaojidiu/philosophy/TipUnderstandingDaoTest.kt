package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.data.repository.*
import com.xiaojidiu.philosophy.data.local.entity.TipUnderstandingEntity
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class TipUnderstandingDaoTest: DatabaseTestBase() {
    @Test fun savingAndEditingKeepExactlyOnePersonalUnderstanding()=runBlocking {
        val repository=TipsRepository(ApplicationProvider.getApplicationContext(),db)
        repository.save("free-will","  我愿意为选择负责  ")
        val first=repository.observe("free-will").first()!!
        assertEquals("我愿意为选择负责",first.content);assertTrue(first.updatedAt>0)
        repository.save("free-will","这是修改后的理解")
        assertEquals(1,repository.understandings.first().size)
        assertEquals("这是修改后的理解",repository.observe("free-will").first()!!.content)
        repository.save("metaphysics","这是另一张卡片的理解")
        repository.remove("free-will")
        assertNull(repository.observe("free-will").first());assertEquals(1,repository.understandings.first().size)
    }
    @Test fun invalidWordsBlankAndOverLimitCannotOverwriteSavedText()=runBlocking {
        val repository=TipsRepository(ApplicationProvider.getApplicationContext(),db)
        repository.save("metaphysics","原来的理解")
        for((id,text) in listOf("missing" to "不应写入","metaphysics" to "  ","metaphysics" to "字".repeat(4001))) {
            try {repository.save(id,text);fail("Invalid writing must be rejected")} catch(expected: IllegalArgumentException) { }
        }
        assertEquals("原来的理解",repository.observe("metaphysics").first()!!.content)
        assertEquals(1,repository.understandings.first().size)
    }
    @Test fun clearingAnswersPreservesUnderstandingButClearingAllRemovesIt()=runBlocking {
        db.tipUnderstandingDao().put(TipUnderstandingEntity("free-will","我的私人文字",123))
        val repository=LocalRepository(db)
        repository.clearAnswers();assertEquals(1,db.tipUnderstandingDao().observe().first().size)
        repository.clearUserData();assertTrue(db.tipUnderstandingDao().observe().first().isEmpty())
        assertEquals(1,db.contentDao().questions().first().size)
    }
}
