package com.xiaojidiu.philosophy.ui.debate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xiaojidiu.philosophy.util.DiscussionParagraphs
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.explore.PhilosopherCard
import com.xiaojidiu.philosophy.ui.components.*
import com.xiaojidiu.philosophy.util.displayDate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.saveable.rememberSaveable
import com.xiaojidiu.philosophy.data.local.SettingsState
import kotlinx.coroutines.flow.first

@Composable fun RoundTableMessageBubble(message: DebateMessage,conversation: Conversation,identity: SettingsState,onAction: ((DebateMessage,String)->Unit)? = null,highlighted: Boolean = false) {
    val user=message.type=="user"
    val target=conversation.messages.find {it.id==message.responseTo}?.displayName
    val paragraphs=remember(message.content,user) {if(user) message.content.split('\n').filter {it.isNotBlank()} else DiscussionParagraphs.split(message.content)}
    Column(Modifier.fillMaxWidth().testTag(if(user) "chat_user_message" else "chat_philosopher_${message.philosopherId}"),horizontalAlignment=if(user) Alignment.End else Alignment.Start,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(highlighted) Text("小结引用的这条发言",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(user) UserAvatar(identity.avatar,size=28.dp) else PhilosopherAvatar(message.philosopherId,size=28.dp,name=message.displayName)
            Text(message.displayName,style=MaterialTheme.typography.titleSmall.copy(fontSize=14.sp,fontWeight=FontWeight.SemiBold),color=MaterialTheme.colorScheme.onSurface)
            if(target!=null) Text("回应 $target",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
        }
        Surface(modifier=(if(user) Modifier.widthIn(max=320.dp) else Modifier.fillMaxWidth()).then(if(!user && onAction!=null) Modifier.clickable(onClickLabel="回应${message.displayName}的这条发言") {onAction(message,"reply")} else Modifier),shape=RoundedCornerShape(topStart=if(user) 20.dp else 6.dp,topEnd=if(user) 6.dp else 20.dp,bottomStart=20.dp,bottomEnd=20.dp),color=if(user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,tonalElevation=0.dp,border=if(user) null else BorderStroke(if(highlighted) 2.dp else 1.dp,if(highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f))) {
            Column(Modifier.padding(horizontal=16.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                paragraphs.forEach {paragraph->Text(paragraph,style=MaterialTheme.typography.bodyLarge.copy(fontSize=15.sp,lineHeight=25.sp,letterSpacing=0.sp),color=if(user) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)}
            }
        }
    }
}
@Composable fun DebateScreen(vm: DebateViewModel,library: Library,journal: Journal,favorite: (String)->Unit,history: ()->Unit,back: (() -> Unit)?=null,configure: ()->Unit={},identity: ()->Unit={}) {
    val topic by vm.topic.collectAsStateWithLifecycle();val selected by vm.selected.collectAsStateWithLifecycle();val state by vm.state.collectAsStateWithLifecycle();val discussion by vm.conversation.collectAsStateWithLifecycle();val input by vm.input.collectAsStateWithLifecycle();val notice by vm.notice.collectAsStateWithLifecycle();val demoIndex by vm.demoIndex.collectAsStateWithLifecycle()
    val user by vm.identity.collectAsStateWithLifecycle();val demo by vm.demo.collectAsStateWithLifecycle()
    val listState=rememberLazyListState()
    val scope=rememberCoroutineScope()
    val summarizing by vm.summarizing.collectAsStateWithLifecycle()
    var highlightedSource by rememberSaveable(discussion?.id) {mutableStateOf<String?>(null)}
    val replyId by vm.replyTo.collectAsStateWithLifecycle()
    val replyMessage=discussion?.messages?.find {it.id==replyId}
    var showThought by rememberSaveable {mutableStateOf(false)}
    var menu by remember {mutableStateOf(false)}
    var membersOverride by rememberSaveable {mutableStateOf<Boolean?>(null)}
    val compact by remember {derivedStateOf {listState.firstVisibleItemIndex>1 || listState.firstVisibleItemIndex==1 && listState.firstVisibleItemScrollOffset>48}}
    LaunchedEffect(compact) {membersOverride=null}
    fun messageIndex(c: Conversation,id: String): Int {
        val index=c.messages.indexOfFirst {it.id==id}
        return if(index<0) 0 else index+1+c.insights.count {it.messageCount<=index}
    }
    fun showSource(c: Conversation,id: String) {highlightedSource=id;scope.launch {listState.animateScrollToItem(messageIndex(c,id))}}
    if(showThought && discussion!=null) MeetingThoughtEditor(vm,discussion!!,library) {showThought=false}
    var showExplanation by rememberSaveable {mutableStateOf(false)}
    var lastConversation by rememberSaveable {mutableStateOf<String?>(null)}
    var lastMessageCount by rememberSaveable {mutableIntStateOf(0)}
    LaunchedEffect(discussion?.id,discussion?.messages?.size) {
        val c=discussion ?: return@LaunchedEffect
        snapshotFlow {listState.layoutInfo.totalItemsCount}.first {it>=c.messages.size+1}
        if(lastConversation!=c.id) {listState.scrollToItem(if(c.messages.isEmpty() || c.thought!=null) 0 else 1)}
        else if(c.messages.size>lastMessageCount) {listState.animateScrollToItem((lastMessageCount+1+c.insights.count {it.messageCount<=lastMessageCount}).coerceAtMost(listState.layoutInfo.totalItemsCount-1))}
        lastConversation=c.id;lastMessageCount=c.messages.size
    }
    Scaffold(topBar={
        if(discussion!=null) {
            Column {
                val c=discussion!!
                Row(Modifier.fillMaxWidth().heightIn(min=64.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    if(back!=null) IconButton(onClick=back) {Icon(Icons.AutoMirrored.Outlined.ArrowBack,"返回")}
                    Column(Modifier.weight(1f).padding(start=8.dp)) {
                        Text(if(compact) c.topic else if(back!=null) "圆桌讨论" else "圆桌会议",style=MaterialTheme.typography.titleLarge,maxLines=1,overflow=TextOverflow.Ellipsis)
                        if(compact) Text("圆桌会议 · ${if(c.demo) "演示讨论" else "AI 思想推演"}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick={membersOverride=!(membersOverride ?: !compact)},modifier=Modifier.testTag("toggle_members")) {Icon(Icons.Outlined.Group,if(membersOverride ?: !compact) "查看或收起入座成员" else "查看入座成员")}
                    Box {
                        IconButton(onClick={menu=true},modifier=Modifier.testTag("meeting_menu")) {Icon(Icons.Outlined.MoreVert,"会议菜单")}
                        DropdownMenu(menu,{menu=false}) {
                            DropdownMenuItem(text={Text("留下会后念头")},onClick={menu=false;showThought=true},enabled=state!=DebateUiState.Loading)
                            DropdownMenuItem(text={Text("再开一桌")},onClick={menu=false;membersOverride=null;vm.newDiscussion()},enabled=state!=DebateUiState.Loading)
                        }
                    }
                }
                if(!compact) Column(Modifier.padding(horizontal=20.dp,vertical=4.dp)) {Text(c.topic,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis);Text("${c.philosopherIds.size+1} 位入座 · 第 ${c.messages.count {it.type=="user"}+1} 轮",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                if(membersOverride ?: !compact) LazyRow(Modifier.testTag("meeting_members"),contentPadding=PaddingValues(horizontal=20.dp,vertical=2.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    item {TextButton(onClick=identity,contentPadding=PaddingValues(horizontal=0.dp,vertical=4.dp)) {UserAvatar(user.avatar,size=24.dp);Spacer(Modifier.width(6.dp));Text(user.nickname,Modifier.widthIn(max=76.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelMedium)}}
                    items(c.philosopherIds,key={it}) {id->val name=library.philosophers.find {it.id==id}?.name ?: id;Row(Modifier.heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {PhilosopherAvatar(id,size=24.dp,name=name);Text(name,style=MaterialTheme.typography.labelMedium)}}
                }
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            }
        } else if(back!=null) XjdTopBar("圆桌讨论",back)
    },bottomBar={
        discussion?.let {c->
            Surface(color=MaterialTheme.colorScheme.surface,shadowElevation=2.dp) {
                Column(Modifier.fillMaxWidth().imePadding().padding(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    replyMessage?.let {target->
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("正在回应 ${target.displayName}：${target.content}",Modifier.weight(1f).testTag("reply_target"),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelMedium)
                            IconButton(onClick=vm::cancelReply) {Icon(Icons.Outlined.Close,"取消定向回应")}
                        }
                        LazyRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            item {TextButton(onClick={vm.respond(target,"reply")}) {Text("回应这一条")}}
                            item {TextButton(onClick={vm.respond(target,"explain")},modifier=Modifier.testTag("reply_explain")) {Text("解释这个概念")}}
                            item {TextButton(onClick={vm.respond(target,"example")},modifier=Modifier.testTag("reply_example")) {Text("举个生活中的例子")}}
                        }
                    }
                    Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(input,vm::editInput,Modifier.weight(1f).testTag("followup_input"),placeholder={Text("说说您的想法……",fontSize=14.sp)},textStyle=MaterialTheme.typography.bodyMedium.copy(fontSize=15.sp,lineHeight=22.sp),maxLines=4,enabled=state!=DebateUiState.Loading,shape=RoundedCornerShape(18.dp))
                        FilledIconButton(onClick={vm.follow(library.philosophers)},enabled=input.isNotBlank() && state!=DebateUiState.Loading,modifier=Modifier.size(48.dp).testTag("chat_send")) {Icon(Icons.AutoMirrored.Outlined.Send,if(c.demo) "存下我的追问" else "发送发言")}
                    }
                    if(c.demo) Text("演示仅保存您的发言",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }) {p->
        LazyColumn(Modifier.padding(p).consumeWindowInsets(p).then(if(discussion==null) Modifier.imePadding() else Modifier).testTag("debate_list"),state=listState,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            if(discussion==null) {
                item {PageHeading("圆桌会议","带着一个问题，听听不同的思想。")}
                item {XjdCard {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {UserAvatar(user.avatar);Column(Modifier.weight(1f)) {Text(user.nickname,style=MaterialTheme.typography.titleMedium);Text("以这个形象加入讨论",style=MaterialTheme.typography.bodyMedium)};TextButton(onClick=identity) {Text("编辑形象")}}}}
                item {Row {TextButton(onClick=history) {Icon(Icons.Outlined.History,null,Modifier.size(18.dp));Text("看看以前的讨论")};TextButton(onClick=configure) {Text("连接模型服务")}}}
                if(demo) item {
                    Column {Text("Demo Mode · 当前为演示讨论",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);Text("固定演示，不生成新回答；追问只保存在本地。",style=MaterialTheme.typography.bodyMedium);TextButton(onClick={showExplanation=!showExplanation}) {Text("演示怎么体验？")};if(showExplanation) Text("开始会议时播放所选演示的固定话题与出场人物。连接模型服务后可自由讨论。")}
                }
                item {SectionHeading("01 / 丢个问题","好问题不必很大，一件小事也值得想。")}
                item {OutlinedTextField(topic,vm::editTopic,Modifier.fillMaxWidth().testTag("topic_input"),label={Text("人为什么一定要努力？")},minLines=2,shape=MaterialTheme.shapes.medium,enabled=state!=DebateUiState.Loading)}
                if(demo) item {LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {itemsIndexed(listOf("人为什么一定要努力？","对陌生人，我们欠多少帮助？","喜欢的东西，一定要有用吗？")) {i,text->FilterChip(demoIndex==i,{vm.chooseDemo(i)},label={Text(text)})}}}
                item {SectionHeading("02 / 邀请几位 · ${selected.size}/4","左右滑动选人，最多四位。")}
                item {LazyRow(Modifier.testTag("philosopher_row"),horizontalArrangement=Arrangement.spacedBy(12.dp)) {items(library.philosophers,key={it.id}) {person->Box(Modifier.width(168.dp)) {PhilosopherCard(person,person.id in selected) {if(state!=DebateUiState.Loading) vm.toggle(person.id)}}}}}
                item {Button(onClick={vm.start(library.philosophers)},enabled=state!=DebateUiState.Loading && (demo || topic.isNotBlank() && selected.isNotEmpty()),modifier=Modifier.fillMaxWidth().heightIn(min=52.dp).testTag("start_meeting"),shape=MaterialTheme.shapes.medium) {Text("开始会议")}}
            } else {
                val c=discussion!!
                item {
                    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    c.thought?.let {MeetingThoughtCard(it,library) {showThought=true}}
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    if(c.demo) Text("Demo Mode · 当前为演示讨论",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                    Text("以下内容是基于相关哲学思想进行的现代推演，并非哲学家本人曾讨论过这一问题。",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("已自动保存到本地 · ${displayDate(c.createdAt)}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row {com.xiaojidiu.philosophy.ui.favorite.FavoriteButton("conversation",c.id,journal) {favorite(c.id)};TextButton(onClick=history) {Text("看看以前的讨论")}}
                    if(c.demo && !demo) TextButton(onClick=vm::newDiscussion) {Text("用我的模型开启新会议")}
                }
                }}
                c.messages.forEachIndexed {index,message->
                    item(key=message.id) {
                        if(message.type=="user") Text("第 ${c.messages.take(index+1).count {it.type=="user"}+1} 轮 · 接着想",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        RoundTableMessageBubble(message,c,user,vm::respond,highlightedSource==message.id)
                    }
                    c.insights.filter {it.messageCount==index+1}.forEach {insight->item(key="insight_${insight.id}") {MeetingSummaryCard(insight,c) {id->showSource(c,id)}}}
                }
                item(key="meeting_actions") {
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        if(!c.demo && c.messages.count {it.type=="user"}>=1) {
                            val upToDate=c.insights.lastOrNull()?.messageCount==c.messages.size
                            OutlinedButton(onClick={vm.summarize(library.philosophers)},enabled=state!=DebateUiState.Loading && !upToDate,modifier=Modifier.testTag("summarize_meeting")) {Text(if(upToDate) "这一轮已整理" else "整理分歧（调用一次模型）")}
                        }
                        TextButton(onClick={showThought=true},enabled=state!=DebateUiState.Loading,modifier=Modifier.testTag("finish_meeting")) {Text("收个尾，留下会后念头")}
                    }
                }
            }
            if(state==DebateUiState.Loading) item {LoadingState(if(summarizing) "正在整理大家的分歧……" else "正在邀请几位思想家入座……")}
            if(state is DebateUiState.Error) item {XjdCard {Text((state as DebateUiState.Error).message);Button(onClick={vm.retry(library.philosophers)}) {Text("重新连接")}}}
            if(notice.isNotEmpty()) item {Text(notice,lineHeight=26.sp)}
        }
    }
}

@Composable fun ConversationHistoryScreen(conversations: List<Conversation>,back: ()->Unit,open: (String)->Unit) {
    Scaffold(topBar={XjdTopBar("以前开过的桌",back)}) {p->LazyColumn(Modifier.padding(p),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        if(conversations.isEmpty()) item {EmptyState("还没人坐下，先丢个问题进去。")}
        items(conversations,key={it.id}) {c->XjdCard {Text(c.topic,style=MaterialTheme.typography.titleLarge);Text(displayDate(c.createdAt));Text(if(c.demo) "演示讨论 · ${c.messages.size} 条消息" else "思想推演 · ${c.messages.size} 条消息");c.thought?.let {note->if(note.conclusion.isNotBlank()) Text("当时的想法：${note.conclusion}",maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium)};TextButton(onClick={open(c.id)}) {Text("继续这场讨论")}}}
    }}
}




