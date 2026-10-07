package com.xiaojidiu.philosophy.ui.music

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaojidiu.philosophy.data.repository.BgmRepository
import com.xiaojidiu.philosophy.ui.components.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private fun clock(ms: Int)="%d:%02d".format(ms/60000,ms/1000%60)

@Composable fun BgmScreen(vm: BgmViewModel,back: ()->Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {uris->vm.importAudio(uris)}
    var remove by rememberSaveable {mutableStateOf<String?>(null)}
    var volume by remember(state.volume) {mutableFloatStateOf(state.volume)}
    var seeking by remember {mutableStateOf(false)}
    var seekValue by remember {mutableFloatStateOf(0f)}
    Scaffold(topBar={XjdTopBar("阅读 BGM",back)}) {p->
        LazyColumn(Modifier.padding(p).testTag("bgm_list"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            item {PageHeading("给思考配个背景音","放一点喜欢的声音，慢慢读。")}
            item {XjdCard {
                Icon(Icons.Outlined.MusicNote,null,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary)
                Text(state.title.ifBlank {"还没有导入音乐"},style=MaterialTheme.typography.titleLarge)
                Text("可以一次选择多首。新歌会加入列表，音频只保存在设备上，不上传。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick={picker.launch(arrayOf("audio/*"))},enabled=!state.importing && !state.loading,modifier=Modifier.fillMaxWidth().testTag("bgm_import")) {Text(if(state.tracks.isEmpty()) "导入本地音频" else "添加音乐到列表")}
                Text("最多 50 首，每首不超过 100 MB；格式支持取决于设备。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(state.importing || state.loading) Text(if(state.importing) "正在把音乐加入列表……" else "正在准备播放……")
                state.notice?.let {Text(it,style=MaterialTheme.typography.bodyMedium)}
                if(state.error!=null) Text(state.error!!,Modifier.testTag("bgm_error"),color=MaterialTheme.colorScheme.error)
            }}
            if(state.tracks.isNotEmpty()) item {Text("我的播放列表 · ${state.tracks.size} 首",style=MaterialTheme.typography.titleLarge)}
            items(state.tracks,key={it.file}) {track->
                Surface(color=if(track.file==state.activeFile) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,shape=RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).heightIn(min=56.dp).clickable(enabled=!state.importing,onClickLabel="播放 ${track.title}") {vm.select(track.file)}.testTag("bgm_track_${track.file}").padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Icon(if(track.file==state.activeFile && state.playing) Icons.Outlined.GraphicEq else Icons.Outlined.MusicNote,null,tint=MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(track.title,style=MaterialTheme.typography.titleSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
                                Text((if(track.file==state.activeFile) "✓ 当前曲目 · " else "")+clock(track.duration),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick={remove=track.file},enabled=!state.importing,modifier=Modifier.testTag("bgm_remove_${track.file}")) {Icon(Icons.Outlined.Close,"移除 ${track.title}")}
                    }
                }
            }
            if(state.title.isNotBlank()) item {XjdCard {
                Text("正在播放 · ${state.tracks.indexOfFirst {it.file==state.activeFile}+1} / ${state.tracks.size}",style=MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    IconButton(onClick={vm.previous()},enabled=state.tracks.size>1 && !state.importing,modifier=Modifier.testTag("bgm_previous")) {Icon(Icons.Outlined.SkipPrevious,"上一首")}
                    Text(state.title,Modifier.weight(1f).padding(12.dp),style=MaterialTheme.typography.titleSmall)
                    IconButton(onClick={vm.next()},enabled=state.tracks.size>1 && !state.importing,modifier=Modifier.testTag("bgm_next")) {Icon(Icons.Outlined.SkipNext,"下一首")}
                }
                Button(onClick=vm::toggle,enabled=state.ready && !state.importing,modifier=Modifier.fillMaxWidth().testTag("bgm_play")) {Icon(if(state.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,null);Spacer(Modifier.width(8.dp));Text(if(state.playing) "暂停音乐" else "播放音乐")}
                if(state.ready) {
                    Text("${clock(state.position)} / ${clock(state.duration)}",style=MaterialTheme.typography.labelMedium)
                    Slider(value=if(seeking) seekValue else state.position.toFloat(),onValueChange={seeking=true;seekValue=it},onValueChangeFinished={vm.seek(seekValue.toInt());seeking=false},valueRange=0f..state.duration.coerceAtLeast(1).toFloat(),modifier=Modifier.testTag("bgm_seek").semantics {contentDescription="音乐播放进度"})
                }
                Text("音乐音量 · ${(volume*100).toInt()}%",style=MaterialTheme.typography.titleSmall)
                Slider(volume,{volume=it;vm.previewVolume(it)},onValueChangeFinished={vm.saveVolume(volume)},enabled=state.ready && !state.importing,modifier=Modifier.testTag("bgm_volume").semantics {contentDescription="背景音乐音量"})
                Text("播放方式",style=MaterialTheme.typography.titleSmall)
                Column {
                    listOf("list" to "列表循环","single" to "单曲循环","sequential" to "顺序播放").forEach {(mode,label)->
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            RadioButton(selected=state.mode==mode,onClick={vm.mode(mode)},enabled=!state.importing,modifier=Modifier.testTag("bgm_mode_$mode"))
                            Text(label,Modifier.weight(1f).heightIn(min=48.dp).wrapContentHeight().clickable(enabled=!state.importing) {vm.mode(mode)})
                        }
                    }
                }
                Text(if(state.mode=="sequential") "按列表播放，最后一首结束后暂停。" else if(state.mode=="single") "循环当前曲目，也可以手动切歌。" else "按列表播放，结束后回到第一首。",style=MaterialTheme.typography.bodySmall)
                Text("离开 App 时暂停；回到 App 可继续。来电、其他音频打断或耳机断开时会暂停。重新启动不会自动播放。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick={remove=state.activeFile},enabled=!state.importing && !state.loading,modifier=Modifier.testTag("bgm_remove")) {Text("移除这首音乐")}
            }}

        }
    }
    if(remove!=null) AlertDialog(onDismissRequest={remove=null},title={Text("从列表移除这首音乐？")},text={Text("只移除这首音乐的 App 内副本，列表里的其他音乐和手机原文件会保留。")},confirmButton={TextButton(onClick={remove?.let(vm::remove);remove=null}) {Text("确认移除")}},dismissButton={TextButton(onClick={remove=null}) {Text("继续留着")}})
}

@Composable fun BgmMiniBar(repository: BgmRepository,open: ()->Unit) {
    // Progress ticks should not recompose the global navigation controls.
    val scope=rememberCoroutineScope()
    val flow=remember(repository) {repository.state.map {Triple(it.title,it.playing,it.ready && !it.importing)}.distinctUntilChanged()}
    val state by flow.collectAsStateWithLifecycle(Triple("",false,false))
    if(state.first.isNotBlank()) Surface(color=MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().testTag("bgm_mini").padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
            Row(Modifier.weight(1f).testTag("bgm_mini_title").heightIn(min=48.dp).clickable(onClickLabel="打开背景音乐设置",onClick=open),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.MusicNote,null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.primary)
                Text(state.first,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelLarge)
            }
            IconButton(onClick={scope.launch {repository.previous()}},enabled=state.third,modifier=Modifier.testTag("bgm_mini_previous")) {Icon(Icons.Outlined.SkipPrevious,"上一首背景音乐")}
            IconButton(onClick={if(state.second) repository.pause() else repository.play()},enabled=state.third,modifier=Modifier.testTag("bgm_mini_toggle")) {Icon(if(state.second) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,if(state.second) "暂停背景音乐" else "播放背景音乐")}
            IconButton(onClick={scope.launch {repository.next()}},enabled=state.third,modifier=Modifier.testTag("bgm_mini_next")) {Icon(Icons.Outlined.SkipNext,"下一首背景音乐")}
        }
    }
}

