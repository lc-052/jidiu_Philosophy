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
import java.io.File
import java.io.FileInputStream
import org.junit.Rule
import org.junit.Test

class GamePhilosophyJourneyTest {
    @get:Rule val compose=createEmptyComposeRule()
    private fun waitText(text: String) {compose.waitUntil(15000) {compose.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}}
    private fun click(list: String,text: String) {compose.onNodeWithTag(list).performScrollToNode(hasText(text));compose.onNodeWithText(text).performScrollTo().performClick()}
    private fun back() {compose.onNodeWithContentDescription("返回").performClick()}
    private fun screenshot(name: String) {
        val ctx=ApplicationProvider.getApplicationContext<android.content.Context>()
        val image=File(ctx.getExternalFilesDir(null),"screenshots/$name.png").apply {parentFile!!.mkdirs()}
        image.outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("mkdir -p /sdcard/Download/little-thoughts-screenshots").use {FileInputStream(it.fileDescriptor).readBytes()}
        automation.executeShellCommand("cp ${image.absolutePath} /sdcard/Download/little-thoughts-screenshots/$name.png").use {FileInputStream(it.fileDescriptor).readBytes()}
    }
    @Test fun worksSearchSpoilerRestorationAndPhilosophyNavigation() {
        val graph=ApplicationProvider.getApplicationContext<ThoughtsApplication>().graph
        runBlocking {graph.preferences.reset();graph.preferences.completeOnboarding()}
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            waitText("今天脑子");compose.onNodeWithTag("tab_explore").performClick()
            click("explore_list","游戏中的哲学");waitText("通关之后")
            screenshot("game-works")
            compose.onNodeWithTag("games_search").performTextInput("美好的每一天");Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("game_sakura").assertDoesNotExist()
            compose.onNodeWithTag("games_list").performScrollToNode(hasTestTag("game_subahibi"));compose.onNodeWithTag("game_subahibi").performClick()
            waitText("先说明阅读边界");screenshot("game-subahibi")
            compose.onNodeWithTag("game_detail_list").performScrollToNode(hasTestTag("spoiler_happiness"))
            compose.onNodeWithTag("topic_context_happiness").assertDoesNotExist()
            compose.onNodeWithTag("game_detail_list").performScrollToNode(hasTestTag("entry_happiness_0"));compose.onNodeWithTag("entry_happiness_0").performScrollTo().performClick()
            compose.onNodeWithTag("spoiler_happiness").performScrollTo().performClick()
            compose.onNodeWithTag("topic_context_happiness").assertExists()
            scenario.recreate();waitText("素晴日")
            compose.onNodeWithTag("game_detail_list").performScrollToNode(hasTestTag("topic_happiness"));compose.onNodeWithTag("topic_context_happiness").assertExists()
            compose.onNodeWithTag("game_detail_list").performScrollToNode(hasText("认识一下维特根斯坦") and hasAnyAncestor(hasTestTag("topic_happiness")));compose.onNode(hasText("认识一下维特根斯坦") and hasAnyAncestor(hasTestTag("topic_happiness"))).performScrollTo().performClick();waitText("他主要在想什么？");back()
            click("game_detail_list","自己选一次：快乐的误会");waitText("先选，再想");back();back()
            compose.onNodeWithTag("games_list").performScrollToNode(hasTestTag("games_search"));compose.onNodeWithContentDescription("清空作品搜索").performClick()
            compose.onNodeWithTag("games_list").performScrollToNode(hasTestTag("game_sakura"));compose.onNodeWithTag("game_sakura").performClick();waitText("先说明阅读边界")
            compose.onNodeWithTag("game_detail_list").performScrollToNode(hasText("我觉得美，能要求您也觉得吗？"));screenshot("game-sakura")
            compose.onNodeWithTag("game_detail_list").performScrollToNode(hasTestTag("entry_beauty_0"));compose.onNodeWithTag("entry_beauty_0").performScrollTo().performClick()
            click("game_detail_list","认识一下康德");waitText("他主要在想什么？")
        } finally {scenario.close();runBlocking {graph.preferences.reset()}}
    }
}


