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
import com.example.data.TranslationRecordEntity
import com.example.data.UserPreferences
import com.example.model.TargetLanguage
import com.example.model.TranslationCoachResult
import com.example.model.TranslationTone
import com.example.ui.theme.GirmaicologyTheme
import com.example.ui.viewmodel.BridgeTranslationUiState
import com.example.ui.viewmodel.DrillTopics
import com.example.ui.viewmodel.SkillDrillUiState
import com.example.ui.viewmodel.VaultFilterMode
import com.example.ui.viewmodel.WritingPolishUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BridgeTranslatorUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `bridge supports writing searching uploading saving editing and deleting translations`() {
        var inputText by mutableStateOf("")
        var writingDraft by mutableStateOf("")
        var vaultQuery by mutableStateOf("")
        var records by mutableStateOf(
            listOf(
                TranslationRecordEntity(
                    id = 10L,
                    sourceText = "Coffee please",
                    translatedText = "ቡና እባክዎ",
                    sourceLanguageCode = "en",
                    sourceLanguageName = "English",
                    targetLanguageCode = "am",
                    targetLanguageName = "Amharic",
                    romanization = "Buna ibakwo",
                    entryType = "TEXT",
                    isStarred = true
                )
            )
        )
        var phraseSaved = false

        val readyState = BridgeTranslationUiState.Ready(
            input = "Good morning",
            targetLanguage = TargetLanguage.AMHARIC,
            tone = TranslationTone.FORMAL,
            result = TranslationCoachResult(
                detectedSourceLanguage = "English",
                translatedText = "እንደምን አደሩ",
                romanization = "Indemin aderu",
                partOfSpeech = "Greeting",
                alternativeTranslations = listOf("ሰላም"),
                phoneticGuide = "in-de-min a-de-ru"
            ),
            recordId = 1L,
            isStarred = false,
            isSavedToPhrasebook = false
        )

        composeRule.setContent {
            GirmaicologyTheme {
                BridgeScreen(
                    preferences = UserPreferences(targetLanguage = TargetLanguage.AMHARIC),
                    sourceLanguage = null,
                    selectedTone = TranslationTone.FORMAL,
                    inputText = inputText,
                    translationState = readyState,
                    writingDraft = writingDraft,
                    writingState = WritingPolishUiState.Idle,
                    savedTranslations = records.filter {
                        vaultQuery.isBlank() || it.translatedText.contains(vaultQuery) || it.sourceText.contains(vaultQuery, true)
                    },
                    allTranslationsCount = records.size,
                    vaultSearchQuery = vaultQuery,
                    vaultFilterMode = VaultFilterMode.ALL,
                    vaultLanguageFilter = null,
                    selectedTopic = DrillTopics.first(),
                    drillState = SkillDrillUiState.Idle,
                    onSourceLanguageChange = {},
                    onTargetLanguageChange = {},
                    onSelectTone = {},
                    onSwapLanguages = {},
                    onInputTextChange = { inputText = it },
                    onTranslate = {},
                    onUploadImageUri = {},
                    onUploadDocumentUri = {},
                    onToggleStarCurrent = {},
                    onEditCurrentTranslation = { _, _ -> },
                    onSaveTranslation = { phraseSaved = true },
                    onWritingDraftChange = { writingDraft = it },
                    onPolishWriting = {},
                    onSaveWritingToVault = {},
                    onSaveWritingToPhrasebook = {},
                    onVaultSearchQueryChange = { vaultQuery = it },
                    onVaultFilterModeChange = {},
                    onVaultLanguageFilterChange = {},
                    onClearVaultFilters = { vaultQuery = "" },
                    onToggleStarRecord = {},
                    onUpdateSavedRecord = { rec, src, tr, rom, note ->
                        records = records.map {
                            if (it.id == rec.id) it.copy(sourceText = src, translatedText = tr, romanization = rom, note = note)
                            else it
                        }
                    },
                    onDeleteSavedRecord = { id -> records = records.filterNot { it.id == id } },
                    onSaveRecordToPhrasebook = {},
                    onLoadRecordIntoTranslator = {},
                    onSpeakText = { _, _, _ -> },
                    onShowMessage = {},
                    onSelectTopic = {},
                    onGenerateDrill = {},
                    onSelectAnswer = { _, _ -> },
                    onSaveDrillExpression = { _, _, _ -> },
                    onPracticeInLiveVoice = {}
                )
            }
        }

        // Verify upload buttons & translation result card controls are accessible
        composeRule.onNodeWithTag("bridge_lazy_list")
            .performScrollToNode(hasTestTag("bridge_upload_photo_button"))
        composeRule.onNodeWithTag("bridge_upload_photo_button").assertIsDisplayed()
        composeRule.onNodeWithTag("bridge_upload_file_button").assertIsDisplayed()

        composeRule.onNodeWithTag("bridge_lazy_list")
            .performScrollToNode(hasTestTag("bridge_save_phrase_button"))
        composeRule.onNodeWithTag("bridge_copy_button").assertIsDisplayed()
        composeRule.onNodeWithTag("bridge_share_button").assertIsDisplayed()
        composeRule.onNodeWithTag("bridge_edit_button").assertIsDisplayed()
        composeRule.onNodeWithTag("bridge_save_phrase_button").performClick()
        assertTrue(phraseSaved)

        // Switch to Writing tab and verify composer
        composeRule.onNodeWithTag("bridge_lazy_list").performScrollToIndex(0)
        composeRule.onNodeWithTag("bridge_writing_tab").performClick()
        composeRule.onNodeWithTag("bridge_lazy_list")
            .performScrollToNode(hasTestTag("bridge_writing_input"))
        composeRule.onNodeWithTag("bridge_writing_input").performTextInput("Selam")
        assertEquals("Selam", writingDraft)

        // Switch to Saved Vault tab and verify search, edit, and delete
        composeRule.onNodeWithTag("bridge_lazy_list").performScrollToIndex(0)
        composeRule.onNodeWithTag("bridge_saved_tab").performClick()
        composeRule.onNodeWithTag("vault_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("bridge_lazy_list")
            .performScrollToNode(hasTestTag("vault_card_10"))
        composeRule.onNodeWithText("ቡና እባክዎ").assertIsDisplayed()

        // Edit record
        composeRule.onNodeWithTag("vault_edit_10").performClick()
        composeRule.onNodeWithTag("edit_vault_note").performTextInput("Ordered at cafe")
        composeRule.onNodeWithTag("confirm_edit_vault").performClick()
        assertEquals("Ordered at cafe", records.single().note)

        // Delete record
        composeRule.onNodeWithTag("vault_delete_10").performClick()
        assertTrue(records.isEmpty())
    }
}
