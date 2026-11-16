package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.model.PresetScenarios
import com.example.model.ProficiencyLevel
import com.example.model.TargetLanguage
import com.example.ui.theme.GirmaicologyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LanguageSelectionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `user can switch Ethiopian African Global categories select language and start live session`() {
        var selectedLanguage by mutableStateOf(TargetLanguage.SPANISH)
        var startedLive = false

        composeRule.setContent {
            GirmaicologyTheme {
                LanguageSelectionScreen(
                    selectedLanguage = selectedLanguage,
                    selectedScenario = PresetScenarios.all.first(),
                    proficiencyLevel = ProficiencyLevel.BEGINNER,
                    hasAudioPermission = { true },
                    onSelectLanguage = { selectedLanguage = it },
                    onStartLiveSession = { startedLive = true },
                    onBack = {}
                )
            }
        }

        // Switch to Ethiopian category, scroll to Amharic card, and select it
        composeRule.onNodeWithTag("region_card_ETHIOPIAN").performClick()
        composeRule.onNodeWithTag("language_selection_list")
            .performScrollToNode(hasTestTag("select_language_am"))
        composeRule.onNodeWithTag("select_language_am").assertIsDisplayed().performClick()
        assertEquals(TargetLanguage.AMHARIC, selectedLanguage)

        // Scroll back to top, switch to African category, scroll to Swahili card, and select it
        composeRule.onNodeWithTag("language_selection_list").performScrollToIndex(0)
        composeRule.onNodeWithTag("region_card_AFRICAN").performClick()
        composeRule.onNodeWithTag("language_selection_list")
            .performScrollToNode(hasTestTag("select_language_sw"))
        composeRule.onNodeWithTag("select_language_sw").assertIsDisplayed().performClick()
        assertEquals(TargetLanguage.SWAHILI, selectedLanguage)

        // Scroll back to top, switch to Global category, search for Hindi, select it, and start Gemini Live
        composeRule.onNodeWithTag("language_selection_list").performScrollToIndex(0)
        composeRule.onNodeWithTag("region_card_GLOBAL").performClick()
        composeRule.onNodeWithTag("language_selection_list")
            .performScrollToNode(hasTestTag("language_selection_search"))
        composeRule.onNodeWithTag("language_selection_search").performTextInput("Hindi")
        composeRule.onNodeWithTag("language_selection_list")
            .performScrollToNode(hasTestTag("select_language_hi"))
        composeRule.onNodeWithTag("select_language_hi").assertIsDisplayed().performClick()
        assertEquals(TargetLanguage.HINDI, selectedLanguage)

        composeRule.onNodeWithText("Start Gemini Live · Hindi").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_start_live_button").performClick()
        assertTrue(startedLive)
    }
}
