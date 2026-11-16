package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.TranslationRecordEntity
import com.example.data.UserPreferences
import com.example.model.TargetLanguage
import com.example.model.TranslationTone
import com.example.ui.components.LanguagePickerDialog
import com.example.ui.util.ShareAndCopyHelper
import com.example.ui.viewmodel.BridgeTranslationUiState
import com.example.ui.viewmodel.SkillDrillUiState
import com.example.ui.viewmodel.VaultFilterMode
import com.example.ui.viewmodel.WritingPolishUiState

private enum class BridgeWorkspaceTab(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    TRANSLATE("Translate", Icons.Filled.Translate, "bridge_translator_tab"),
    WRITE("Write", Icons.Filled.EditNote, "bridge_writing_tab"),
    VAULT("Saved", Icons.Filled.Bookmarks, "bridge_saved_tab"),
    DRILLS("Drills", Icons.Filled.Quiz, "bridge_drills_tab")
}

@Composable
fun BridgeScreen(
    preferences: UserPreferences,
    sourceLanguage: TargetLanguage?,
    selectedTone: TranslationTone,
    inputText: String,
    translationState: BridgeTranslationUiState,
    writingDraft: String,
    writingState: WritingPolishUiState,
    savedTranslations: List<TranslationRecordEntity>,
    allTranslationsCount: Int,
    vaultSearchQuery: String,
    vaultFilterMode: VaultFilterMode,
    vaultLanguageFilter: String?,
    selectedTopic: String,
    drillState: SkillDrillUiState,
    onSourceLanguageChange: (TargetLanguage?) -> Unit,
    onTargetLanguageChange: (TargetLanguage) -> Unit,
    onSelectTone: (TranslationTone) -> Unit,
    onSwapLanguages: (TargetLanguage) -> Unit,
    onInputTextChange: (String) -> Unit,
    onTranslate: () -> Unit,
    onUploadImageUri: (Uri) -> Unit,
    onUploadDocumentUri: (Uri) -> Unit,
    onToggleStarCurrent: () -> Unit,
    onEditCurrentTranslation: (String, String) -> Unit,
    onSaveTranslation: () -> Unit,
    onWritingDraftChange: (String) -> Unit,
    onPolishWriting: () -> Unit,
    onSaveWritingToVault: () -> Unit,
    onSaveWritingToPhrasebook: () -> Unit,
    onVaultSearchQueryChange: (String) -> Unit,
    onVaultFilterModeChange: (VaultFilterMode) -> Unit,
    onVaultLanguageFilterChange: (String?) -> Unit,
    onClearVaultFilters: () -> Unit,
    onToggleStarRecord: (TranslationRecordEntity) -> Unit,
    onUpdateSavedRecord: (TranslationRecordEntity, String, String, String, String) -> Unit,
    onDeleteSavedRecord: (Long) -> Unit,
    onSaveRecordToPhrasebook: (TranslationRecordEntity) -> Unit,
    onLoadRecordIntoTranslator: (TranslationRecordEntity) -> Unit,
    onSpeakText: (String, TargetLanguage, Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    onSelectTopic: (String) -> Unit,
    onGenerateDrill: () -> Unit,
    onSelectAnswer: (Int, Int) -> Unit,
    onSaveDrillExpression: (String, String, String) -> Unit,
    onPracticeInLiveVoice: (TargetLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(BridgeWorkspaceTab.TRANSLATE) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }
    var showEditCurrentDialog by remember { mutableStateOf(false) }
    var editingVaultRecord by remember { mutableStateOf<TranslationRecordEntity?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUploadImageUri(uri)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onUploadDocumentUri(uri)
        }
    }

    val availableVaultLanguages = remember(savedTranslations) {
        savedTranslations
            .map { it.targetLanguageCode to it.targetLanguageName }
            .distinctBy { it.first }
            .sortedBy { it.second }
    }

    val handleCopy: (String) -> Unit = { text ->
        if (ShareAndCopyHelper.copyToClipboard(context, "Girmaicology Translation", text)) {
            onShowMessage("Copied to clipboard")
        }
    }

    val handleShare: (String, String) -> Unit = { subject, body ->
        ShareAndCopyHelper.shareText(context, subject, body)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 760.dp)
            .testTag("bridge_lazy_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Global Language Bridge",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Translate text, photos & files, write & proofread, save, edit, search & share across all 55 languages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BridgeWorkspaceTab.entries) { tab ->
                        val labelText = if (tab == BridgeWorkspaceTab.VAULT && allTranslationsCount > 0) {
                            "${tab.label} ($allTranslationsCount)"
                        } else {
                            tab.label
                        }
                        FilterChip(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            leadingIcon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text(labelText) },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }

        when (activeTab) {
            BridgeWorkspaceTab.TRANSLATE -> {
                bridgeTranslatorItems(
                    preferences = preferences,
                    sourceLanguage = sourceLanguage,
                    selectedTone = selectedTone,
                    inputText = inputText,
                    translationState = translationState,
                    onOpenSourcePicker = { showSourcePicker = true },
                    onOpenTargetPicker = { showTargetPicker = true },
                    onSwapLanguages = onSwapLanguages,
                    onSelectTone = onSelectTone,
                    onInputTextChange = onInputTextChange,
                    onUploadPhotoClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onUploadDocumentClick = {
                        documentPickerLauncher.launch(
                            arrayOf("text/plain", "text/markdown", "text/csv", "application/pdf", "text/*")
                        )
                    },
                    onTranslate = onTranslate,
                    onToggleStarCurrent = onToggleStarCurrent,
                    onOpenEditCurrent = { showEditCurrentDialog = true },
                    onSaveToPhrasebook = onSaveTranslation,
                    onSpeakText = onSpeakText,
                    onCopyText = handleCopy,
                    onShareText = handleShare,
                    onPracticeInLiveVoice = onPracticeInLiveVoice
                )
            }

            BridgeWorkspaceTab.WRITE -> {
                bridgeWritingItems(
                    preferences = preferences,
                    selectedTone = selectedTone,
                    writingDraft = writingDraft,
                    writingState = writingState,
                    onOpenTargetPicker = { showTargetPicker = true },
                    onSelectTone = onSelectTone,
                    onWritingDraftChange = onWritingDraftChange,
                    onPolishWriting = onPolishWriting,
                    onSaveToVault = onSaveWritingToVault,
                    onSaveToPhrasebook = onSaveWritingToPhrasebook,
                    onSpeakText = onSpeakText,
                    onCopyText = handleCopy,
                    onShareText = handleShare
                )
            }

            BridgeWorkspaceTab.VAULT -> {
                bridgeVaultItems(
                    records = savedTranslations,
                    allRecordsCount = allTranslationsCount,
                    availableTargetLanguages = availableVaultLanguages,
                    searchQuery = vaultSearchQuery,
                    filterMode = vaultFilterMode,
                    languageFilter = vaultLanguageFilter,
                    onSearchQueryChange = onVaultSearchQueryChange,
                    onFilterModeChange = onVaultFilterModeChange,
                    onLanguageFilterChange = onVaultLanguageFilterChange,
                    onClearFilters = onClearVaultFilters,
                    onToggleStar = onToggleStarRecord,
                    onOpenEditDialog = { editingVaultRecord = it },
                    onDeleteRecord = onDeleteSavedRecord,
                    onSaveToPhrasebook = onSaveRecordToPhrasebook,
                    onLoadIntoTranslator = { rec ->
                        onLoadRecordIntoTranslator(rec)
                        activeTab = BridgeWorkspaceTab.TRANSLATE
                    },
                    onSpeakText = onSpeakText,
                    onCopyText = handleCopy,
                    onShareText = handleShare
                )
            }

            BridgeWorkspaceTab.DRILLS -> {
                bridgeDrillItems(
                    preferences = preferences,
                    selectedTopic = selectedTopic,
                    drillState = drillState,
                    onOpenTargetPicker = { showTargetPicker = true },
                    onSelectTopic = onSelectTopic,
                    onGenerateDrill = onGenerateDrill,
                    onSelectAnswer = onSelectAnswer,
                    onSaveDrillExpression = onSaveDrillExpression,
                    onSpeakText = onSpeakText
                )
            }
        }
    }

    if (showSourcePicker) {
        LanguagePickerDialog(
            title = "Translate From",
            selectedLanguage = sourceLanguage,
            allowAutoDetect = true,
            onSelect = onSourceLanguageChange,
            onDismiss = { showSourcePicker = false }
        )
    }

    if (showTargetPicker) {
        LanguagePickerDialog(
            title = "Translate To",
            selectedLanguage = preferences.targetLanguage,
            allowAutoDetect = false,
            onSelect = { selected ->
                if (selected != null) onTargetLanguageChange(selected)
            },
            onDismiss = { showTargetPicker = false }
        )
    }

    if (showEditCurrentDialog && translationState is BridgeTranslationUiState.Ready) {
        EditTranslationResultDialog(
            initialTranslatedText = translationState.result.translatedText,
            initialNote = translationState.result.culturalNote,
            onDismiss = { showEditCurrentDialog = false },
            onConfirm = { newText, newNote ->
                onEditCurrentTranslation(newText, newNote)
                showEditCurrentDialog = false
            }
        )
    }

    editingVaultRecord?.let { rec ->
        EditVaultRecordDialog(
            record = rec,
            onDismiss = { editingVaultRecord = null },
            onSave = { source, translated, romanization, note ->
                onUpdateSavedRecord(rec, source, translated, romanization, note)
                editingVaultRecord = null
            }
        )
    }
}
