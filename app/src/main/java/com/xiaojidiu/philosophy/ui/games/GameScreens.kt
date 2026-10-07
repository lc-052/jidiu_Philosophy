package com.xiaojidiu.philosophy.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.components.*

/** Abstract editorial artwork, not the game's cover or character assets. */
@Composable private fun WorkArt(motif: String, modifier: Modifier=Modifier) {
    Canvas(modifier.fillMaxWidth().height(108.dp).clip(RoundedCornerShape(22.dp)).semantics {contentDescription=if(motif=="sky") "原创天空与窗口插画" else "原创樱花与画框插画"}) {
        val w=size.width;val h=size.height;val ink=Color(0xFF494154)
        drawRect(if(motif=="sky") Color(0xFFD5EFFD) else Color(0xFFFFDCD4))
        if(motif=="sky") {
            drawCircle(Color.White,h*.2f,Offset(w*.2f,h*.42f));drawCircle(Color.White,h*.28f,Offset(w*.3f,h*.37f));drawCircle(Color.White,h*.2f,Offset(w*.4f,h*.42f))
            drawRoundRect(ink,Offset(w*.65f,h*.2f),Size(w*.2f,h*.62f),CornerRadius(10f),style=Stroke(4f))
            drawLine(ink,Offset(w*.75f,h*.2f),Offset(w*.75f,h*.82f),4f);drawLine(ink,Offset(w*.65f,h*.51f),Offset(w*.85f,h*.51f),4f)
        } else {
            drawRoundRect(Color.White,Offset(w*.13f,h*.17f),Size(w*.3f,h*.65f),CornerRadius(10f))
            drawLine(ink,Offset(w*.28f,h*.6f),Offset(w*.28f,h*.85f),4f)
            repeat(5) {i->val a=i*Math.PI*2/5;drawCircle(Color(0xFFB65A84),h*.075f,Offset(w*.28f+kotlin.math.cos(a).toFloat()*h*.09f,h*.46f+kotlin.math.sin(a).toFloat()*h*.09f))}
            repeat(7) {i->drawOval(Color(0xFFB65A84),Offset(w*(.57f+i%3*.12f),h*(.18f+i/3*.22f)),Size(h*.09f,h*.16f))}
        }
    }
}

@Composable fun GameWorksScreen(works: List<GameWork>, back: ()->Unit, open: (String)->Unit) {
    var query by rememberSaveable {mutableStateOf("")}
    val matches=remember(works,query) {works.filter {w->query.isBlank() || (listOf(w.title,w.originalTitle,w.summary)+w.aliases+w.tags+w.topics.map {it.title}).any {it.contains(query.trim(),true)}}}
    Scaffold(topBar={XjdTopBar("游戏中的哲学",back)}) {p->LazyColumn(Modifier.padding(p).imePadding().testTag("games_list"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {PageHeading("通关之后，继续想","有些问题，会从故事里走出来。")}
        item {OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().testTag("games_search"),placeholder={Text("搜作品、哲学家或主题……")},leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon={if(query.isNotEmpty()) IconButton(onClick={query=""}) {Icon(Icons.Outlined.Close,"清空作品搜索")}},singleLine=true,shape=MaterialTheme.shapes.medium)}
        item {Text("作品各有一页。先看主题，涉及后段的解读由您主动展开。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(matches.isEmpty()) item {EmptyState("没找到这部作品，换个名字或主题试试。")}
        items(matches,key={it.id}) {work->
            Card(onClick={open(work.id)},modifier=Modifier.fillMaxWidth().testTag("game_${work.id}"),shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    WorkArt(work.motif)
                    Row(verticalAlignment=Alignment.CenterVertically) {Text(work.title,Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall);Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,"查看${work.title}的哲学内容",tint=MaterialTheme.colorScheme.primary)}
                    Text("${work.studio} · ${work.year} · ${work.topics.size} 个思想入口",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                    Text(work.summary,style=MaterialTheme.typography.bodyMedium,lineHeight=25.sp)
                    Text(work.tags.joinToString(" / "),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }}
}

@Composable private fun SourceCard(sources: List<GameSource>) {
    val uri=LocalUriHandler.current
    var error by remember {mutableStateOf(false)}
    XjdCard {
        Text("依据与继续阅读",style=MaterialTheme.typography.titleMedium)
        sources.forEach {source->
            Column {TextButton(onClick={error=runCatching {uri.openUri(source.url)}.isFailure},contentPadding=PaddingValues(0.dp)) {Icon(Icons.AutoMirrored.Outlined.OpenInNew,null,Modifier.size(16.dp));Spacer(Modifier.width(8.dp));Text(source.title)};Text(source.note,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
        if(error) Text("无法打开外部网页，请确认设备已安装浏览器。",color=MaterialTheme.colorScheme.error)
    }
}

@Composable private fun GameTopicCard(topic: GameTopic, library: Library, philosopher: (String)->Unit, question: (String)->Unit) {
    var expanded by rememberSaveable(topic.id) {mutableStateOf(false)}
    var initialChoice by rememberSaveable(topic.id) {mutableStateOf<Int?>(null)}
    val visible=initialChoice!=null && (!topic.spoiler || expanded)
    XjdCard(Modifier.testTag("topic_${topic.id}")) {
        Text(topic.title,style=MaterialTheme.typography.titleLarge)
        Text("先想一下，不涉及剧情",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
        Text(topic.question,style=MaterialTheme.typography.titleMedium)
        topic.entryChoices.forEachIndexed {index,choice->
            FilterChip(initialChoice==index,{initialChoice=index},modifier=Modifier.fillMaxWidth().testTag("entry_${topic.id}_$index"),label={Text(choice)},leadingIcon={if(initialChoice==index) Icon(Icons.Outlined.Check,"已选择")})
        }
        Text("凭直觉选就好，没有评分，也可以改。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(initialChoice==null) Text("先留下一个想法，再看看作品如何呈现这个问题。",style=MaterialTheme.typography.bodyMedium)
        Text(topic.relation,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
        if(topic.spoiler) {
            Text("涉及后段主题解读；未通关也可以先跳过。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick={expanded=!expanded},enabled=initialChoice!=null,modifier=Modifier.testTag("spoiler_${topic.id}")) {Icon(if(expanded) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text(if(expanded) "收起这段解读" else "展开主题解读（可能剧透）")}
        }
        if(visible) {
            Text("在作品里怎么看",style=MaterialTheme.typography.titleSmall)
            Text(topic.context,Modifier.testTag("topic_context_${topic.id}"),lineHeight=27.sp)
            Text("背后的哲学",style=MaterialTheme.typography.titleSmall)
            topic.explanation.forEach {Text(it,lineHeight=27.sp)}
            Text("不要急着等同",style=MaterialTheme.typography.titleSmall)
            Text(topic.caution,style=MaterialTheme.typography.bodyMedium,lineHeight=25.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.secondaryContainer) {Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("回看刚才的直觉",style=MaterialTheme.typography.labelLarge)
                Text(topic.entryChoices.getOrNull(initialChoice ?: -1).orEmpty(),lineHeight=26.sp)
                Text("读到这里，这个想法有没有松动？",style=MaterialTheme.typography.bodyMedium)
                TextButton(onClick={initialChoice=null;expanded=false}) {Text("回到问题，再想一下")}
            }}
            topic.philosopherIds.forEach {id->library.philosophers.find {it.id==id}?.let {person->TextButton(onClick={philosopher(id)}) {Text("认识一下${person.name}")}}}
            topic.questionIds.forEach {id->library.questions.find {it.id==id}?.let {q->TextButton(onClick={question(id)}) {Text("自己选一次：${q.title}")}}}
            SourceCard(topic.sources)
        }
    }
}

@Composable fun GameWorkDetailScreen(work: GameWork?, library: Library, back: ()->Unit, philosopher: (String)->Unit, question: (String)->Unit) {
    Scaffold(topBar={XjdTopBar(work?.title ?: "作品内容",back)}) {p->LazyColumn(Modifier.padding(p).testTag("game_detail_list"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        if(work==null) item {EmptyState("这部作品的内容尚未收录。")} else {
            item {WorkArt(work.motif)}
            item {PageHeading(work.title,work.originalTitle)}
            item {Text("${work.studio} · 原版 ${work.year}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);Text(work.summary,lineHeight=27.sp)}
            item {XjdCard {Text("先说明阅读边界",style=MaterialTheme.typography.titleMedium);Text("这是独立的哲学阅读笔记，不是官方剧情解析。条目区分官方主题、作品关联解读与延伸对照；不将角色发言当作作者结论，也不伪造台词。",style=MaterialTheme.typography.bodyMedium,lineHeight=25.sp);Text("本页不包含成人画面或详细伤害描写。外部作品网页可能包含面向成年人的原作内容。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            item {SectionHeading("从作品走进思想","先选一个让您在意的问题。")}
            items(work.topics,key={it.id}) {GameTopicCard(it,library,philosopher,question)}
            item {SourceCard(work.sources)}
        }
    }}
}

