package com.xiaojidiu.philosophy.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaojidiu.philosophy.ui.components.*

@Composable fun IdentityScreen(vm: IdentityViewModel, back: ()->Unit) {
    val nickname by vm.nickname.collectAsStateWithLifecycle();val avatar by vm.avatar.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle();val status by vm.status.collectAsStateWithLifecycle()
    Scaffold(topBar={XjdTopBar("我在圆桌上",back)}) {padding->
        LazyColumn(Modifier.padding(padding).imePadding().testTag("identity_list"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            item {PageHeading("给自己留个位置","选张脸，带着自己的想法入座。")}
            if(!ready) item {LoadingState()} else {
                item {XjdCard {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {UserAvatar(avatar,size=76.dp);Column {Text(nickname.ifBlank {"还没起名字"},style=MaterialTheme.typography.titleLarge);Text("圆桌里的您",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
                item {OutlinedTextField(nickname,vm::edit,Modifier.fillMaxWidth().testTag("nickname_input"),label={Text("我的昵称")},supportingText={Text("最多 20 个字，用喜欢的名字就好。")},singleLine=true,enabled=!busy,shape=MaterialTheme.shapes.medium)}
                item {SectionHeading("挑一个头像","原创小头像，也可以随时换。")}
                userAvatarChoices.chunked(2).forEach {row->item {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {row.forEach {(id,label)->
                        val selected=avatar==id
                        OutlinedCard(onClick={vm.select(id)},enabled=!busy,modifier=Modifier.weight(1f).testTag("avatar_$id").semantics {this.selected=selected;contentDescription="${label}头像${if(selected) "，已选择" else ""}"},border=BorderStroke(if(selected) 2.dp else 1.dp,if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {UserAvatar(id,size=64.dp);Row(verticalAlignment=Alignment.CenterVertically) {Text(label);if(selected) {Spacer(Modifier.width(6.dp));Icon(Icons.Outlined.CheckCircle,"已选择",Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary)}}}
                        }
                    }}
                }}
                item {if(status.isNotEmpty()) Text(status,Modifier.testTag("identity_status").semantics {liveRegion=LiveRegionMode.Polite});Button(onClick=vm::save,enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text(if(busy) "正在保存……" else "保存我的形象")}}
                item {Text("昵称和头像只保存在本机，无需注册。昵称会出现在后续的圆桌发言中；历史发言保留当时的名字。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
        }
    }
}

