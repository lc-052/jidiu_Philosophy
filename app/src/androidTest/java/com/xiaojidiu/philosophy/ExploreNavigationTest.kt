package com.xiaojidiu.philosophy

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.explore.ExploreScreen
import com.xiaojidiu.philosophy.ui.theme.LittleThoughtsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import androidx.test.espresso.Espresso

class ExploreNavigationTest {
    @get:Rule val compose=createComposeRule()
    private fun question(id: String,title: String,category: String)=Question(
        id=id,title=title,scenario="面对$title，您会如何选择？",category=category,
        background="选择之后才展开的背景",sourceType="原创情境",source="编辑情境",
        options=listOf(Choice("a","愿意",0,emptyMap(),emptyList()),Choice("b","不愿意",1,emptyMap(),emptyList())),
        perspectives=emptyList(),philosopherIds=emptyList(),schoolIds=emptyList()
    )
    @Test fun categoryAndSearchOpenTheCorrectQuestion() {
        var opened=""
        compose.setContent {
            LittleThoughtsTheme {
                ExploreScreen(Library(questions=listOf(question("ethics","帮助陌生人","伦理"),question("happy","享受周末","幸福"))),{opened=it},{},{},{})
            }
        }
        compose.onNodeWithText("伦理").performClick()
        compose.onNodeWithText("帮助陌生人").assertIsDisplayed()
        compose.onNodeWithText("享受周末").assertDoesNotExist()
        compose.onNodeWithText("全部").performClick()
        compose.onNodeWithTag("explore_search").performTextInput("周末")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("帮助陌生人").assertDoesNotExist()
        compose.onNodeWithTag("explore_list").performScrollToNode(hasText("享受周末"))
        compose.onNodeWithText("享受周末").performClick()
        compose.runOnIdle {assertEquals("happy",opened)}
        compose.onNodeWithContentDescription("清空搜索").performClick()
        compose.onNodeWithText("帮助陌生人").assertExists()
    }
}
