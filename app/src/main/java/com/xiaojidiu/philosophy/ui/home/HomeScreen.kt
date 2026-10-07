package com.xiaojidiu.philosophy.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.xiaojidiu.philosophy.ui.AppViewModel
import com.xiaojidiu.philosophy.ui.components.*
import com.xiaojidiu.philosophy.domain.model.Library
import kotlinx.coroutines.delay
import java.time.*

@Composable fun HomeScreen(vm: AppViewModel, library: Library, openQuestion: (String, String?) -> Unit) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now=LocalDateTime.now(); delay(60_000) } }
    var alternate by rememberSaveable { mutableStateOf<String?>(null) }
    val today=vm.graph.repository.questionRepository.today(library.questions,now.toLocalDate())
    val question=library.questions.find { it.id==alternate } ?: today
    LazyColumn(Modifier.testTag("home_list"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(when(now.hour) { in 5..11 -> "早上好"; in 12..17 -> "下午好"; else -> "晚上好" },style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                    Text("今天脑子\n要不要动一下？",style=MaterialTheme.typography.headlineLarge)
                }
                XiaojidiuMascot(Modifier.width(88.dp))
            }
        }
        item { SectionHeading(if(alternate==null) "今天丢个大道理" else "刚丢来的另一个","先听听自己的直觉，再看看不同的理由。") }
        if(question==null) item { LoadingState() } else {
            item(key=question.id) { QuestionCard(question) { openQuestion(question.id,it.id) } }
            item {
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text("12,842 人想过这个问题 · 展示数据",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick={ alternate=vm.graph.repository.questionRepository.another(library.questions,question.id)?.id }) { Text("再来一个") }
                        if(alternate!=null) TextButton(onClick={ alternate=null }) { Text("回到今日") }
                    }
                }
            }
        }
        item { Text("这里没有标准答案。一个选择，也可以慢慢想。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
