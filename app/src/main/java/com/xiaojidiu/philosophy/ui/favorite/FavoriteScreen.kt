package com.xiaojidiu.philosophy.ui.favorite
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.components.*

@Composable fun FavoriteButton(type: String,id: String,journal: Journal,toggle: ()->Unit) {
    val saved=journal.favorites.any {it.type==type && it.targetId==id}
    OutlinedButton(onClick=toggle) {Text(if(saved) "✓ 已存着 · 取消收藏" else "先存着，以后再想")}
}
@Composable fun FavoriteScreen(library: Library,journal: Journal,back: ()->Unit,open: (Favorite)->Unit,remove: (Favorite)->Unit,conversationLabel: (String)->String={"已存的圆桌讨论"}) {
    var filter by rememberSaveable {mutableStateOf("all")}
    val groups=listOf("all" to "全部","question" to "问题","philosopher" to "哲学家","school" to "流派","conversation" to "圆桌")
    val visible=journal.favorites.filter {filter=="all" || it.type==filter}
    Scaffold(topBar={XjdTopBar("先存着，以后再想",back)}) { p -> LazyColumn(Modifier.padding(p),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Text("把想继续探索的，留在这里。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        item {LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(groups,key={it.first}) {(key,label)->FilterChip(filter==key,{filter=key},label={Text(label)})}}}
        if(visible.isEmpty()) item {EmptyState(if(journal.favorites.isEmpty()) "什么大道理都没存。" else "这里还没存下东西，去其他分类看看。")}
        items(visible,key={it.type+it.targetId}) { f ->
            val title=when(f.type) {"question"->library.questions.find {it.id==f.targetId}?.title;"philosopher"->library.philosophers.find {it.id==f.targetId}?.name;"school"->library.schools.find {it.id==f.targetId}?.name;else->conversationLabel(f.targetId)}
            XjdCard { Text(when(f.type) {"question"->"脑洞问题";"philosopher"->"哲学家";"school"->"思想流派";else->"圆桌讨论"},color=MaterialTheme.colorScheme.primary);Text(title ?: "内容已变更",style=MaterialTheme.typography.titleLarge)
                Row { TextButton(onClick={open(f)},enabled=title!=null) {Text("继续想想")};TextButton(onClick={remove(f)}) {Text("取消收藏")} }
            }
        }
    } }
}
