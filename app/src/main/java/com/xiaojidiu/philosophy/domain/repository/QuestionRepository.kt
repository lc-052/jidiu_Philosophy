package com.xiaojidiu.philosophy.domain.repository

import com.xiaojidiu.philosophy.domain.model.Question
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface QuestionRepository {
    val questions: Flow<List<Question>>
    fun today(items: List<Question>, date: LocalDate = LocalDate.now()): Question?
    fun another(items: List<Question>, current: String?): Question?
}
