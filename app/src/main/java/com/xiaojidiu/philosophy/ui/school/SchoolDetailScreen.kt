package com.xiaojidiu.philosophy.ui.school
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.components.*
import androidx.compose.ui.platform.testTag

@Composable fun SchoolDetailScreen(s: School?,library: Library,back: ()->Unit,philosopher: (String)->Unit,question: (String)->Unit,favorite: (@Composable ()->Unit)?=null) {
    Scaffold(topBar={XjdTopBar(s?.name ?: "思想流派",back)}) { padding -> LazyColumn(Modifier.padding(padding).testTag("school_list"),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        if(s==null) item { LoadingState() } else {
            item { XjdCard { Text(s.name,style=MaterialTheme.typography.headlineLarge); Text(s.intro,lineHeight=28.sp); favorite?.invoke() } }
            item { XjdCard { Text("核心问题",style=MaterialTheme.typography.titleLarge); Text(s.coreQuestion,lineHeight=28.sp); Text("代表思想 · 编辑性概括",style=MaterialTheme.typography.titleMedium); Text(s.ideas,lineHeight=28.sp) } }
            item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { Text("可能误解",style=MaterialTheme.typography.titleLarge); Text(s.misconception,lineHeight=28.sp) } } }
            item { Text("代表／相关哲学家",style=MaterialTheme.typography.titleLarge) }
            items(library.philosophers.filter {it.id in s.philosopherIds},key={it.id}) { p -> XjdCard { Text(p.name,style=MaterialTheme.typography.titleMedium); Text(p.intro); TextButton(onClick={philosopher(p.id)}) {Text("看看他在想什么")} } }
            item { Text("相关问题",style=MaterialTheme.typography.titleLarge) }
            items(library.questions.filter { s.id in it.schoolIds || it.options.any {o->o.weights.keys.any {d->d.name==s.id}} },key={it.id}) { q -> XjdCard { Text(q.title,style=MaterialTheme.typography.titleMedium); TextButton(onClick={question(q.id)}) {Text("这个问题，想一下")} } }
            item { Text("内容参考：${s.source}",color=MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    } }
}
