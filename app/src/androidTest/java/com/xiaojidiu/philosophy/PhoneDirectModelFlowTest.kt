package com.xiaojidiu.philosophy

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Dedicated test device only: validates phone UI -> provider HTTP -> saved meeting. */
class PhoneDirectModelFlowTest {
    @get:Rule val compose=createEmptyComposeRule()
    private fun waitText(text: String) {compose.waitUntil(15000) {compose.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}}
    private fun clickIn(list: String,text: String) {compose.onNodeWithTag(list).performScrollToNode(hasText(text));compose.onNodeWithText(text).performClick()}
    @Test fun enterKeySaveRecreateStartAndFollow() {
        val graph=ApplicationProvider.getApplicationContext<ThoughtsApplication>().graph
        runBlocking {graph.repository.clearUserData();graph.preferences.reset();graph.preferences.completeOnboarding()}
        FakeModelService().use {service->
            val scenario=ActivityScenario.launch(MainActivity::class.java)
            try {
                waitText("今天脑子");compose.onNodeWithTag("tab_debate").performClick()
                clickIn("debate_list","连接模型服务");waitText("接口地址 / Base URL")
                clickIn("model_settings_list","保存配置")
                compose.onNodeWithTag("model_status").assertTextContains("请填写您的 API Key",substring=true).assertIsDisplayed()
                assertTrue(service.requests.isEmpty())
                compose.onNodeWithTag("model_settings_list").performScrollToNode(hasTestTag("model_url"))
                compose.onNodeWithTag("model_url").performTextReplacement(service.baseUrl)
                Espresso.closeSoftKeyboard();compose.waitForIdle()
                compose.onNodeWithTag("model_settings_list").performScrollToNode(hasTestTag("model_key"))
                compose.onNodeWithTag("model_key").performTextInput("test-phone-key")
                Espresso.closeSoftKeyboard();clickIn("model_settings_list","保存配置")
                compose.waitUntil(5000) {runBlocking {graph.preferences.settings.first().aiMode=="direct"}}
                compose.onNodeWithTag("model_status").assertTextContains("配置已保存",substring=true).assertIsDisplayed()
                assertEquals(0,service.requests.size) // Saving never invokes a paid model.
                scenario.recreate();waitText("连接您的大模型")
                compose.onNodeWithTag("model_settings_list").performScrollToNode(hasTestTag("model_key"))
                compose.onNodeWithTag("model_key").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,AnnotatedString(""))) // Stored secret is never loaded into UI.
                clickIn("model_settings_list","测试连接（调用一次模型）");waitText("模型连接成功")
                assertEquals(1,service.requests.size)
                compose.onNodeWithContentDescription("返回").performClick();waitText("圆桌会议")
                compose.onNodeWithTag("debate_list").performScrollToNode(hasTestTag("topic_input"))
                compose.onNodeWithTag("topic_input").performTextInput("人为什么要努力？")
                Espresso.closeSoftKeyboard();clickIn("debate_list","开始会议")
                compose.waitUntil(10000) {runBlocking {graph.repository.conversations.first().any {!it.demo}}}
                val first=runBlocking {graph.repository.conversations.first().first()}
                assertFalse(first.demo);assertEquals("庄子",first.messages.first().displayName)
                compose.onNodeWithTag("followup_input").assertIsDisplayed()
                compose.onNodeWithTag("followup_input").performTextInput("我不同意，能再解释一下吗？")
                Espresso.closeSoftKeyboard();compose.onNodeWithTag("chat_send").performClick()
                compose.waitUntil(10000) {runBlocking {graph.repository.conversations.first().first().messages.size==3}}
                assertEquals(3,service.requests.size)
                assertEquals("Bearer test-phone-key",service.requests.last().authorization)
                val messages=service.requests.last().json["messages"]!!.jsonArray
                val material=Json.parseToJsonElement(messages[1].jsonObject["content"]!!.jsonPrimitive.content).jsonObject
                assertEquals(2,material["discussion"]!!.jsonObject["messages"]!!.jsonArray.size)
            } finally {scenario.close();runBlocking {graph.preferences.reset()}}
        }
    }
}



