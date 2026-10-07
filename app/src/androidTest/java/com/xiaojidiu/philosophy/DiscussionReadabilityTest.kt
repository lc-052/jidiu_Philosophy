package com.xiaojidiu.philosophy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.xiaojidiu.philosophy.data.local.SettingsState
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.debate.RoundTableMessageBubble
import com.xiaojidiu.philosophy.ui.theme.LittleThoughtsTheme
import com.xiaojidiu.philosophy.util.DiscussionParagraphs
import org.junit.Rule
import org.junit.Test

class DiscussionReadabilityTest {
    @get:Rule val compose=createComposeRule()
    @Test fun longMessageRemainsReadableOnSmallScreenWithLargeFont() {
        val content="人想为生活找到意义，世界却未必给出我们期待的回答。看见这种分歧，并不要求我们放弃生活。"+
            "如果每件事都要等到一个最终答案才开始，我们可能永远无法迈出下一步。"+
            "可以先看清自己正在做什么，承认问题仍然存在，再选择今天愿意承担的事情。"+
            "这种解释是一种思想推演，不是哲学家的原话，也不是对您生活的判断。"
        val message=DebateMessage("long","camus","加缪",content,null,"perspective")
        val conversation=Conversation("reading","努力一定需要终点吗？",listOf("camus"),0,true,listOf(message))
        compose.setContent {
            val density=LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density,1.8f)) {
                LittleThoughtsTheme("dark") {
                    Surface(Modifier.width(320.dp).height(480.dp)) {
                        LazyColumn(Modifier.testTag("reading_list"),contentPadding=PaddingValues(16.dp)) {
                            item {RoundTableMessageBubble(message,conversation,SettingsState())}
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("加缪").assertIsDisplayed()
        val paragraphs=DiscussionParagraphs.split(content)
        paragraphs.forEach {paragraph->
            compose.onNodeWithText(paragraph).performScrollTo().assertIsDisplayed()
        }
    }
}
