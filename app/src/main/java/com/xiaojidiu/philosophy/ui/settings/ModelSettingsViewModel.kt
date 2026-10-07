package com.xiaojidiu.philosophy.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaojidiu.philosophy.AppGraph
import com.xiaojidiu.philosophy.BuildConfig
import com.xiaojidiu.philosophy.data.remote.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ModelSettingsViewModel(private val graph: AppGraph, private val saved: SavedStateHandle): ViewModel() {
    val settings=graph.preferences.settings.stateIn(viewModelScope,SharingStarted.Eagerly,null)
    val provider=saved.getStateFlow("provider","qwen")
    val baseUrl=saved.getStateFlow("baseUrl","https://dashscope.aliyuncs.com/compatible-mode/v1/")
    val model=saved.getStateFlow("model","qwen-plus")
    val backendUrl=saved.getStateFlow("backendUrl","")
    private val _key=MutableStateFlow("");val key=_key.asStateFlow() // Never saved to a Bundle or DataStore.
    private val _hasKey=MutableStateFlow(false);val hasKey=_hasKey.asStateFlow()
    private val _ready=MutableStateFlow(false);val ready=_ready.asStateFlow()
    private val _busy=MutableStateFlow(false);val busy=_busy.asStateFlow()
    private val _status=MutableStateFlow("");val status=_status.asStateFlow()
    private val _errorField=MutableStateFlow<String?>(null);val errorField=_errorField.asStateFlow()
    init {viewModelScope.launch {
        val s=graph.preferences.settings.first()
        if(saved.get<Boolean>("initialized")!=true) {
            saved["provider"]=if(s.aiMode=="backend") "backend" else s.modelProvider
            saved["baseUrl"]=s.modelBaseUrl;saved["model"]=s.modelName;saved["backendUrl"]=s.aiBaseUrl;saved["initialized"]=true
        }
        _hasKey.value=try {graph.apiKeys.readFor(s.modelBaseUrl)!=null} catch(e: CancellationException) {throw e} catch(e: Exception) {false}
        _ready.value=true
    }}
    fun editUrl(value: String) {saved["baseUrl"]=value.take(2048);clearStatus()}
    fun editModel(value: String) {saved["model"]=value.take(200);clearStatus()}
    fun editBackend(value: String) {saved["backendUrl"]=value.take(2048);clearStatus()}
    fun editKey(value: String) {_key.value=value.take(4096);clearStatus()}
    private fun clearStatus() {_status.value="";_errorField.value=null}
    fun choose(value: String) {
        if(value==provider.value) return
        saved["provider"]=value;_key.value="";clearStatus()
        if(value=="qwen") {saved["baseUrl"]="https://dashscope.aliyuncs.com/compatible-mode/v1/";saved["model"]="qwen-plus"}
        else if(value=="compatible") {saved["baseUrl"]="";saved["model"]=""}
    }
    fun region(url: String) {saved["baseUrl"]=url;_key.value="";clearStatus()}
    private fun validation(field: String, message: String): Nothing {_errorField.value=field;throw ModelConfigurationException(message)}
    private suspend fun directValues(): Triple<String,String,String> {
        val url=try {ModelUrl.normalize(baseUrl.value,BuildConfig.DEBUG)} catch(e: IllegalArgumentException) {validation("url",e.message ?: "接口地址无效。")}
        val name=model.value.trim()
        if(name.isBlank()) validation("model","请填写模型名称，例如 qwen-plus。")
        val typed=_key.value.trim()
        val key=if(typed.isNotBlank()) typed else try {graph.apiKeys.readFor(url)} catch(e: CancellationException) {throw e} catch(e: Exception) {validation("key","无法读取已保存的 API Key，请重新输入。")}
        if(key.isNullOrBlank()) validation("key",if(_hasKey.value) "接口地址已改变，请填写这个地址对应的 API Key。" else "请填写您的 API Key。")
        if(key.any {it.isISOControl()}) validation("key","API Key 不能含有换行或控制字符，请重新粘贴。")
        return Triple(url,name,key)
    }
    private fun action(block: suspend ()->Unit) {
        if(_busy.value || !_ready.value) return
        viewModelScope.launch {
            _busy.value=true;clearStatus()
            try {block()} catch(e: CancellationException) {throw e} catch(e: Exception) {_status.value=AiErrorMessages.describe(e)} finally {_busy.value=false}
        }
    }
    fun save()=action {
        if(provider.value=="backend") {
            val url=try {BackendUrl.normalize(backendUrl.value,BuildConfig.DEBUG)} catch(e: IllegalArgumentException) {validation("backend",e.message ?: "服务地址无效。")}
            if(url.isBlank()) validation("backend","请填写自有圆桌服务的地址。")
            graph.preferences.setAiBaseUrl(url);saved["backendUrl"]=url
        } else {
            val (url,name,key)=directValues()
            graph.apiKeys.save(url,key)
            graph.preferences.setDirectModel(url,name,provider.value)
            saved["baseUrl"]=url;saved["model"]=name;_key.value="";_hasKey.value=true
        }
        _status.value="✓ 配置已保存。返回圆桌会议，点击开始会议即可使用。"
    }
    fun test()=action {
        _status.value="正在测试连接……"
        if(provider.value=="backend") {
            val health=graph.ai.checkConnection(backendUrl.value)
            _status.value=if(health.configured) "✓ 自有服务可访问，已配置模型 ${health.model}。" else "自有服务可访问，但服务端尚未配置模型。"
        } else {
            val (url,name,key)=directValues()
            graph.ai.testDirectConnection(url,name,key,provider.value)
            _status.value="✓ 模型连接成功。若修改过配置，请点击保存配置再开始会议。"
        }
    }
    fun demo()=action {graph.preferences.setDemoMode();_status.value="✓ 已切换为演示模式，已保存的密钥会保留。"}
    fun removeKey()=action {graph.apiKeys.clear();graph.preferences.setDemoMode();_key.value="";_hasKey.value=false;_status.value="API Key 已移除，并切换为演示模式。"}
}
