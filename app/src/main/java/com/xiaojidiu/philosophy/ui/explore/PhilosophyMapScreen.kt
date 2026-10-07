package com.xiaojidiu.philosophy.ui.explore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.components.*
@Composable fun PhilosophyMapScreen(library: Library,back: ()->Unit,open: (String)->Unit) {
    Scaffold(topBar={XjdTopBar("这些人到底什么关系？",back)}) { p -> LazyColumn(Modifier.padding(p),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item { Text("两条传统各有自己的问题。这里的分支展示影响、时代与分歧，不是一棵统一的师承树。",lineHeight=27.sp) }
        library.relations.groupBy {it.era}.forEach { (era,edges) ->
            item(key=era) { Text("● $era",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary) }
            items(edges,key={it.from+it.to}) { edge -> XjdCard {
                Text(edge.kind,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                Text("${library.philosophers.find {it.id==edge.from}?.name} ↔ ${library.philosophers.find {it.id==edge.to}?.name}",style=MaterialTheme.typography.titleMedium)
                Text(edge.description,lineHeight=27.sp)
                Row { TextButton(onClick={open(edge.from)}) {Text("看看前一位")}; TextButton(onClick={open(edge.to)}) {Text("看看后一位")} }
            } }
        }
    } }
}
