package com.xiaojidiu.philosophy.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaojidiu.philosophy.BuildConfig
import com.xiaojidiu.philosophy.ui.components.*
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.MusicNote

@Composable fun SettingsScreen(vm: SettingsViewModel,back: ()->Unit,info: (String)->Unit) {
    val state by vm.settings.collectAsStateWithLifecycle();val message by vm.message.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle()
    val context=LocalContext.current;var delete by rememberSaveable {mutableIntStateOf(0)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {allowed->val s=state;if(s!=null) vm.reminder(allowed,s.hour,s.minute);if(!allowed) vm.notice("通知权限未开启，每日提醒保持关闭。您可以稍后在系统设置中授权。")}
    Scaffold(topBar={XjdTopBar("设置",back)}) {p->LazyColumn(Modifier.padding(p).imePadding().testTag("settings_list"),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        val s=state
        if(s==null) item {LoadingState()} else {
            item {SectionHeading("外观与节奏")}
            item {
                XjdCard {
                    Text("舒服地想",style=MaterialTheme.typography.titleLarge)
                    listOf("system" to "跟随系统","light" to "浅色","dark" to "深色").forEach { (key,label) ->
                        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).selectable(selected=s.theme==key,enabled=!busy,role=Role.RadioButton,onClick={vm.theme(key)}),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(s.theme==key,null,enabled=!busy)
                            Text(label)
                        }
                    }
                }
            }
            item {XjdCard {Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {Text("每日提醒",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium);Switch(s.reminder,{enabled->if(enabled && Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.reminder(enabled,s.hour,s.minute)},enabled=!busy)}
                OutlinedButton(onClick={TimePickerDialog(context,{_,h,m->vm.reminder(s.reminder,h,m)},s.hour,s.minute,true).show()},enabled=!busy) {Text("提醒时间 · %02d:%02d 左右".format(s.hour,s.minute))}
                Text("完全可关闭。系统省电、通知权限和时区变化可能影响送达时间；这不是精确闹钟。",lineHeight=25.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }}
            item {NavigationRow("阅读 BGM",if(s.bgmTracks.isEmpty()) "导入本地音频，为阅读配个背景音" else "${s.bgmTracks.size} 首音乐 · ${s.bgmName}",Icons.Outlined.MusicNote) {info("bgm")}}
            item {SectionHeading("圆桌会议 · 大模型服务")}
            item {NavigationRow("配置模型与 API Key",when(s.aiMode) {"direct"->"手机直连 · ${s.modelName}";"backend"->"自有圆桌服务";else->"千问、兼容接口或演示模式"},Icons.Outlined.VpnKey) {info("model")}}
            item {SectionHeading("数据与隐私","这些记录保存在您的设备上。")}
            item {XjdCard {Text("本地记录",style=MaterialTheme.typography.titleMedium);OutlinedButton(onClick={delete=1},enabled=!busy) {Text("清除回答历史")};OutlinedButton(onClick={delete=2},enabled=!busy) {Text("清除全部本地数据")}}}
            item {XjdCard {TextButton(onClick={info("about")}) {Text("关于小寄丢")};TextButton(onClick={info("privacy")}) {Text("隐私说明")};Text("App Version ${BuildConfig.VERSION_NAME}")}}
            if(message.isNotEmpty()) item {Text(message,lineHeight=26.sp)}
        }
    }}
    if(delete!=0) AlertDialog(onDismissRequest={delete=0},title={Text(if(delete==1) "清除回答历史？" else "清除全部本地数据？")},text={Text(if(delete==1) "回答、Reflection 和思想画像轨迹会被永久清除。收藏与讨论会保留。此操作无法撤销。" else "回答、Reflection、收藏、画像轨迹、讨论、导入的音乐副本和设置会被永久清除。内置哲学内容会保留，并重新显示首次引导。此操作无法撤销。")},confirmButton={TextButton(onClick={vm.clear(delete==2);delete=0}) {Text("确认清除")}},dismissButton={TextButton(onClick={delete=0}) {Text("留着吧")}})
}
@Composable fun InfoScreen(kind: String,back: ()->Unit) {
    Scaffold(topBar={XjdTopBar(if(kind=="about") "关于小寄丢" else "隐私说明",back)}) {p->LazyColumn(Modifier.padding(p),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        if(kind=="about") {
            item {XiaojidiuMascot();Text("小寄丢大道理",style=MaterialTheme.typography.headlineLarge);Text("没事想两下，万一想明白了呢。",lineHeight=28.sp)}
            item {XjdCard {Text("我们想让您在选择、记录和回看中发现自己相信什么。这里没有标准答案，思想画像不是科学人格测评。",lineHeight=28.sp);Text("哲学内容均为通俗概括或情境改写，出处见各问题和详情页。圆桌是基于思想的现代推演，不冒充历史人物原话。",lineHeight=28.sp)}}
        } else {
            item {XjdCard {Text("您的念头留在设备上",style=MaterialTheme.typography.titleLarge);Text("回答、Reflection、收藏、画像与讨论存储在设备本地。没有登录、广告或统计 SDK，Android 系统备份已关闭。清除数据或卸载会失去这些记录。",lineHeight=28.sp)}}
            item {XjdCard {Text("圆桌的网络请求",style=MaterialTheme.typography.titleLarge);Text("演示模式完全使用内置内容。手机直连模式把话题、所选哲学家、本场上下文和您填写的 API Key 发送到您配置的模型接口；自有服务模式把讨论数据发送到配置的 Backend。其他回答、Reflection 和思想画像不随请求发送。测试连接会调用一次模型并可能产生费用。",lineHeight=28.sp);Text("API Key 由 Android Keystore 加密后保存在设备上，不写入代码、日志或备份。移除密钥、清除全部本地数据或卸载会失去该密钥。模型服务的日志与保留策略由供应商或部署者决定，请查阅相应隐私政策。",lineHeight=28.sp)}}
            item {XjdCard {Text("背景音乐只在本地",style=MaterialTheme.typography.titleLarge);Text("导入时只读取您在系统选择器中选中的音频，并复制到 App 私有空间。不会扫描整部手机，也不会上传音频。移除时仅删除副本；清除全部数据或卸载会移除副本。",lineHeight=28.sp)}}
            item {Text("每日提醒由 Android 本地后台任务实现，可以完全关闭。",lineHeight=28.sp)}
        }
    }}
}
