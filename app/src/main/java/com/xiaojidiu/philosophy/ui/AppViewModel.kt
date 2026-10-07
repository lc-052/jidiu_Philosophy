package com.xiaojidiu.philosophy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaojidiu.philosophy.data.local.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.xiaojidiu.philosophy.AppGraph
import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.coroutines.CancellationException

class AppViewModel(val graph: AppGraph) : ViewModel() {
    val preferences = graph.preferences
    private val _games=MutableStateFlow<List<GameWork>>(emptyList()); val games=_games.asStateFlow()
    private val relations = MutableStateFlow<List<ThoughtRelation>>(emptyList())
    val library = combine(graph.repository.library,relations) { library,edges -> library.copy(relations=edges) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Library())
    val journal = graph.repository.journal.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Journal())
    val conversations = graph.repository.conversations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    val settings = preferences.settings.map { it as SettingsState? }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    init { retry() }
    fun retry() { viewModelScope.launch { _error.value = null
        try { graph.seed.import(); relations.value=graph.loadRelations(); _games.value=graph.games.load(); _ready.value = true }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { _error.value = "本地内容未能加载，请重试。${e.message.orEmpty()}" }
    } }
    fun completeOnboarding() { viewModelScope.launch { preferences.completeOnboarding() } }
    fun toggleFavorite(type: String,id: String) { viewModelScope.launch {
        try { graph.repository.favorite(type,id,journal.value.favorites.none {it.type==type && it.targetId==id}) }
        catch (e: CancellationException) {throw e}
        catch (e: Exception) {_error.value="收藏未能保存，请重试。"}
    } }
}

