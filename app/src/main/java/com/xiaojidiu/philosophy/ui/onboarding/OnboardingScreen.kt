package com.xiaojidiu.philosophy.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.xiaojidiu.philosophy.ui.components.XiaojidiuMascot

@Composable fun SplashScreen(done: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true; delay(700); done() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible, enter = fadeIn()) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) { XiaojidiuMascot(); Text("小寄丢大道理", style = MaterialTheme.typography.headlineMedium); Text("没事想两下，\n万一想明白了呢。", style = MaterialTheme.typography.bodyLarge) } }
    }
}
@Composable fun OnboardingScreen(complete: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf("这里没有标准答案", "先选，再想", "看看自己的脑回路")
    val descriptions = listOf("我们不会告诉您应该相信什么。", "面对一些奇怪的问题，\n看看自己真正重视什么。", "想得越多，\n您的思想画像就会慢慢出现。")
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp)); XiaojidiuMascot(thinking = page == 1)
        Text("${page+1} / 3", color = MaterialTheme.colorScheme.primary)
        Crossfade(page, label = "引导页") { i -> Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { Text(titles[i], style = MaterialTheme.typography.headlineLarge); Text(descriptions[i], style = MaterialTheme.typography.bodyLarge) } }
        Button(onClick = { if (page == 2) complete() else page++ }, modifier = Modifier.fillMaxWidth()) { Text(if (page == 2) "开始胡思乱想" else "继续看看") }
        if (page > 0) TextButton(onClick = { page-- }) { Text("上一页") }
    }
}
