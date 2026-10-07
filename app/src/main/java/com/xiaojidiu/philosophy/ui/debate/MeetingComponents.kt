package com.xiaojidiu.philosophy.ui.debate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.components.XjdCard

@Composable fun MeetingSummaryCard(insight: MeetingInsight, conversation: Conversation, source: (String)->Unit) {
    XjdCard(Modifier.testTag("meeting_summary_${insight.id}")) {
        Text("分歧小结",style=MaterialTheme.typography.titleLarge)
        Text(if(conversation.demo) "固定演示小结 · 仅依据演示发言" else "AI 整理 · 截至第 ${insight.messageCount} 条发言",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("目前争论的是……",style=MaterialTheme.typography.titleSmall)
        insight.summary.disagreement.forEach {point->SummaryPointView(point,conversation,source)}
        Text("他们共同承认的是……",style=MaterialTheme.typography.titleSmall)
        if(insight.summary.commonGround.isEmpty()) Text("这一轮还没有足够依据确认共同点。",style=MaterialTheme.typography.bodyMedium)
        insight.summary.commonGround.forEach {point->SummaryPointView(point,conversation,source)}
        Text("这份整理也可能遗漏细节，可以回看原文；它不替您判断谁正确。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun SummaryPointView(point: SummaryPoint,c: Conversation,source: (String)->Unit) {
    Text(point.text,style=MaterialTheme.typography.bodyMedium)
    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        items(point.messageIds.distinct(),key={it}) {id->
            c.messages.find {it.id==id}?.let {message->
                TextButton(onClick={source(id)},modifier=Modifier.testTag("source_$id")) {Text("回看${message.displayName}的发言")}
            }
        }
    }
}
@Composable fun MeetingThoughtCard(thought: MeetingThought,library: Library,edit: ()->Unit) {
    XjdCard(Modifier.testTag("meeting_thought")) {
        Text("那场讨论之后，我留下了……",style=MaterialTheme.typography.titleMedium)
        thought.favoredPhilosopherId?.let {id->Text("当时更认同：${library.philosophers.find {it.id==id}?.name ?: id}",style=MaterialTheme.typography.labelLarge)}
        if(thought.conclusion.isNotBlank()) Text(thought.conclusion)
        if(thought.changedThought.isNotBlank()) {Text("让我改变想法的地方",style=MaterialTheme.typography.labelLarge);Text(thought.changedThought)}
        if(thought.conclusion.isBlank() && thought.changedThought.isBlank() && thought.favoredPhilosopherId==null) Text("当时还没有结论，也可以继续想。")
        TextButton(onClick=edit) {Text("再看看这个念头")}
    }
}
@Composable fun MeetingThoughtEditor(vm: DebateViewModel,c: Conversation,library: Library,close: ()->Unit) {
    val favored by vm.thoughtFavored.collectAsState()
    val changed by vm.thoughtChanged.collectAsState()
    val conclusion by vm.thoughtConclusion.collectAsState()
    val saving by vm.savingThought.collectAsState()
    AlertDialog(onDismissRequest=close,title={Text("留个会后念头")},text={
        Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()).imePadding(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("我现在更认同谁？可以不选，也可以保持分歧。")
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                item {FilterChip(favored==null,{vm.editFavored(null)},label={Text("暂时不选")})}
                items(c.philosopherIds,key={it}) {id->FilterChip(favored==id,{vm.editFavored(id)},modifier=Modifier.testTag("favor_$id"),label={Text(library.philosophers.find {it.id==id}?.name ?: id)},leadingIcon={if(favored==id) Icon(Icons.Outlined.Check,"已选择")})}
            }
            OutlinedTextField(changed,vm::editChanged,Modifier.fillMaxWidth().testTag("meeting_changed"),label={Text("哪句话让我改变了想法？")},maxLines=4)
            OutlinedTextField(conclusion,vm::editConclusion,Modifier.fillMaxWidth().testTag("meeting_conclusion"),label={Text("此刻，我的想法是……")},maxLines=4)
            Text("只存给自己，不作为思想画像的评分依据。",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(onClick={vm.saveThought();close()},enabled=!saving,modifier=Modifier.testTag("save_meeting_thought")) {Text("存下会后念头")}},dismissButton={TextButton(onClick=close) {Text("先不写")}})
}
