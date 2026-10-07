package com.xiaojidiu.philosophy

import androidx.test.core.app.ApplicationProvider
import com.xiaojidiu.philosophy.data.remote.api.*
import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class DirectModelRepositoryTest {
    @Test fun encryptedKeyDirectRepliesContextAndRemoval()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<ThoughtsApplication>()
        val graph=app.graph
        val old=graph.preferences.settings.first()
        val oldKey=graph.apiKeys.readFor(old.modelBaseUrl)
        FakeModelService().use {service->
            try {
                graph.apiKeys.save(service.baseUrl,"test-user-key")
                graph.preferences.setDirectModel(service.baseUrl,"qwen-plus","qwen")
                val encrypted=app.getSharedPreferences("model_credentials",0).getString("encrypted",null)!!
                assertFalse(encrypted.contains("test-user-key"))
                assertEquals("test-user-key",graph.apiKeys.readFor(service.baseUrl))
                assertNull(graph.apiKeys.readFor("https://other.example/v1/"))
                val catalog=Json.decodeFromString<List<Philosopher>>(app.assets.open("seed/philosophers.json").bufferedReader().use {it.readText()})
                graph.ai.testDirectConnection(service.baseUrl,"qwen-plus","test-user-key","qwen")
                val (response,demo)=graph.ai.request("努力为了什么？",listOf("zhuangzi"),emptyList(),catalog,0)
                assertFalse(demo);assertEquals("庄子",response.messages.first().displayName)
                val prior=response.messages.first().let {DebateMessage(it.id,it.philosopherId,it.displayName,it.content,it.responseTo,it.type)}
                val user=DebateMessage("user-1","user","您","我不同意。",null,"user")
                val (follow,_)=graph.ai.request("努力为了什么？",listOf("zhuangzi"),listOf(prior,user),catalog,0)
                assertEquals("user-1",follow.messages.first().responseTo)
                assertEquals(3,service.requests.size)
                val sent=service.requests.last()
                assertEquals("Bearer test-user-key",sent.authorization)
                assertEquals(false,sent.json["enable_thinking"]!!.jsonPrimitive.boolean)
                assertEquals("json_object",sent.json["response_format"]!!.jsonObject["type"]!!.jsonPrimitive.content)
                assertFalse(sent.json.toString().contains("test-user-key"))
                graph.apiKeys.clear()
                try {graph.ai.request("努力为了什么？",listOf("zhuangzi"),emptyList(),catalog,0);fail("Missing key must fail, not fall back to demo")} catch(expected: ModelConfigurationException) {assertTrue(expected.message!!.contains("API Key"))}
                assertEquals(3,service.requests.size)
            } finally {
                if(oldKey!=null) graph.apiKeys.save(old.modelBaseUrl,oldKey) else graph.apiKeys.clear()
                graph.preferences.setDirectModel(old.modelBaseUrl,old.modelName,old.modelProvider)
                if(old.aiMode=="backend") graph.preferences.setAiBaseUrl(old.aiBaseUrl) else if(old.aiMode=="demo") graph.preferences.setDemoMode()
            }
        }
    }
    @Test fun invalidProviderKeyProducesSpecificError()=runBlocking {
        FakeModelService(401).use {service->
            val graph=ApplicationProvider.getApplicationContext<ThoughtsApplication>().graph
            try {graph.ai.testDirectConnection(service.baseUrl,"qwen-plus","test-invalid-key","qwen");fail("401 must fail")} catch(error: Exception) {
                val message=AiErrorMessages.describe(error)
                assertTrue(message.contains("API Key 无效"));assertFalse(message.contains("test-invalid-key"))
            }
        }
    }
}
