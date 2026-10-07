package com.xiaojidiu.philosophy

import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream

/** 此测试重置测试设备上该 App 的用户数据，只应在专用测试设备运行。 */
class ProductJourneyTest {
    @get:Rule val compose=createEmptyComposeRule()
    private fun waitText(text: String) {compose.waitUntil(15_000) {compose.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}}
    private fun clickIn(list: String,text: String) {compose.onNodeWithTag(list).performScrollToNode(hasText(text));compose.onNodeWithText(text).performClick()}
    private fun back() {compose.onNodeWithContentDescription("返回").performClick()}
    private fun screenshot(name: String) {
        val ctx=ApplicationProvider.getApplicationContext<android.content.Context>()
        val folder=File(ctx.getExternalFilesDir(null),"screenshots").apply {mkdirs()}
        File(folder,"$name.png").outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("mkdir -p /sdcard/Download/little-thoughts-screenshots").use {FileInputStream(it.fileDescriptor).readBytes()}
        automation.executeShellCommand("cp ${File(folder,"$name.png").absolutePath} /sdcard/Download/little-thoughts-screenshots/$name.png").use {FileInputStream(it.fileDescriptor).readBytes()}
    }
    @Test fun completeProductLoopAndRotation() {
        val app=ApplicationProvider.getApplicationContext<ThoughtsApplication>()
        runBlocking {app.graph.repository.clearUserData();app.graph.preferences.reset()}
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            waitText("这里没有标准答案");screenshot("onboarding")
            compose.onNodeWithText("继续看看").performClick();waitText("先选，再想")
            compose.onNodeWithText("继续看看").performClick();waitText("看看自己的脑回路")
            compose.onNodeWithText("开始胡思乱想").performClick();waitText("今天脑子")
            val q=runBlocking {app.graph.repository.questionRepository.today(app.graph.repository.questions.first())!!}
            compose.onNodeWithText(q.background).assertDoesNotExist();screenshot("home")
            clickIn("home_list",q.options.first().text);waitText("这个念头")
            clickIn("result_list","先存着，以后再想")
            compose.onNodeWithTag("result_list").performScrollToNode(hasTestTag("reflection_input"))
            compose.onNodeWithTag("reflection_input").performTextInput("我看重实际行动，也想为未来留一点空间。")
            Espresso.closeSoftKeyboard();clickIn("result_list","存下这个念头");waitText("念头存好了")
            assertEquals(1,runBlocking {app.graph.repository.journal.first().answers.size})
            clickIn("result_list","看看我的脑回路");waitText("我的脑回路");screenshot("profile")
            clickIn("profile_list","编辑形象");waitText("给自己留个位置")
            compose.onNodeWithTag("nickname_input").performTextReplacement("想两下")
            Espresso.closeSoftKeyboard();compose.onNodeWithTag("identity_list").performScrollToNode(hasTestTag("avatar_cat"));compose.onNodeWithTag("avatar_cat").performClick()
            clickIn("identity_list","保存我的形象");waitText("昵称和头像已保存")
            scenario.recreate();waitText("我在圆桌上");compose.onNodeWithTag("identity_list").performScrollToNode(hasTestTag("nickname_input"));compose.onNodeWithTag("nickname_input").assertTextContains("想两下");compose.onNodeWithTag("identity_list").performScrollToNode(hasTestTag("avatar_cat"));compose.onNodeWithTag("avatar_cat").assertIsSelected();screenshot("user-identity");back()
            clickIn("profile_list","以前都想了些什么");waitText("以前都想了些什么")
            compose.onNodeWithText("打开这个念头").performClick();waitText("这个念头")
            clickIn("result_list","再想两下，重新选择");waitText("先选，再想")
            compose.onNodeWithText(q.options[1].text).performScrollTo().performClick();waitText("这个念头")
            compose.onNodeWithTag("result_list").performScrollToNode(hasText("您的答案变了。"));compose.onNodeWithText("您的答案变了。").assertIsDisplayed();screenshot("past-self")
            back();back();back();compose.onNodeWithTag("tab_explore").performClick();waitText("随便逛逛")
            screenshot("explore");compose.onNodeWithText("哲学家").performClick();compose.onNodeWithTag("explore_list").performScrollToNode(hasText("庄子"))
            compose.onNodeWithTag("invite_zhuangzi").performClick();waitText("他主要在想什么？")
            clickIn("philosopher_list","先存着，以后再想");clickIn("philosopher_list","去看看");waitText("道家思想")
            clickIn("school_list","先存着，以后再想");compose.onNodeWithTag("school_list").performScrollToNode(hasText("可能误解"));screenshot("school")
            back();clickIn("philosopher_list","继续探索思想关系");waitText("这些人到底什么关系？");screenshot("map");back();back()
            compose.onNodeWithTag("tab_debate").performClick();waitText("Demo Mode")
            screenshot("debate-setup");compose.onNodeWithTag("debate_list").performScrollToNode(hasTestTag("topic_input"));compose.onNodeWithTag("topic_input").performTextInput("人为什么一定要努力？");Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("debate_list").performScrollToNode(hasTestTag("philosopher_row"));compose.onNodeWithTag("philosopher_row").performScrollToNode(hasTestTag("invite_kant"));compose.onNodeWithTag("invite_kant").performClick()
            clickIn("debate_list","开始会议")
            compose.waitUntil(10_000) {runBlocking {app.graph.repository.conversations.first().isNotEmpty()}}
            compose.onNodeWithTag("debate_list").performScrollToNode(hasText("已自动保存到本地",substring=true));waitText("已自动保存到本地")
            compose.onNodeWithTag("followup_input").performTextInput("庄子怎么看康德刚才说的？");Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("chat_send").performClick()
            compose.waitUntil(10_000) {runBlocking {app.graph.repository.conversations.first().first().messages.size==5}}
            compose.onNodeWithTag("debate_list").performScrollToNode(hasText("您的追问已存下",substring=true));waitText("您的追问已存下")
            assertEquals("想两下",runBlocking {app.graph.repository.conversations.first().first().messages.last().displayName});compose.onNodeWithTag("chat_user_message").assertIsDisplayed();screenshot("group-chat");clickIn("debate_list","先存着，以后再想");screenshot("debate")
            clickIn("debate_list","看看以前的讨论");waitText("以前开过的桌");compose.onNodeWithText("继续这场讨论").performClick();waitText("圆桌讨论");compose.onNodeWithTag("debate_list").performScrollToNode(hasText("已自动保存到本地",substring=true));back();back()
            compose.onNodeWithTag("tab_profile").performClick();clickIn("profile_list","先存着，以后再想");waitText("继续想想")
            assertEquals(4,runBlocking {app.graph.repository.journal.first().favorites.size});screenshot("favorites");back()
            compose.onNodeWithTag("profile_list").performScrollToIndex(0);compose.onNodeWithText("设置").performClick();waitText("舒服地想")
            compose.onNodeWithText("深色").performClick();compose.waitUntil(5000) {runBlocking {app.graph.preferences.settings.first().theme=="dark"}}
            scenario.recreate();waitText("舒服地想");screenshot("settings-dark")
            clickIn("settings_list","配置模型与 API Key");waitText("连接您的大模型")
            compose.onNodeWithText("自有服务").performClick()
            compose.onNodeWithTag("model_settings_list").performScrollToNode(hasTestTag("backend_url"))
            compose.onNodeWithTag("backend_url").performTextReplacement("https://example.invalid/api/")
            Espresso.closeSoftKeyboard();clickIn("model_settings_list","保存配置")
            compose.waitUntil(5000) {runBlocking {app.graph.preferences.settings.first().aiBaseUrl=="https://example.invalid/api/"}}
            scenario.recreate();waitText("连接您的大模型")
            compose.onNodeWithTag("model_settings_list").performScrollToNode(hasTestTag("backend_url"))
            compose.onNodeWithTag("backend_url").assertTextContains("https://example.invalid/api/")
            clickIn("model_settings_list","使用演示模式")
            compose.waitUntil(5000) {runBlocking {app.graph.preferences.settings.first().aiMode=="demo"}}
            back();waitText("舒服地想");screenshot("settings-api")
            clickIn("settings_list","清除回答历史");waitText("此操作无法撤销");compose.onNodeWithText("留着吧").performClick()
            assertEquals(2,runBlocking {app.graph.repository.journal.first().answers.size})
            assertEquals(2,runBlocking {app.graph.repository.journal.first().snapshots.size})
            assertEquals(5,runBlocking {app.graph.repository.conversations.first().first().messages.size})
        } finally {scenario.close()}
    }
}





