package com.xiaojidiu.philosophy.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.domain.usecase.PhilosophyProfileCalculator
import com.xiaojidiu.philosophy.domain.usecase.ThinkingStreak
import com.xiaojidiu.philosophy.ui.components.*

@Composable fun ThoughtBubble(dimension: PhilosophyDimension, score: Int, evidence: Int, onClick: () -> Unit) {
    val palette=listOf(Color(0xFFE4DFFF),Color(0xFFD5EFFD),Color(0xFFFFEAA4),Color(0xFFCDF3DD))
    Box(Modifier.width((116+score*.3).dp).heightIn(min=(88+score*.3).dp).clip(RoundedCornerShape(60.dp)).background(palette[dimension.ordinal%4]).clickable(role=Role.Button,onClick=onClick).semantics(mergeDescendants=true) { contentDescription="${dimension.title}，${if(evidence==0) "尚未探索" else "倾向程度$score%，来自${evidence}道问题"}" }.padding(16.dp),contentAlignment=Alignment.Center) {
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(dimension.title,color=Color(0xFF24222B),style=MaterialTheme.typography.titleMedium)
            Text(if(evidence==0) "未探索" else "$score%",color=Color(0xFF494154),style=MaterialTheme.typography.titleLarge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ProfileScreen(library: Library, journal: Journal, school: (String)->Unit, history: ()->Unit, favorites: ()->Unit, snapshots: ()->Unit, settings: ()->Unit, identity: com.xiaojidiu.philosophy.data.local.SettingsState=com.xiaojidiu.philosophy.data.local.SettingsState(), editIdentity: ()->Unit={},music: ()->Unit={}) {
    val profile=remember(library.questions,journal.answers) { PhilosophyProfileCalculator().calculate(library.questions,journal.answers) }
    var expanded by rememberSaveable {mutableStateOf(false)}
    var explanation by rememberSaveable {mutableStateOf(false)}
    val sorted=profile.scores.entries.sortedWith(compareByDescending<Map.Entry<PhilosophyDimension,Int>> { (profile.evidenceCounts[it.key] ?: 0)>0 }.thenByDescending {it.value})
    LazyColumn(Modifier.testTag("profile_list"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { PageHeading("我的脑回路","选择会留下痕迹，也允许慢慢改变。") { TextButton(onClick=settings) { Text("设置") } } }
        item {
            XjdCard {
                Text("您已经认真胡思乱想了 ${journal.answers.size} 次。",style=MaterialTheme.typography.titleMedium)
                Text("连续胡思乱想 ${ThinkingStreak.calculate(journal.answers.map {it.answeredAt})} 天",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.primary)
            }
        }
        item { XjdCard { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {UserAvatar(identity.avatar);Column(Modifier.weight(1f)) {Text(identity.nickname,style=MaterialTheme.typography.titleLarge);Text("我在圆桌上",style=MaterialTheme.typography.bodyMedium)};TextButton(onClick=editIdentity) {Text("编辑形象")}} } }
        item { NavigationRow("阅读 BGM","导入喜欢的音乐，慢慢想",Icons.Outlined.MusicNote,music) }
        item { SectionHeading("我的思考记录") }
        item { NavigationRow("以前都想了些什么","回看每一次选择与留下的念头",Icons.Outlined.History,history) }
        item { NavigationRow("先存着，以后再想","问题、人物、流派与圆桌收藏",Icons.Outlined.BookmarkBorder,favorites) }
        item { NavigationRow("我的脑回路怎么变成这样的？","沿着时间，看看思想如何变化",Icons.Outlined.Timeline,snapshots) }
        item { SectionHeading("思想画像",profile.clarity) }
        item {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                (if(expanded) sorted else sorted.take(6)).forEach { (dimension,score) -> ThoughtBubble(dimension,score,profile.evidenceCounts[dimension] ?: 0) { school(dimension.name) } }
            }
        }
        item { TextButton(onClick={expanded=!expanded}) {Text(if(expanded) "收起部分脑回路" else "展开全部思想维度")} }
        item {
            XjdCard {
                Text("这只是根据您的选择生成的一种观察方式，不是什么科学人格测试，更不是对您的定义。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick={explanation=!explanation}) {Text(if(explanation) "收起说明" else "画像是怎么来的？")}
                if(explanation) Text("每道题只用最新选择形成画像，历史回答仍会保留。样本少时，分数更靠近中间。点击气泡可以了解对应思想。",style=MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

