package com.xiaojidiu.philosophy.ui.profile
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.components.*
import com.xiaojidiu.philosophy.util.displayDate

@Composable fun SnapshotScreen(journal: Journal,back: ()->Unit) {
    var selected by rememberSaveable {mutableStateOf(PhilosophyDimension.Existentialism.name)}
    val dimension=PhilosophyDimension.valueOf(selected)
    val history=journal.snapshots.take(30).reversed()
    val line=MaterialTheme.colorScheme.primary
    Scaffold(topBar={XjdTopBar("我的思想轨迹",back)}) { p -> LazyColumn(Modifier.padding(p),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {Text("我的脑回路怎么变成这样的？",style=MaterialTheme.typography.headlineMedium)}
        if(history.isEmpty()) item {EmptyState("先留下一次选择，轨迹就开始了。")}
        else {
            item {LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(PhilosophyDimension.entries,key={it.name}) {d->FilterChip(selected==d.name,{selected=d.name},label={Text(d.title)})}}}
            item {XjdCard {Text("${dimension.title} · 最近 ${history.size} 次变化");Text("倾向程度 0—100，越靠上表示越倾向；样本量增加也会让图像更清晰。",style=MaterialTheme.typography.bodyMedium)
                Canvas(Modifier.fillMaxWidth().height(160.dp).semantics {contentDescription=history.joinToString("；") {"${displayDate(it.createdAt)}：${it.scores[dimension]}%"}}) {
                    listOf(0f,.5f,1f).forEach {y->drawLine(line.copy(alpha=.15f),Offset(0f,size.height*y),Offset(size.width,size.height*y),2f)}
                    val points=history.mapIndexed {i,s->Offset(size.width*i/(history.size-1).coerceAtLeast(1),size.height*(1-(s.scores[dimension] ?:50)/100f))}
                    points.zipWithNext().forEach {(a,b)->drawLine(line,a,b,4f)};points.forEach {drawCircle(line,5f,it)}
                };Text("最早 → 最近",color=MaterialTheme.colorScheme.onSurfaceVariant)
            }}
            items(journal.snapshots,key={it.id}) {s->XjdCard {Text(displayDate(s.createdAt));Text("已探索 ${s.answerCount} 道不同问题");Text("${dimension.title} ${s.scores[dimension]}%",style=MaterialTheme.typography.titleLarge)}}
        }
    } }
}
