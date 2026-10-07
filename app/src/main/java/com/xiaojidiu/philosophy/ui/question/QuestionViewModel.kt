package com.xiaojidiu.philosophy.ui.question

import androidx.lifecycle.*
import com.xiaojidiu.philosophy.data.repository.LocalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException

class QuestionViewModel(private val repository: LocalRepository, private val saved: SavedStateHandle) : ViewModel() {
    val selected = saved.getStateFlow<String?>("selected", null)
    val result = saved.getStateFlow<Long?>("answerId", null)
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private var submitting = false
    private val requestId=saved.get<String>("requestId") ?: java.util.UUID.randomUUID().toString().also {saved["requestId"]=it}
    fun choose(questionId: String, optionId: String) {
        if (submitting || result.value != null) return
        submitting = true; saved["selected"] = optionId; _error.value = null
        viewModelScope.launch {
            try {
                delay(300)
                val question = repository.questions.first { qs -> qs.any { it.id == questionId } }.first { it.id == questionId }
                saved["answerId"] = repository.answer(question, optionId,requestId)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = "回答未能保存，请重试。"; saved["selected"] = null }
            finally { submitting = false }
        }
    }
}
