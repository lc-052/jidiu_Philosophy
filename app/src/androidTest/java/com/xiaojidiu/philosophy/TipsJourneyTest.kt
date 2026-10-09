package com.xiaojidiu.philosophy

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class TipsJourneyTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val app get()=ApplicationProvider.getApplicationContext<ThoughtsApplication>()
    private val graph get()=app.graph
    private fun waitText(text: String)=compose.waitUntil(15000) {compose.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}
    private fun prepare()=runBlocking {graph.preferences.completeOnboarding();graph.preferences.setTheme("light");graph.database.tipUnderstandingDao().clear()}
    private fun enter() {
        waitText("今天脑子");compose.onNodeWithTag("tab_explore").performClick()
        compose.onNodeWithTag("explore_list").performScrollToNode(hasText("TIPS · 哲学词语"));compose.onNodeWithText("TIPS · 哲学词语").performClick()
        compose.waitUntil(15000) {compose.onAllNodesWithTag("tip_metaphysics").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun note() {compose.onNodeWithTag("tip_detail_list").performScrollToNode(hasTestTag("tip_understanding"))}
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val file=File(app.getExternalFilesDir(null),"screenshots/$name.png").apply {parentFile!!.mkdirs()}
        file.outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("mkdir -p /sdcard/Download/little-thoughts-screenshots").use {FileInputStream(it.fileDescriptor).readBytes()}
        automation.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/little-thoughts-screenshots/$name.png").use {FileInputStream(it.fileDescriptor).readBytes()}
    }
    @Test fun searchWriteRotateSaveReopenAndRelatedContents() {
        prepare();val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            enter();screenshot("tips-list")
            compose.onNodeWithTag("tips_search").performTextInput("意志自由");Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("tip_free-will").assertExists().performClick();waitText("带着这些问题看");screenshot("tips-detail")
            compose.onNodeWithText("标准释义").assertDoesNotExist()
            compose.onNodeWithTag("tip_write_jump").performClick();note();val text="我把自由意志理解成：即使受到影响，也要认真承担自己的选择。"
            compose.onNodeWithTag("tip_understanding").performTextInput(text);Espresso.closeSoftKeyboard()
            scenario.recreate();waitText("自由意志");note();compose.onNodeWithTag("tip_understanding").assertTextContains(text)
            compose.onNodeWithContentDescription("返回").performClick();waitText("还有没存下的理解")
            compose.onNodeWithText("继续写").performClick()
            compose.onNodeWithTag("tip_save").performClick();waitText("理解存好了")
            assertEquals(text,runBlocking {graph.tips.observe("free-will").first()!!.content});screenshot("tips-understanding")
            compose.onNodeWithContentDescription("返回").performClick();waitText("给词语留一点想法")
            compose.onNodeWithTag("tips_written").performClick();compose.onNodeWithTag("tips_search").performTextReplacement("认真承担");Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("tip_free-will").assertExists().performClick();waitText("带着这些问题看");note()
            compose.onNodeWithTag("tip_understanding").assertTextContains(text)
            compose.onNodeWithTag("tip_detail_list").performScrollToNode(hasTestTag("related_tip_compatibilism"));compose.onNodeWithTag("related_tip_compatibilism").performClick();waitText("Compatibilism")
            compose.onNodeWithTag("tip_detail_list").performScrollToNode(hasText("看看他主要在想什么"));compose.onAllNodesWithText("休谟").onLast().performClick();compose.waitUntil(15000) {compose.onAllNodesWithTag("philosopher_list").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithTag("philosopher_list").performScrollToNode(hasText("代表著作"))
            compose.onNodeWithContentDescription("返回").performClick();waitText("相容论")
            val q=runBlocking {graph.repository.questions.first().first {it.id=="q20"}}
            compose.onNodeWithTag("tip_detail_list").performScrollToNode(hasText(q.title));compose.onNodeWithText(q.title).performClick();waitText(q.scenario)
        } finally {scenario.close();runBlocking {graph.database.tipUnderstandingDao().clear()}}
    }
    @Test fun deleteNeedsConfirmationAndSavingOnReturnKeepsNewUnderstanding() {
        prepare();runBlocking {graph.tips.save("metaphysics","我之前写下的理解")}
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            enter();compose.onNodeWithTag("tip_metaphysics").performClick();waitText("带着这些问题看");note()
            compose.onNodeWithTag("tip_understanding").assertTextContains("我之前写下的理解")
            compose.onNodeWithTag("tip_detail_list").performScrollToNode(hasTestTag("tip_remove"));compose.onNodeWithTag("tip_remove").performClick();waitText("清除这条理解？")
            compose.onNodeWithText("留着吧").performClick();assertNotNull(runBlocking {graph.tips.observe("metaphysics").first()})
            compose.onNodeWithTag("tip_remove").performClick();compose.onNodeWithText("确认清除").performClick();waitText("这条理解已清除")
            assertNull(runBlocking {graph.tips.observe("metaphysics").first()})
            note();compose.onNodeWithTag("tip_understanding").performTextInput("这一次我会从存在的问题开始想")
            Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("返回").performClick();waitText("还有没存下的理解")
            compose.onNodeWithText("先保存，再返回").performClick();waitText("给词语留一点想法")
            assertEquals("这一次我会从存在的问题开始想",runBlocking {graph.tips.observe("metaphysics").first()!!.content})
            compose.onNodeWithTag("tip_metaphysics").performClick();waitText("带着这些问题看")
            runBlocking {graph.preferences.setTheme("dark")};scenario.recreate();waitText("形而上学");note();screenshot("tips-dark")
        } finally {scenario.close();runBlocking {graph.database.tipUnderstandingDao().clear();graph.preferences.setTheme("light")}}
    }
}
