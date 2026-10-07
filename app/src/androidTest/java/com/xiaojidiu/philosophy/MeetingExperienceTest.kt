package com.xiaojidiu.philosophy

import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileInputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MeetingExperienceTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val graph get()=ApplicationProvider.getApplicationContext<ThoughtsApplication>().graph
    private fun waitText(text: String)=compose.waitUntil(15000) {compose.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}
    private fun click(list: String,tag: String) {compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag));compose.onNodeWithTag(tag).performScrollTo().performClick()}
    private fun screenshot(name: String) {
        val ctx=ApplicationProvider.getApplicationContext<android.content.Context>()
        val image=File(ctx.getExternalFilesDir(null),"screenshots/$name.png").apply {parentFile!!.mkdirs()}
        image.outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("mkdir -p /sdcard/Download/little-thoughts-screenshots").use {FileInputStream(it.fileDescriptor).readBytes()}
        automation.executeShellCommand("cp ${image.absolutePath} /sdcard/Download/little-thoughts-screenshots/$name.png").use {FileInputStream(it.fileDescriptor).readBytes()}
    }
    private fun start() {
        waitText("今天脑子");compose.onNodeWithTag("tab_debate").performClick()
        click("debate_list","start_meeting")
        compose.waitUntil(15000) {runBlocking {graph.repository.conversations.first().isNotEmpty()}}
    }
    @Test fun demoDirectedReplySummarySourcesAndPersonalThought() {
        runBlocking {graph.repository.clearUserData();graph.preferences.reset();graph.preferences.completeOnboarding()}
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            start()
            compose.waitUntil(15000) {runBlocking {graph.repository.conversations.first().first().insights.isNotEmpty()}}
            val original=runBlocking {graph.repository.conversations.first().first()}
            compose.onNodeWithText(original.messages.first().content).performClick()
            compose.onNodeWithTag("reply_target").assertTextContains("尼采",substring=true)
            compose.onNodeWithTag("reply_example").performScrollTo().performClick()
            compose.onNodeWithTag("followup_input").assertTextContains("生活中的例子",substring=true)
            scenario.recreate();waitText("正在回应")
            compose.onNodeWithTag("reply_target").assertTextContains("尼采",substring=true)
            compose.onNodeWithTag("chat_send").performClick()
            compose.waitUntil(15000) {runBlocking {graph.repository.conversations.first().first().messages.size==5}}
            assertEquals(original.messages.first().id,runBlocking {graph.repository.conversations.first().first().messages.last().responseTo})
            compose.onNodeWithTag("debate_list").performScrollToNode(hasText("分歧小结"))
            compose.onNodeWithTag("meeting_members").assertDoesNotExist();screenshot("meeting-summary")
            compose.onAllNodesWithTag("source_${original.messages.first().id}")[0].performScrollTo().performClick()
            compose.waitForIdle();compose.onNodeWithTag("chat_philosopher_nietzsche").assertIsDisplayed()
            click("debate_list","finish_meeting");waitText("留个会后念头")
            compose.onNodeWithTag("favor_nietzsche").performClick()
            compose.onNodeWithTag("meeting_conclusion").performTextInput("我想给自己的目标留一点空间。")
            Espresso.closeSoftKeyboard();compose.onNodeWithTag("save_meeting_thought").performClick()
            compose.waitUntil(15000) {runBlocking {graph.repository.conversations.first().first().thought?.conclusion=="我想给自己的目标留一点空间。"}}
            compose.onNodeWithTag("debate_list").performScrollToIndex(0);screenshot("meeting-thought")
            compose.onNodeWithTag("debate_list").performScrollToNode(hasText("看看以前的讨论"));compose.onNodeWithText("看看以前的讨论").performClick()
            waitText("当时的想法");compose.onNodeWithText("继续这场讨论").performClick()
            waitText("那场讨论之后");compose.onNodeWithTag("meeting_thought").assertIsDisplayed()
        } finally {scenario.close();runBlocking {graph.preferences.reset()}}
    }
    @Test fun realProtocolRoundAndNeutralSummaryPersistWithoutAddingSpeeches() {
        runBlocking {graph.repository.clearUserData();graph.preferences.reset();graph.preferences.completeOnboarding()}
        FakeModelService().use {service->
            runBlocking {graph.apiKeys.save(service.baseUrl,"test-meeting-key");graph.preferences.setDirectModel(service.baseUrl,"test-model","qwen")}
            val scenario=ActivityScenario.launch(MainActivity::class.java)
            try {
                waitText("今天脑子");compose.onNodeWithTag("tab_debate").performClick()
                compose.onNodeWithTag("debate_list").performScrollToNode(hasTestTag("topic_input"));compose.onNodeWithTag("topic_input").performTextInput("努力需要终点吗？")
                Espresso.closeSoftKeyboard();click("debate_list","start_meeting")
                compose.waitUntil(15000) {runBlocking {graph.repository.conversations.first().isNotEmpty()}}
                val first=runBlocking {graph.repository.conversations.first().first().messages.first()}
                compose.onNodeWithText(first.content).performClick()
                compose.onNodeWithTag("reply_explain").performClick();compose.onNodeWithTag("chat_send").performClick()
                compose.waitUntil(15000) {runBlocking {graph.repository.conversations.first().first().messages.size==3}}
                val sent=Json.parseToJsonElement(service.requests.last().json["messages"]!!.jsonArray[1].jsonObject["content"]!!.jsonPrimitive.content).jsonObject["discussion"]!!.jsonObject
                assertEquals(first.id,sent["replyTo"]!!.jsonPrimitive.content)
                assertTrue(sent["speakerIds"]!!.jsonArray.size<=3)
                click("debate_list","summarize_meeting")
                compose.waitUntil(15000) {runBlocking {graph.repository.conversations.first().first().insights.size==1}}
                val loaded=runBlocking {graph.repository.conversations.first().first()}
                assertEquals(3,loaded.messages.size);assertEquals(first.id,loaded.insights.single().summary.disagreement.single().messageIds.single())
                assertEquals(3,service.requests.size)
                compose.onNodeWithTag("debate_list").performScrollToNode(hasText("分歧小结"));waitText("没有足够依据确认共同点");screenshot("meeting-live-summary")
                compose.onNodeWithTag("followup_input").performTextInput("这个尺度也会随生活变化吗？")
                Espresso.closeSoftKeyboard();compose.onNodeWithTag("chat_send").performClick()
                compose.waitUntil(15000) {runBlocking {graph.repository.conversations.first().first().insights.size==2}}
                val afterThirdRound=runBlocking {graph.repository.conversations.first().first()}
                assertEquals(5,afterThirdRound.messages.size);assertEquals(5,afterThirdRound.insights.last().messageCount)
                assertEquals(4,service.requests.size)
                compose.onNodeWithTag("meeting_menu").performClick();compose.onNodeWithText("再开一桌").performClick();waitText("丢个问题")
                compose.onNodeWithTag("reply_target").assertDoesNotExist()
            } finally {scenario.close();runBlocking {graph.preferences.reset()}}
        }
    }
}
