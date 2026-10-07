package com.xiaojidiu.philosophy.ui.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.components.*
import com.xiaojidiu.philosophy.util.displayDate
import java.time.*
import java.time.temporal.ChronoUnit
import androidx.compose.ui.platform.testTag

@Composable fun ResultScreen(question: Question?, answer: AnswerRecord?, vm: ResultViewModel, back: () -> Unit, school: (String) -> Unit, profile: () -> Unit, journal: Journal, favorite: @Composable ()->Unit, reconsider: (String)->Unit) {
    val draft by vm.draft.collectAsStateWithLifecycle(); val status by vm.status.collectAsStateWithLifecycle(); val saving by vm.saving.collectAsStateWithLifecycle()
    LaunchedEffect(answer?.id) { answer?.let { vm.initialize(it.reflection) } }
    Scaffold(topBar={ XjdTopBar("这个念头",back) }) { padding ->
        LazyColumn(Modifier.padding(padding).imePadding().testTag("result_list"),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            if(question==null || answer==null) item { LoadingState() } else {
                val choice=question.options.find { it.id==answer.optionId }
                item { Text(if(question.category=="死亡") "这是您这次的想法。" else "嚯，您是这么想的。",style=MaterialTheme.typography.headlineMedium) }
                item { XjdCard { Text("这次的选择",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary); Text(choice?.text.orEmpty(),style=MaterialTheme.typography.headlineSmall); HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant); Text(question.scenario,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                item { favorite(); TextButton(onClick={reconsider(question.id)}) {Text("再想两下，重新选择")} }
                val previous=journal.answers.firstOrNull {it.questionId==answer.questionId && (it.answeredAt<answer.answeredAt || it.answeredAt==answer.answeredAt && it.id<answer.id)}
                if(previous!=null) {
                    item { XjdCard {Text("您以前也想过这个。",style=MaterialTheme.typography.titleLarge)
                        val zone=ZoneId.systemDefault();val days=ChronoUnit.DAYS.between(Instant.ofEpochMilli(previous.answeredAt).atZone(zone).toLocalDate(),Instant.ofEpochMilli(answer.answeredAt).atZone(zone).toLocalDate())
                        Text(if(days==0L) "同一天较早的时候：" else "$days 天前：")
                        Text(question.options.find {it.id==previous.optionId}?.text.orEmpty())
                        Text("这次：${choice?.text.orEmpty()}")
                        Text(if(previous.optionId!=answer.optionId) "您的答案变了。" else "这次，您仍然这样想。",style=MaterialTheme.typography.titleMedium)
                        Text(displayDate(previous.answeredAt),color=MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                    item {ReflectionCard(previous.reflection)}
                }
                item { XjdCard { Text("这可能意味着……",style=MaterialTheme.typography.titleLarge); Text("从这次选择来看：",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant); choice?.tendencies?.forEach { Text("• $it",lineHeight=26.sp) }; Text("理由可能比选项更丰富，这不是对您的定义。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                item { SectionHeading(if(question.category=="死亡") "这个问题的背景" else "这个问题有点东西。","从直觉出发，再多了解一点。") }
                item { XjdCard { Text(question.background,style=MaterialTheme.typography.bodyLarge,lineHeight=28.sp); Text("${question.sourceType}\n${question.source}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                item { SectionHeading("不同的人可能会怎么想？","这些是观察角度，您可以有自己的理由。") }
                question.perspectives.forEach { perspective -> item(key=perspective.schoolId) { PerspectiveCard(perspective,school) } }
                item { SectionHeading("留句话给以后的自己？","可以写几句，也可以先留白。") }
                item { OutlinedTextField(draft,vm::edit,Modifier.fillMaxWidth().testTag("reflection_input"),label={Text("我为什么会这么选……")},minLines=3,shape=MaterialTheme.shapes.medium) }
                item { Button(onClick={ vm.save(answer.id) },enabled=!saving,modifier=Modifier.fillMaxWidth()) { Text(if(saving) "正在保存……" else "存下这个念头") }; if(status.isNotEmpty()) Text(status,Modifier.padding(top=12.dp)) }
                item { OutlinedButton(onClick=profile,modifier=Modifier.fillMaxWidth()) { Text("看看我的脑回路") } }
            }
        }
    }
}
