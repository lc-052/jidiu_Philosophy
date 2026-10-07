package com.xiaojidiu.philosophy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.profile.ProfileScreen
import com.xiaojidiu.philosophy.ui.theme.LittleThoughtsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class AccessibilityLayoutTest {
    @get:Rule val compose=createComposeRule()
    @Test fun smallScreenLargeFontsKeepActionsReachable() {
        var opened="";var history=false
        compose.setContent {
            val density=LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density,1.8f)) {
                LittleThoughtsTheme {
                    Surface(Modifier.width(320.dp).height(640.dp)) {
                        ProfileScreen(Library(),Journal(),{opened=it},{history=true},{},{},{})
                    }
                }
            }
        }
        compose.onNodeWithTag("profile_list").performScrollToNode(hasContentDescription("存在主义，尚未探索"))
        compose.onNodeWithContentDescription("存在主义，尚未探索").performClick()
        compose.runOnIdle {assertEquals("Existentialism",opened)}
        compose.onNodeWithTag("profile_list").performScrollToNode(hasText("展开全部思想维度"))
        compose.onNodeWithText("展开全部思想维度").performClick()
        compose.onNodeWithTag("profile_list").performScrollToNode(hasContentDescription("怀疑主义，尚未探索"))
        compose.onNodeWithContentDescription("怀疑主义，尚未探索").performClick()
        compose.runOnIdle {assertEquals("Skepticism",opened)}
        compose.onNodeWithTag("profile_list").performScrollToNode(hasText("以前都想了些什么"))
        compose.onNodeWithText("以前都想了些什么").assertIsDisplayed().performClick()
        compose.runOnIdle {assertTrue(history)}
    }
}
