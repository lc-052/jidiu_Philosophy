package com.xiaojidiu.philosophy.ui.result
import androidx.lifecycle.*
import com.xiaojidiu.philosophy.data.repository.LocalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class ResultViewModel(private val repository: LocalRepository, private val saved: SavedStateHandle) : ViewModel() {
    val draft = saved.getStateFlow("draft", "")
    private val _status = MutableStateFlow("")
    val status = _status.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    fun initialize(text: String) { if (!saved.contains("initialized")) { saved["draft"] = text; saved["initialized"] = true } }
    fun edit(text: String) { saved["draft"] = text; _status.value = "" }
    fun save(id: Long) { if (_saving.value) return
        viewModelScope.launch { _saving.value = true
            try { repository.reflect(id,draft.value); _status.value = "念头存好了。以后再来看看。" }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _status.value = "未能保存，请再试一次。" }
            finally { _saving.value = false }
        }
    }
}
