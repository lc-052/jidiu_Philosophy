package com.xiaojidiu.philosophy.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.components.*
import com.xiaojidiu.philosophy.util.displayDate

@Composable fun HistoryCard(question: Question, answer: AnswerRecord, open: ()->Unit) {
    XjdCard { Text(displayDate(answer.answeredAt),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant); Text(question.title,style=MaterialTheme.typography.titleLarge); Text("当时选择 · ${question.options.find { it.id==answer.optionId }?.text.orEmpty()}",style=MaterialTheme.typography.titleMedium); Text(answer.reflection.ifBlank { "当时的您什么也没留下。" },style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2); TextButton(onClick=open) { Text("打开这个念头") } }
}
@Composable fun HistoryScreen(library: Library, journal: Journal, back: ()->Unit, open: (Long)->Unit) {
    Scaffold(topBar={XjdTopBar("以前都想了些什么",back)}) { padding ->
        LazyColumn(Modifier.padding(padding),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            if(journal.answers.isEmpty()) item { EmptyState("脑子还挺干净，先去想两道。") }
            items(journal.answers,key={it.id}) { answer -> library.questions.find { it.id==answer.questionId }?.let { HistoryCard(it,answer) {open(answer.id)} } }
        }
    }
}
