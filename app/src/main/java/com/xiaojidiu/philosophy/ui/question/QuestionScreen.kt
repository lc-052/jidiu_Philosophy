package com.xiaojidiu.philosophy.ui.question
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaojidiu.philosophy.domain.model.Question
import com.xiaojidiu.philosophy.ui.components.*

@Composable fun QuestionScreen(question: Question?, vm: QuestionViewModel, initialChoice: String?, back: () -> Unit, result: (Long) -> Unit) {
    val selected by vm.selected.collectAsStateWithLifecycle(); val id by vm.result.collectAsStateWithLifecycle(); val error by vm.error.collectAsStateWithLifecycle()
    LaunchedEffect(question?.id) { if(question != null && id == null && (selected!=null || initialChoice!=null)) vm.choose(question.id,selected ?: initialChoice!!) }
    LaunchedEffect(id) { id?.let(result) }
    Scaffold(topBar={ XjdTopBar("先选，再想",back) }) { padding ->
        LazyColumn(Modifier.padding(padding),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            item { Text("先听听自己的第一反应。",style=MaterialTheme.typography.titleMedium) }
            if(question==null) item { LoadingState() } else item { QuestionCard(question,selected,selected==null) { vm.choose(question.id,it.id) } }
            if(selected!=null) item { Text("记下这个选择……",color=MaterialTheme.colorScheme.primary) }
            if(error!=null) item { Text(error!!,color=MaterialTheme.colorScheme.error) }
        }
    }
}
