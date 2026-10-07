package com.xiaojidiu.philosophy.data.repository

import android.content.Context
import com.xiaojidiu.philosophy.BuildConfig
import com.xiaojidiu.philosophy.data.local.*
import com.xiaojidiu.philosophy.data.remote.api.*
import com.xiaojidiu.philosophy.data.remote.dto.*
import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

class AiRepository(private val context: Context, private val preferences: PreferencesRepository, private val apiKeys: ApiKeyStore) {
    private val json=Json {ignoreUnknownKeys=true}
    // User credentials must never follow a redirect to another destination. No body/header logging.
    private val client=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).connectTimeout(15,TimeUnit.SECONDS).readTimeout(60,TimeUnit.SECONDS).callTimeout(75,TimeUnit.SECONDS).build()
    private fun retrofit(baseUrl: String)=Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
    private fun api(baseUrl: String)=retrofit(BackendUrl.normalize(baseUrl,BuildConfig.DEBUG)).create(AiApiService::class.java)
    private fun modelApi(baseUrl: String)=retrofit(ModelUrl.normalize(baseUrl,BuildConfig.DEBUG)).create(CompatibleModelApi::class.java)

    suspend fun checkConnection(baseUrl: String): BackendHealth {
        require(baseUrl.isNotBlank()) {"请先填写服务地址。"}
        return api(baseUrl).health().also {require(it.status=="ok") {"服务尚未就绪。"}}
    }
    suspend fun testDirectConnection(baseUrl: String, model: String, key: String, provider: String) {
        val response=modelApi(baseUrl).complete("Bearer $key",ChatCompletionRequest(model,listOf(ChatMessage("user","连接测试，请只回复 OK。")),false,16,enableThinking=if(provider=="qwen") false else null))
        if(response.choices.firstOrNull()?.message?.content.isNullOrBlank()) throw ModelConfigurationException("服务没有返回测试回复，请确认模型支持非流式文本输出。")
    }
    suspend fun demos(): List<RoundTableResponse> = withContext(Dispatchers.IO) { context.assets.open("seed/conversations.json").bufferedReader().use {json.decodeFromString(it.readText())} }
    suspend fun request(topic: String, selected: List<String>, contextMessages: List<DebateMessage>, philosophers: List<Philosopher>, demoIndex: Int, replyTo: String? = null, summaryOnly: Boolean = false): Pair<RoundTableResponse, Boolean> {
        val settings=preferences.settings.first()
        if(settings.aiMode=="demo") {
            require(contextMessages.isEmpty()) {"继续真实会议前，请先在设置中连接讨论服务。"}
            delay(600)
            return demos()[demoIndex.coerceIn(0,2)] to true
        }
        if(topic.isBlank() || topic.length>500 || selected.size !in 1..4) throw ModelConfigurationException("请填写会议话题，并邀请1至4位思想家。")
        if(contextMessages.size>40 || contextMessages.sumOf {it.content.length}>40000) throw ModelConfigurationException("会议上下文较长，请另开一场会议继续思考。")
        val speakers=com.xiaojidiu.philosophy.domain.usecase.DiscussionTurnPlanner.speakers(selected,contextMessages,replyTo)
        val request=RoundTableRequest(topic,selected,contextMessages.map {RoundTableMessage(it.id,it.philosopherId,it.displayName,it.content,it.responseTo,it.type)},mode=if(summaryOnly) "summary" else "discussion",speakerIds=speakers,replyTo=replyTo,includeSummary=!summaryOnly && contextMessages.count {it.type=="user"}>=2)
        val response=if(settings.aiMode=="direct") {
            val key=try {apiKeys.readFor(settings.modelBaseUrl)} catch(e: CancellationException) {throw e} catch(e: Exception) {throw ModelConfigurationException("无法读取已保存的 API Key，请重新填写并保存。")}
                ?: throw ModelConfigurationException("请先保存这个接口地址对应的 API Key。")
            directRequest(settings,request,philosophers,key)
        } else api(settings.aiBaseUrl).discuss(request)
        val validated=try {
            val result=ResponseValidator.validate(response,speakers,philosophers.associate {it.id to it.name},contextMessages.map {it.id}.toSet(),summaryOnly)
            if(!summaryOnly) require(result.messages.size<=speakers.size && result.messages.map {it.philosopherId}.distinct().size==result.messages.size)
            if(request.includeSummary) require(result.summary!=null)
            result
        } catch(e: IllegalArgumentException) {throw ModelConfigurationException("模型未遵守本轮人数或小结格式，请重试或更换支持 JSON 输出的模型。")}
        return validated.copy(topic=topic) to false
    }
    private suspend fun directRequest(settings: SettingsState, request: RoundTableRequest, philosophers: List<Philosopher>, key: String): RoundTableResponse {
        val prompt=withContext(Dispatchers.IO) {context.assets.open("ai/round_table_system.txt").bufferedReader().use {it.readText().removePrefix("\uFEFF")}}
        val people=philosophers.filter {it.id in request.philosopherIds}.map {p->buildJsonObject {put("id",p.id);put("name",p.name);put("focus",p.focus);put("ideas",p.ideas);put("source",p.source)}}
        val material=buildJsonObject {put("turnId",UUID.randomUUID().toString());put("discussion",json.encodeToJsonElement(request));put("participants",JsonArray(people))}
        val result=modelApi(settings.modelBaseUrl).complete("Bearer $key",ChatCompletionRequest(settings.modelName,listOf(ChatMessage("system",prompt),ChatMessage("user",material.toString())),false,4096,JsonOutputFormat("json_object"),if(settings.modelProvider=="qwen") false else null))
        val first=result.choices.firstOrNull() ?: throw ModelConfigurationException("模型没有返回发言，请重试。")
        if(first.finishReason=="length") throw ModelConfigurationException("模型回复被截断，请重试或更换模型。")
        return json.decodeFromString(first.message.content ?: throw ModelConfigurationException("模型没有返回文本，请确认模型支持非流式 JSON 输出。"))
    }
}

