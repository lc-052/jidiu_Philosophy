package com.xiaojidiu.philosophy.ui.philosopher
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

@Composable fun PhilosopherDetailScreen(p: Philosopher?, library: Library, back: ()->Unit, school: (String)->Unit, question: (String)->Unit, map: ()->Unit, favorite: (@Composable ()->Unit)? = null) {
    Scaffold(topBar={XjdTopBar(p?.name ?: "哲学家",back)}) { padding -> LazyColumn(Modifier.padding(padding).testTag("philosopher_list"),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        if(p==null) item { LoadingState() } else {
            item { XjdCard { PhilosopherAvatar(p.id,name=p.name); Text(p.name,style=MaterialTheme.typography.headlineLarge); Text("${p.years}\n${p.region}"); Text(p.intro,lineHeight=27.sp); favorite?.invoke() } }
            item { XjdCard { Text("他主要在想什么？",style=MaterialTheme.typography.titleLarge); Text(p.focus,lineHeight=27.sp); Text("思想概括",style=MaterialTheme.typography.titleMedium); Text(p.ideas,lineHeight=28.sp) } }
            item { XjdCard { Text("代表著作",style=MaterialTheme.typography.titleLarge); p.works.forEach {Text("• $it",lineHeight=26.sp)}; Text("内容参考：${p.source}",color=MaterialTheme.colorScheme.onSurfaceVariant) } }
            item { Text("相关思想",style=MaterialTheme.typography.titleLarge); Text("相关不等于该哲学家自称属于此流派。",color=MaterialTheme.colorScheme.onSurfaceVariant) }
            if(p.schoolIds.isEmpty()) item { Text("他的思想跨越多个问题，可从下面的相关问题继续探索。") }
            p.schoolIds.forEach { id -> item(key=id) { library.schools.find {it.id==id}?.let { XjdCard { Text(it.name,style=MaterialTheme.typography.titleMedium); Text(it.intro); TextButton(onClick={school(id)}) {Text("去看看")} } } } }
            item { Text("相关问题",style=MaterialTheme.typography.titleLarge) }
            items(library.questions.filter {p.id in it.philosopherIds},key={it.id}) { q -> XjdCard { Text(q.title,style=MaterialTheme.typography.titleMedium); TextButton(onClick={question(q.id)}) {Text("先选，再想")} } }
            item { OutlinedButton(onClick=map,modifier=Modifier.fillMaxWidth()) {Text("继续探索思想关系")} }
        }
    } }
}

