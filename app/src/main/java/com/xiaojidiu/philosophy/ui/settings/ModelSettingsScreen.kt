package com.xiaojidiu.philosophy.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaojidiu.philosophy.ui.components.*

private fun Context.activity(): Activity?=when(this) {is Activity->this;is ContextWrapper->baseContext.activity();else->null}

@Composable fun ModelSettingsScreen(vm: ModelSettingsViewModel, back: ()->Unit) {
    val state by vm.settings.collectAsStateWithLifecycle()
    val provider by vm.provider.collectAsStateWithLifecycle();val url by vm.baseUrl.collectAsStateWithLifecycle();val model by vm.model.collectAsStateWithLifecycle()
    val backend by vm.backendUrl.collectAsStateWithLifecycle();val key by vm.key.collectAsStateWithLifecycle();val hasKey by vm.hasKey.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle();val ready by vm.ready.collectAsStateWithLifecycle();val error by vm.errorField.collectAsStateWithLifecycle()
    var visible by rememberSaveable {mutableStateOf(false)};var remove by rememberSaveable {mutableStateOf(false)}
    val window=LocalContext.current.activity()?.window
    val keyboard=LocalSoftwareKeyboardController.current
    DisposableEffect(window) {
        val originallySecure=(window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_SECURE != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {if(!originallySecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)}
    }
    Scaffold(topBar={XjdTopBar("模型与 API Key",back)}) {p->
        LazyColumn(Modifier.padding(p).imePadding().testTag("model_settings_list"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {PageHeading("连接您的大模型","保存自己的 Key，让圆桌会议开始讨论。")}
            if(!ready) item {LoadingState()} else {
                item {Text(when(state?.aiMode) {"direct"->"当前已启用手机直连 · ${state?.modelName}";"backend"->"当前使用自有圆桌服务";else->"当前使用演示模式"},style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}
                item {LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(listOf("qwen" to "千问","compatible" to "兼容接口","backend" to "自有服务")) {(id,label)->FilterChip(provider==id,{vm.choose(id)},enabled=!busy,label={Text(label)},leadingIcon=if(provider==id) ({Icon(Icons.Outlined.Check,null,Modifier.size(18.dp))}) else null)}}}
                if(provider=="backend") {
                    item {Text("自有服务需要提供圆桌协议接口。千问或其他供应商的地址，请选择上面的直连方式。",style=MaterialTheme.typography.bodyMedium)}
                    item {OutlinedTextField(backend,vm::editBackend,Modifier.fillMaxWidth().testTag("backend_url"),label={Text("自有圆桌服务地址")},isError=error=="backend",enabled=!busy,singleLine=true,shape=MaterialTheme.shapes.medium)}
                } else {
                    if(provider=="qwen") item {
                        Column {Text("地址与 API Key 需属于同一地域。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(listOf("北京" to "https://dashscope.aliyuncs.com/compatible-mode/v1/","新加坡" to "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/","弗吉尼亚" to "https://dashscope-us.aliyuncs.com/compatible-mode/v1/")) {(label,address)->FilterChip(url==address,{vm.region(address)},enabled=!busy,label={Text(label)})}}
                        }
                    }
                    item {OutlinedTextField(url,vm::editUrl,Modifier.fillMaxWidth().testTag("model_url"),label={Text("接口地址 / Base URL")},placeholder={Text("https://…/v1/")},supportingText={Text(if(provider=="qwen") "已预填千问兼容地址，可改为业务空间专属地址。" else "需要支持 Chat Completions 和 JSON 输出。")},isError=error=="url",enabled=!busy,shape=MaterialTheme.shapes.medium,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Uri))}
                    item {OutlinedTextField(model,vm::editModel,Modifier.fillMaxWidth().testTag("model_name"),label={Text("模型名称")},placeholder={Text("例如 qwen-plus")},isError=error=="model",enabled=!busy,singleLine=true,shape=MaterialTheme.shapes.medium)}
                    item {OutlinedTextField(key,vm::editKey,Modifier.fillMaxWidth().testTag("model_key"),label={Text("API Key")},placeholder={Text(if(hasKey) "已加密保存，留空保留同一地址的 Key" else "粘贴您自己的 API Key")},supportingText={Text(if(hasKey) "修改接口地址后，请同时填写对应的 Key。" else "密钥使用 Android Keystore 加密保存在本机。")},isError=error=="key",enabled=!busy,singleLine=true,visualTransformation=if(visible) VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrectEnabled=false),trailingIcon={IconButton(onClick={visible=!visible}) {Icon(if(visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,if(visible) "隐藏已输入的 Key" else "显示已输入的 Key")}},shape=MaterialTheme.shapes.medium)}
                }
                item {
                    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        if(status.isNotEmpty()) Text(status,Modifier.testTag("model_status").semantics {liveRegion=LiveRegionMode.Polite},style=MaterialTheme.typography.bodyMedium,color=if(error!=null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        Button(onClick={keyboard?.hide();vm.save()},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),shape=MaterialTheme.shapes.medium) {Text(if(busy) "正在处理……" else "保存配置")}
                        OutlinedButton(onClick={keyboard?.hide();vm.test()},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text(if(provider=="backend") "检查自有服务连接" else "测试连接（调用一次模型）")}
                        Text(if(provider=="backend") "检查仅查询服务状态，不调用模型。" else "测试会发送一条简短消息，可能产生供应商费用。保存配置不会自动调用模型。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {XjdCard {Text("您的 Key，您的选择",style=MaterialTheme.typography.titleMedium);Text("开始会议时，话题与这场会议的上下文会直接发送到您填写的模型服务。其他回答、Reflection 与思想画像不随请求发送。Key 不写入代码、日志或备份。",style=MaterialTheme.typography.bodyMedium)}}
                item {TextButton(onClick=vm::demo,enabled=!busy) {Text("使用演示模式")};if(hasKey) TextButton(onClick={remove=true},enabled=!busy) {Text("移除已保存的 API Key")}}
            }
        }
    }
    if(remove) AlertDialog(onDismissRequest={remove=false},title={Text("移除 API Key？")},text={Text("本机保存的 API Key 会被移除，圆桌会议切换为演示模式。以后可以重新填写。")},confirmButton={TextButton(onClick={remove=false;vm.removeKey()}) {Text("确认移除")}},dismissButton={TextButton(onClick={remove=false}) {Text("留着吧")}})
}
