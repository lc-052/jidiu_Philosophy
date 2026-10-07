package com.xiaojidiu.philosophy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading

@Composable fun XiaojidiuMascot(modifier: Modifier = Modifier, thinking: Boolean = false) {
    Canvas(modifier.size(112.dp).semantics { contentDescription = if (thinking) "正在思考的小寄丢" else "发呆的小寄丢" }) {
        val ink = Color(0xFF24222B); val s = size.width
        drawOval(Color(0xFFFFD966), Offset(s*.15f,s*.19f), Size(s*.7f,s*.62f))
        drawOval(ink, Offset(s*.15f,s*.19f), Size(s*.7f,s*.62f), style = Stroke(s*.025f))
        drawLine(ink,Offset(s*.34f,s*.78f),Offset(s*.30f,s*.87f),s*.027f)
        drawLine(ink,Offset(s*.64f,s*.78f),Offset(s*.69f,s*.87f),s*.027f)
        drawOval(ink,Offset(s*.34f,s*.43f),Size(s*.04f,s*.065f))
        drawOval(ink,Offset(s*.62f,s*.43f),Size(s*.04f,s*.065f))
        drawLine(ink,Offset(s*.46f,s*.61f),Offset(s*.54f,s*.61f),s*.025f)
        if (thinking) { drawCircle(Color(0xFF7C6CF2),s*.055f,Offset(s*.8f,s*.08f)); drawCircle(ink,s*.013f,Offset(s*.76f,s*.17f)) }
    }
}
@Composable fun XjdCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun XjdTopBar(title: String, back: (() -> Unit)? = null) {
    TopAppBar(title = { Text(title, fontSize = 20.sp) }, navigationIcon = { if (back != null) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
}
val mainRoutes = listOf("home", "explore", "debate", "profile")
@Composable fun XjdBottomBar(route: String?, navigate: (String) -> Unit) {
    val labels = listOf("今天", "探索", "圆桌会议", "我的")
    val icons = listOf(Icons.Outlined.Lightbulb, Icons.Outlined.Explore, Icons.Outlined.Forum, Icons.Outlined.Face)
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        mainRoutes.forEachIndexed { i, item -> NavigationBarItem(selected = route == item, onClick = { navigate(item) }, modifier=Modifier.testTag("tab_$item"), icon = { Icon(icons[i], null, Modifier.size(23.dp)) }, label = { Text(labels[i], fontWeight = if(route==item) FontWeight.Bold else FontWeight.Normal) }, colors = NavigationBarItemDefaults.colors(selectedIconColor=MaterialTheme.colorScheme.primary, selectedTextColor=MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer)) }
    }
}
@Composable fun EmptyState(message: String, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) { XiaojidiuMascot(); Text(message); action?.invoke() }
}
@Composable fun LoadingState(message: String = "想一下……") {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) { XiaojidiuMascot(thinking = true); LinearProgressIndicator(Modifier.width(120.dp)); Text(message) }
}
@Composable fun ErrorState(message: String, retry: () -> Unit) { XjdCard { Text(message); Button(onClick = retry) { Text("再试一次") } } }

@Composable fun PageHeading(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
            action?.invoke()
        }
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun SectionHeading(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One accessible target for a navigation entry, with an explicit destination and a quiet explanation. */
@Composable fun NavigationRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, Modifier.padding(10.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

