package com.xiaojidiu.philosophy.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.xiaojidiu.philosophy.domain.model.*

@Composable fun ChoiceButton(text: String, selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick, modifier = Modifier.fillMaxWidth().heightIn(min=56.dp).animateContentSize().semantics { this.selected = selected }, enabled = enabled, border = BorderStroke(1.dp, if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant), contentPadding = PaddingValues(horizontal=16.dp, vertical=14.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onSurface)) { Text((if (selected) "✓  " else "") + text, modifier = Modifier.fillMaxWidth(), fontSize=16.sp, lineHeight=24.sp) }
}
@Composable fun QuestionCard(question: Question, selected: String? = null, enabled: Boolean = true, choose: (Choice) -> Unit) {
    XjdCard { Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(10.dp)) { Text("${question.category} · ${if(question.difficulty==1) "轻轻想" else "多想两下"}", Modifier.padding(horizontal=10.dp,vertical=5.dp), style=MaterialTheme.typography.labelMedium, color=MaterialTheme.colorScheme.primary) }; Text(question.scenario,Modifier.padding(vertical=8.dp),style=MaterialTheme.typography.headlineSmall)
        question.options.forEach { choice -> ChoiceButton(choice.text, selected == choice.id, enabled) { choose(choice) } }
    }
}
@Composable fun PerspectiveCard(perspective: Perspective, explore: (String) -> Unit) {
    var expanded by rememberSaveable(perspective.schoolId) { mutableStateOf(false) }
    XjdCard(Modifier.animateContentSize()) {
        Text(perspective.title,style=MaterialTheme.typography.titleMedium)
        Text(perspective.content, maxLines=if(expanded) Int.MAX_VALUE else 3, style=MaterialTheme.typography.bodyLarge, lineHeight=27.sp)
        Row { TextButton(onClick={ expanded=!expanded }) { Text(if(expanded) "收起来" else "展开读读") }; TextButton(onClick={ explore(perspective.schoolId) }) { Text("看看这个思想") } }
    }
}
@Composable fun ReflectionCard(text: String) { XjdCard { Text("那时候的您写道",style=MaterialTheme.typography.titleMedium); Text(text.ifBlank { "当时的您什么也没留下。" },lineHeight=27.sp) } }
