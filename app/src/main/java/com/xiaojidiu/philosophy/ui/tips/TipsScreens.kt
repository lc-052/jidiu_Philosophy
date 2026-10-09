package com.xiaojidiu.philosophy.ui.tips

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.components.*
import com.xiaojidiu.philosophy.util.displayDate

@Composable fun TipTermCard(term: TipTerm,note: TipUnderstanding?,open: ()->Unit) {
    Surface(onClick=open,shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surfaceContainerLow,modifier=Modifier.fillMaxWidth().testTag("tip_${term.id}")) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(term.category,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                if(note!=null) Text("✓ 写过理解",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(term.name,style=MaterialTheme.typography.titleLarge)
                    Text(term.english,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,tint=MaterialTheme.colorScheme.primary)
            }
            Text(term.questions.first(),style=MaterialTheme.typography.bodyMedium,lineHeight=24.sp)
            if(note!=null) Text("我的理解：${note.content}",style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun TipsListScreen(vm: TipsListViewModel,back: ()->Unit,open: (String)->Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar={XjdTopBar("TIPS · 哲学词语",back)}) {p->
        LazyColumn(Modifier.padding(p).imePadding().testTag("tips_list"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item {PageHeading("给词语留一点想法","先看看它牵出的内容，再写下自己的理解。")}
            item {OutlinedTextField(state.query,vm::search,Modifier.fillMaxWidth().testTag("tips_search"),placeholder={Text("搜词语、别名或自己的理解……")},singleLine=true,shape=MaterialTheme.shapes.medium,
                leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon={if(state.query.isNotEmpty()) IconButton(onClick={vm.search("")}) {Icon(Icons.Outlined.Close,"清空词语搜索")}})}
            if(!state.loading && state.error==null) {
                item {LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(listOf("全部")+state.categories,key={it}) {category->FilterChip(state.category==category,{vm.category(category)},label={Text(category)})}}}
                item {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    FilterChip(state.writtenOnly,{vm.written(!state.writtenOnly)},label={Text("我写过的")},leadingIcon={if(state.writtenOnly) Icon(Icons.Outlined.Check,null)},modifier=Modifier.testTag("tips_written"))
                    Text("${state.terms.size} / ${state.allCount} 个词语",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }}
            }
            when {
                state.loading -> item {LoadingState("正在翻开这本小词典……")}
                state.error!=null -> item {ErrorState(state.error!!,vm::retry)}
                state.terms.isEmpty() -> item {EmptyState(if(state.writtenOnly && state.query.isBlank()) "还没给这些词留下理解。先打开一个看看。" else "没找到这个词语，换个词或分类试试。")}
                else -> items(state.terms,key={it.id}) {term->TipTermCard(term,state.notes[term.id]) {open(term.id)}}
            }
        }
    }
}

@Composable fun TipDetailScreen(vm: TipDetailViewModel,library: Library,back: ()->Unit,question: (String)->Unit,philosopher: (String)->Unit,school: (String)->Unit,term: (String)->Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val listState=rememberLazyListState()
    val scope=rememberCoroutineScope()
    var remove by rememberSaveable {mutableStateOf(false)}
    var leave by rememberSaveable {mutableStateOf(false)}
    val dirty=state.initialized && draft.trim()!=state.note?.content.orEmpty()
    val requestBack: ()->Unit={if(state.saving) Unit else if(dirty) leave=true else back()}
    BackHandler(enabled=dirty || state.saving,onBack=requestBack)
    Scaffold(topBar={XjdTopBar(state.term?.name ?: "词语卡片",requestBack)}) {p->
        LazyColumn(Modifier.padding(p).imePadding().testTag("tip_detail_list"),state=listState,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            when {
                state.loading -> item {LoadingState()}
                state.error!=null -> item {ErrorState(state.error!!,vm::retry)}
                state.term==null -> item {EmptyState("这个词语暂未收录。")}
                else -> {
                    val word=requireNotNull(state.term)
                    item {Text(word.category,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);PageHeading(word.name,word.english) {
                        TextButton(onClick={scope.launch {listState.animateScrollToItem((listState.layoutInfo.totalItemsCount-1).coerceAtLeast(0))}},modifier=Modifier.testTag("tip_write_jump")) {Icon(Icons.Outlined.Edit,null,Modifier.size(18.dp));Spacer(Modifier.width(4.dp));Text("写下理解")}
                    }}
                    if(word.aliases.isNotEmpty()) item {Text("也可以用这些名字找它：${word.aliases.joinToString("、")}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    item {XjdCard {
                        Text("带着这些问题看",Modifier.semantics {heading()},style=MaterialTheme.typography.titleLarge)
                        word.questions.forEachIndexed {i,prompt->Text("${i+1}. $prompt",lineHeight=28.sp)}
                        Text("这里放的是思考与阅读线索，理解由您自己来写。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }}
                    item {SectionHeading("相关阅读","书目与阅读时可以留意的问题，不是原文引语。")}
                    items(word.readings,key={it.author+it.work}) {reading->XjdCard {
                        Text(reading.work,style=MaterialTheme.typography.titleMedium)
                        Text(reading.author,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                        Text(reading.question,style=MaterialTheme.typography.bodyMedium,lineHeight=26.sp)
                    }}
                    val people=library.philosophers.filter {it.id in word.philosopherIds}
                    if(people.isNotEmpty()) item {SectionHeading("相关哲学家")}
                    items(people,key={"person_"+it.id}) {person->NavigationRow(person.name,"看看他主要在想什么",Icons.Outlined.Person) {philosopher(person.id)}}
                    val schools=library.schools.filter {it.id in word.schoolIds}
                    if(schools.isNotEmpty()) item {SectionHeading("相关思想")}
                    items(schools,key={"school_"+it.id}) {s->NavigationRow(s.name,"沿着这个方向继续探索",Icons.Outlined.AccountTree) {school(s.id)}}
                    val questions=library.questions.filter {it.id in word.questionIds}
                    if(questions.isNotEmpty()) item {SectionHeading("自己选一次","先作选择，再看问题背后的分歧。")}
                    items(questions,key={"question_"+it.id}) {q->NavigationRow(q.title,q.category,Icons.Outlined.Lightbulb) {question(q.id)}}
                    val neighbors=state.catalog.filter {it.id in word.relatedTermIds}
                    if(neighbors.isNotEmpty()) item {SectionHeading("旁边还有这些词");LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(neighbors,key={it.id}) {neighbor->SuggestionChip(onClick={term(neighbor.id)},label={Text(neighbor.name)},modifier=Modifier.testTag("related_tip_${neighbor.id}"))}}}
                    item {XjdCard(Modifier.testTag("tip_understanding_card")) {
                        Text("我对这个词的理解",Modifier.semantics {heading()},style=MaterialTheme.typography.titleLarge)
                        Text("可以是自己的说法、生活中的例子，也可以是暂时没想通的地方。不评分，不纠正。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(state.note!=null) Text("上次保存：${displayDate(state.note!!.updatedAt)}",style=MaterialTheme.typography.bodySmall)
                        OutlinedTextField(draft,vm::edit,Modifier.fillMaxWidth().testTag("tip_understanding"),label={Text("我的理解")},placeholder={Text("这个词让我想到……")},minLines=5,enabled=state.initialized && !state.saving,shape=MaterialTheme.shapes.medium,
                            supportingText={Text("${draft.length} / 4000 字 · 只保存在本机")})
                        Button(onClick={vm.save()},enabled=state.initialized && !state.saving && draft.isNotBlank(),modifier=Modifier.fillMaxWidth().testTag("tip_save")) {Text(if(state.saving) "正在保存……" else "存下我的理解")}
                        if(state.status.isNotBlank()) Text(state.status,Modifier.testTag("tip_status"),style=MaterialTheme.typography.bodyMedium)
                        else if(dirty) Text("还有未保存的想法。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(state.note!=null) TextButton(onClick={remove=true},enabled=!state.saving,modifier=Modifier.testTag("tip_remove")) {Text("清除这条理解")}
                    }}
                }
            }
        }
    }
    if(remove) AlertDialog(onDismissRequest={remove=false},title={Text("清除这条理解？")},text={Text("只清除您对这个词语保存的文字，词语和其他笔记会保留。此操作无法撤销。")},confirmButton={TextButton(onClick={remove=false;vm.remove()}) {Text("确认清除")}},dismissButton={TextButton(onClick={remove=false}) {Text("留着吧")}})
    if(leave) AlertDialog(onDismissRequest={leave=false},title={Text("还有没存下的理解")},text={Text("离开这张卡片前，要把刚才的想法存下来吗？")},confirmButton={Column {
        TextButton(onClick={leave=false;vm.save(back)},enabled=draft.isNotBlank()) {Text("先保存，再返回")}
        TextButton(onClick={leave=false;back()}) {Text("直接返回")}
    }},dismissButton={TextButton(onClick={leave=false}) {Text("继续写")}})
}
