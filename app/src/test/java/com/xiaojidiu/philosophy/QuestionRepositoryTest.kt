package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.data.repository.QuestionRepositoryImpl
import com.xiaojidiu.philosophy.domain.model.Question
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class QuestionRepositoryTest {
    private val items = (1..50).map { Question("q%02d".format(it),"问题","情境","知识",background="解释",sourceType="原创",source="编辑",options=emptyList(),perspectives=emptyList(),philosopherIds=emptyList(),schoolIds=emptyList()) }
    private val repo = QuestionRepositoryImpl(flowOf(items))
    @Test fun sameDayIsStableEvenIfDatabaseOrderChanges() { val day=LocalDate.of(2026,10,7); assertEquals(repo.today(items,day),repo.today(items.reversed(),day)) }
    @Test fun nextDayChanges() { val day=LocalDate.of(2026,10,7); assertNotEquals(repo.today(items,day),repo.today(items,day.plusDays(1))) }
    @Test fun anotherDoesNotReplaceDailyQuestion() { val day=LocalDate.of(2026,10,7); val original=repo.today(items,day); assertNotEquals(original,repo.another(items,original!!.id)); assertEquals(original,repo.today(items,day)) }
    @Test fun emptyCatalogIsSafe() { assertNull(repo.today(emptyList())); assertNull(repo.another(emptyList(),null)) }
}
