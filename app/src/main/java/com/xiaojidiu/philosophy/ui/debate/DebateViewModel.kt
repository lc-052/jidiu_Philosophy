package com.xiaojidiu.philosophy.ui.debate

import androidx.lifecycle.*
import com.xiaojidiu.philosophy.AppGraph
import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import com.xiaojidiu.philosophy.BuildConfig
import com.xiaojidiu.philosophy.data.remote.api.AiErrorMessages

sealed interface DebateUiState {
    data object Idle: DebateUiState
    data object Loading: DebateUiState
    data class Success(val conversation: Conversation): DebateUiState
    data class Error(val message: String): DebateUiState
}
class DebateViewModel(private val graph: AppGraph,private val saved: SavedStateHandle): ViewModel() {
    val identity=graph.preferences.settings.stateIn(viewModelScope,SharingStarted.Eagerly,com.xiaojidiu.philosophy.data.local.SettingsState())
    val demo=graph.preferences.settings.map {it.aiMode=="demo"}.stateIn(viewModelScope,SharingStarted.Eagerly,BuildConfig.AI_API_BASE_URL.isBlank())
    val topic=saved.getStateFlow("topic","")
    val selected=saved.getStateFlow("selected", listOf("nietzsche","camus","zhuangzi","kant"))
    val input=saved.getStateFlow("input","")
    val demoIndex=saved.getStateFlow("demoIndex",0)
    val replyTo=saved.getStateFlow<String?>("replyTo",null)
    val thoughtFavored=saved.getStateFlow<String?>("thoughtFavored",null)
    val thoughtChanged=saved.getStateFlow("thoughtChanged","")
    val thoughtConclusion=saved.getStateFlow("thoughtConclusion","")
    private val _savingThought=MutableStateFlow(false);val savingThought=_savingThought.asStateFlow()
    private val _summarizing=MutableStateFlow(false);val summarizing=_summarizing.asStateFlow()
    private var lastOperation="discussion"
    private val _state=MutableStateFlow<DebateUiState>(DebateUiState.Idle);val state=_state.asStateFlow()
    private val _conversation=MutableStateFlow<Conversation?>(null);val conversation=_conversation.asStateFlow()
    private val _notice=MutableStateFlow("");val notice=_notice.asStateFlow()
    init { val id=saved.get<String>("conversationId") ?: if(saved.get<Boolean>("newDiscussion")==true) null else saved.get<String>("id"); if(id!=null) viewModelScope.launch { _state.value=DebateUiState.Loading
        try { val found=graph.repository.conversations.first().find {it.id==id};_conversation.value=found; if(found!=null) loadThought(found)
            _state.value=found?.let {DebateUiState.Success(it)} ?: DebateUiState.Error("找不到这场讨论，可能已被清除。")
        } catch(e: CancellationException) {throw e} catch(e: Exception) {_state.value=DebateUiState.Error("本地讨论未能读取，请返回后重试。")}
    } }
    fun editTopic(value: String) {saved["topic"]=value.take(500)}
    fun editInput(value: String) {saved["input"]=value.take(2000)}
    fun toggle(id: String) { val next=selected.value.toMutableList();if(id in next) next.remove(id) else if(next.size<4) next.add(id) else {_notice.value="最多四位，先让一位歇歇。";return};saved["selected"]=next.toList();_notice.value="" }
    fun chooseDemo(index: Int) {saved["demoIndex"]=index}
    fun start(philosophers: List<Philosopher>) { if(_state.value==DebateUiState.Loading || !demo.value && (topic.value.isBlank() || selected.value.isEmpty())) return
        run(philosophers,null)
    }
    fun follow(philosophers: List<Philosopher>) { if(input.value.isBlank() || _state.value==DebateUiState.Loading) return;val old=conversation.value ?: return
        if(old.demo) { _state.value=DebateUiState.Loading; viewModelScope.launch {
            try { val user=DebateMessage(UUID.randomUUID().toString(),"user",graph.preferences.settings.first().nickname,input.value,replyTo.value,"user")
                val updated=graph.repository.saveConversation(old.topic,old.philosopherIds,true,listOf(user),old)
                _conversation.value=updated;_state.value=DebateUiState.Success(updated);saved["input"]="";saved["replyTo"]=null;_notice.value="您的追问已存下。演示模式不会生成新的哲学家回复。"
            } catch(e: CancellationException) {throw e} catch(e: Exception) {_state.value=DebateUiState.Error("追问未能保存，请重试。")}
        };return }
        run(philosophers,old)
    }
    private fun run(philosophers: List<Philosopher>,existing: Conversation?) { lastOperation="discussion"; viewModelScope.launch { _state.value=DebateUiState.Loading;_notice.value=""
        try {
            val user=existing?.let {DebateMessage(UUID.randomUUID().toString(),"user",graph.preferences.settings.first().nickname,input.value,replyTo.value,"user")}
            val (response,isDemo)=graph.ai.request(existing?.topic ?: topic.value,existing?.philosopherIds ?: selected.value,(existing?.messages ?: emptyList())+listOfNotNull(user),philosophers,demoIndex.value,replyTo.value)
            val messages=listOfNotNull(user)+response.messages.map {DebateMessage(it.id,it.philosopherId,it.displayName,it.content,it.responseTo,it.type)}
            val people=if(isDemo) response.messages.map {it.philosopherId}.distinct() else existing?.philosopherIds ?: selected.value
            var result=graph.repository.saveConversation(existing?.topic ?: response.topic,people,isDemo,messages,existing)
            if(response.summary!=null) {
                val mapped=(response.summary.disagreement+response.summary.commonGround).flatMap {it.messageIds}.associateWith {raw->result.messages.firstOrNull {it.id==raw || it.id=="${result.id}:$raw"}?.id ?: raw}
                fun points(list: List<SummaryPoint>)=list.map {it.copy(messageIds=it.messageIds.map {raw->mapped.getValue(raw)})}
                val summary=DiscussionSummary(points(response.summary.disagreement),points(response.summary.commonGround))
                val insight=graph.repository.saveMeetingInsight(result,summary)
                result=result.copy(insights=result.insights+insight)
            }
            _conversation.value=result;if(existing==null) loadThought(result);saved["replyTo"]=null;saved["conversationId"]=result.id;saved["input"]="";_state.value=DebateUiState.Success(result)
            if(isDemo) _notice.value="已打开固定演示：${response.topic}。演示人物和内容不会随输入生成。"
        } catch(e: CancellationException) {throw e} catch(e: Exception) {_state.value=DebateUiState.Error(AiErrorMessages.describe(e))}
    } }
    fun respond(message: DebateMessage, action: String) {
        if(_state.value==DebateUiState.Loading) return
        saved["replyTo"]=message.id
        if(action=="explain") saved["input"]="请用通俗语言解释这条发言中的核心概念。"
        if(action=="example") saved["input"]="请给这条发言举一个生活中的例子。"
    }
    fun cancelReply() {saved["replyTo"]=null}
    private fun loadThought(c: Conversation) {
        if(saved.get<String>("thoughtLoadedFor")!=c.id) {
            saved["thoughtLoadedFor"]=c.id
            saved["thoughtFavored"]=c.thought?.favoredPhilosopherId
            saved["thoughtChanged"]=c.thought?.changedThought.orEmpty()
            saved["thoughtConclusion"]=c.thought?.conclusion.orEmpty()
        }
    }
    fun editFavored(id: String?) {saved["thoughtFavored"]=id}
    fun editChanged(value: String) {saved["thoughtChanged"]=value.take(2000)}
    fun editConclusion(value: String) {saved["thoughtConclusion"]=value.take(2000)}
    fun saveThought() {
        val c=conversation.value ?: return
        if(_savingThought.value) return
        _savingThought.value=true
        viewModelScope.launch {
            try {
                val thought=MeetingThought(thoughtFavored.value,thoughtChanged.value,thoughtConclusion.value)
                graph.repository.saveMeetingThought(c.id,thought)
                _conversation.value=c.copy(thought=thought);_notice.value="会后念头已保存，下次打开会先看到它。"
            } catch(e: CancellationException) {throw e} catch(e: Exception) {_notice.value="会后念头未能保存，请重试。"}
            finally {_savingThought.value=false}
        }
    }
    fun summarize(philosophers: List<Philosopher>) {
        val c=conversation.value ?: return
        if(c.demo) {_notice.value="这是固定演示小结，只依据演示发言；用户追问不会改变它。";return}
        if(_state.value==DebateUiState.Loading) return
        lastOperation="summary"
        _summarizing.value=true
        viewModelScope.launch {
            _state.value=DebateUiState.Loading;_notice.value=""
            try {
                val (response,_)=graph.ai.request(c.topic,c.philosopherIds,c.messages,philosophers,demoIndex.value,summaryOnly=true)
                val insight=graph.repository.saveMeetingInsight(c,requireNotNull(response.summary))
                val updated=c.copy(insights=c.insights+insight)
                _conversation.value=updated;_state.value=DebateUiState.Success(updated)
            } catch(e: CancellationException) {throw e} catch(e: Exception) {_state.value=DebateUiState.Error(AiErrorMessages.describe(e))}
            finally {_summarizing.value=false}
        }
    }
    fun retry(philosophers: List<Philosopher>) {
        if(lastOperation=="summary") summarize(philosophers)
        else if(conversation.value==null) start(philosophers) else follow(philosophers)
    }
    fun newDiscussion() {
        saved["thoughtFavored"]=null;saved["thoughtChanged"]="";saved["thoughtConclusion"]="";saved["replyTo"]=null;saved["thoughtLoadedFor"]=null
        saved["newDiscussion"]=true;saved["input"]="";saved.remove<String>("conversationId");_conversation.value=null;_state.value=DebateUiState.Idle;_notice.value=""}
}

