package com.xiaojidiu.philosophy.ui.tips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaojidiu.philosophy.data.repository.TipsRepository
import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private data class CatalogState(val terms: List<TipTerm> = emptyList(),val loading: Boolean = true,val error: String? = null)
data class TipsListState(val terms: List<TipTerm> = emptyList(),val allCount: Int = 0,val categories: List<String> = emptyList(),
    val notes: Map<String,TipUnderstanding> = emptyMap(),val query: String = "",val category: String = "全部",val writtenOnly: Boolean = false,
    val loading: Boolean = true,val error: String? = null)
class TipsListViewModel(private val repository: TipsRepository,private val saved: SavedStateHandle): ViewModel() {
    private val catalog=MutableStateFlow(CatalogState())
    val state=combine(catalog,saved.getStateFlow("query",""),saved.getStateFlow("category","全部"),saved.getStateFlow("written",false),repository.understandings) {data,query,category,written,notes->
        val byId=notes.associateBy {it.termId}
        TipsListState(data.terms.filter {(category=="全部" || it.category==category) && (!written || it.id in byId) && it.matches(query,byId[it.id]?.content.orEmpty())},
            data.terms.size,data.terms.map {it.category}.distinct(),byId,query,category,written,data.loading,data.error)
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),TipsListState())
    init {retry()}
    fun search(text: String) {saved["query"]=text.take(200)}
    fun category(value: String) {saved["category"]=value}
    fun written(value: Boolean) {saved["written"]=value}
    fun retry() {viewModelScope.launch {
        catalog.value=catalog.value.copy(loading=true,error=null)
        try {catalog.value=CatalogState(repository.load(),false)}
        catch(e: CancellationException) {throw e}
        catch(e: Exception) {catalog.value=CatalogState(loading=false,error="哲学词语未能加载，请重试。")}
    }}
}
data class TipDetailState(val term: TipTerm? = null,val catalog: List<TipTerm> = emptyList(),val loading: Boolean = true,
    val note: TipUnderstanding? = null,val initialized: Boolean = false,val saving: Boolean = false,val status: String = "",val error: String? = null)
class TipDetailViewModel(private val repository: TipsRepository,private val saved: SavedStateHandle): ViewModel() {
    private val id=saved.get<String>("id").orEmpty()
    private val _state=MutableStateFlow(TipDetailState());val state=_state.asStateFlow()
    val draft=saved.getStateFlow("draft","")
    private var loading: Job?=null
    init {retry()}
    fun retry() {
        loading?.cancel();loading=viewModelScope.launch {
            _state.update {it.copy(loading=true,error=null)}
            try {
                val terms=repository.load();val term=terms.find {it.id==id}
                _state.update {it.copy(term=term,catalog=terms,loading=false)}
                if(term!=null) repository.observe(id).collect {note->
                    if(!saved.contains("initialized")) {saved["draft"]=note?.content.orEmpty();saved["initialized"]=true}
                    _state.update {it.copy(note=note,initialized=true)}
                }
            } catch(e: CancellationException) {throw e}
            catch(e: Exception) {_state.update {it.copy(loading=false,error="这个词语的内容未能加载，请重试。")}}
        }
    }
    fun edit(text: String) {if(text.length<=4000) {saved["draft"]=text;_state.update {it.copy(status="")}}}
    fun save(afterSaved: ()->Unit = {}) {
        if(_state.value.saving || !_state.value.initialized || draft.value.isBlank()) return
        val text=draft.value
        viewModelScope.launch {
            _state.update {it.copy(saving=true,status="")}
            try {val note=repository.save(id,text);_state.update {it.copy(note=note,status="理解存好了。以后可以回来改。")};afterSaved()}
            catch(e: CancellationException) {throw e}
            catch(e: Exception) {_state.update {it.copy(status="未能保存，请再试一次。")}}
            finally {_state.update {it.copy(saving=false)}}
        }
    }
    fun remove() {
        if(_state.value.saving) return
        viewModelScope.launch {
            _state.update {it.copy(saving=true,status="")}
            try {repository.remove(id);saved["draft"]="";_state.update {it.copy(note=null,status="这条理解已清除，可以重新写。")}}
            catch(e: CancellationException) {throw e}
            catch(e: Exception) {_state.update {it.copy(status="未能清除，请再试一次。")}}
            finally {_state.update {it.copy(saving=false)}}
        }
    }
}
