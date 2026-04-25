package com.example.clipystudio.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.clipystudio.theme.MyApplicationTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.example.clipystudio.ui.main.MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent { MyApplicationTheme { MainScreen(onItemClick = {}) } }
  }

  @Test
  fun onboarding_existsOnFirstLaunch() {
    composeTestRule.onNodeWithText("Clipy Studio").assertExists()
  }
}
