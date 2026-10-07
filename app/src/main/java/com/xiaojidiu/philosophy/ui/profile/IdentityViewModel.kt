package com.xiaojidiu.philosophy.ui.profile

import androidx.lifecycle.*
import com.xiaojidiu.philosophy.data.local.PreferencesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class IdentityViewModel(private val preferences: PreferencesRepository, private val saved: SavedStateHandle): ViewModel() {
    val nickname=saved.getStateFlow("nickname","")
    val avatar=saved.getStateFlow("avatar","sprout")
    private val _ready=MutableStateFlow(false);val ready=_ready.asStateFlow()
    private val _status=MutableStateFlow("");val status=_status.asStateFlow()
    private val _busy=MutableStateFlow(false);val busy=_busy.asStateFlow()
    init {viewModelScope.launch {val current=preferences.settings.first();if(saved.get<Boolean>("initialized")!=true) {saved["nickname"]=current.nickname;saved["avatar"]=current.avatar;saved["initialized"]=true};_ready.value=true}}
    fun edit(value: String) {saved["nickname"]=value.take(20);_status.value=""}
    fun select(value: String) {saved["avatar"]=value;_status.value=""}
    fun save() {if(!ready.value || busy.value) return;viewModelScope.launch {
        if(nickname.value.isBlank()) {_status.value="请给自己起个名字。";return@launch}
        _busy.value=true
        try {preferences.setIdentity(nickname.value,avatar.value);_status.value="✓ 昵称和头像已保存。"} catch(e: CancellationException) {throw e} catch(e: Exception) {_status.value="保存失败，请重试。"} finally {_busy.value=false}
    }}
}
