package com.nantcompany.clipy.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nantcompany.clipy.home.pages.StudioPage
import com.nantcompany.clipy.home.pages.LibraryPage
import com.nantcompany.clipy.home.pages.ToolsPage
import com.nantcompany.clipy.home.pages.SettingsPage
import com.nantcompany.clipy.theme.ClipyTheme
import org.junit.Rule
import org.junit.Test

class HomeTabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun studioTab_shouldBeDisplayed() {
        composeTestRule.setContent {
            ClipyTheme {
                StudioPage(onNavigate = {}, onToolSelected = { _, _ -> })
            }
        }

        composeTestRule.onNodeWithText("Quick Tools").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clipy").assertIsDisplayed()
    }

    @Test
    fun libraryTab_shouldShowEmptyState_whenNoData() {
        composeTestRule.setContent {
            ClipyTheme {
                LibraryPage(recentExports = emptyList(), onRecentClick = {}, onNavigate = {})
            }
        }

        composeTestRule.onNodeWithText("Your Library is Empty").assertIsDisplayed()
    }
}
