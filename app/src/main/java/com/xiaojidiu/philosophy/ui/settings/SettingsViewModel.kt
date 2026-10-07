package com.xiaojidiu.philosophy.ui.settings
import androidx.lifecycle.*
import com.xiaojidiu.philosophy.AppGraph
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
class SettingsViewModel(private val graph: AppGraph): ViewModel() {
    val settings=graph.preferences.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
    private val _message=MutableStateFlow("");val message=_message.asStateFlow()
    private val _busy=MutableStateFlow(false);val busy=_busy.asStateFlow()
    private fun act(block: suspend ()->Unit) { if(_busy.value) return;viewModelScope.launch { _busy.value=true;_message.value=""
        try {block()} catch(e: CancellationException) {throw e} catch(e: Exception) {_message.value="设置未能保存，请重试。"} finally {_busy.value=false}
    } }
    fun theme(value: String)=act {graph.preferences.setTheme(value)}
    fun reminder(enabled: Boolean,h: Int,m: Int)=act {graph.preferences.setReminder(enabled,h,m);graph.reminders.configure(graph.preferences.settings.first())}
    fun clear(all: Boolean)=act {if(all) {graph.bgm.clear();graph.repository.clearUserData();graph.preferences.reset();graph.reminders.configure(graph.preferences.settings.first())} else graph.repository.clearAnswers();_message.value="本地记录已清除。"}
    fun notice(value: String) {_message.value=value}
}
