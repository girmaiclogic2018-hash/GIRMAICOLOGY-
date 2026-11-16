package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferences
import com.example.model.TargetLanguage
import com.example.model.TranslationTone
import com.example.ui.viewmodel.BridgeTranslationUiState

private val QuickBridgePhrases = listOf(
    "Good morning, how are you?",
    "How much does this cost?",
    "Where can I find good coffee?",
    "Thank you very much for your help.",
    "Please speak slowly, I am learning."
)

fun LazyListScope.bridgeTranslatorItems(
    preferences: UserPreferences,
    sourceLanguage: TargetLanguage?,
    selectedTone: TranslationTone,
    inputText: String,
    translationState: BridgeTranslationUiState,
    onOpenSourcePicker: () -> Unit,
    onOpenTargetPicker: () -> Unit,
    onSwapLanguages: (TargetLanguage) -> Unit,
    onSelectTone: (TranslationTone) -> Unit,
    onInputTextChange: (String) -> Unit,
    onUploadPhotoClick: () -> Unit,
    onUploadDocumentClick: () -> Unit,
    onTranslate: () -> Unit,
    onToggleStarCurrent: () -> Unit,
    onOpenEditCurrent: () -> Unit,
    onSaveToPhrasebook: () -> Unit,
    onSpeakText: (String, TargetLanguage, Boolean) -> Unit,
    onCopyText: (String) -> Unit,
    onShareText: (String, String) -> Unit,
    onPracticeInLiveVoice: (TargetLanguage) -> Unit
) {
    // Language Pair Selector Row
    item {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenSourcePicker)
                        .padding(8.dp)
                        .testTag("bridge_source_picker")
                ) {
                    Text(
                        text = "From",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = sourceLanguage?.displayName ?: "Auto-Detect",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onSwapLanguages(preferences.targetLanguage) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("bridge_swap_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = "Swap languages",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenTargetPicker)
                        .padding(8.dp)
                        .testTag("bridge_target_picker")
                ) {
                    Text(
                        text = "To (${preferences.targetLanguage.category.label})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = preferences.targetLanguage.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // Tone / Register Chips + Input Editor + Upload Actions
    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TranslationTone.entries) { tone ->
                    FilterChip(
                        selected = selectedTone == tone,
                        onClick = { onSelectTone(tone) },
                        label = { Text(tone.label) },
                        modifier = Modifier.testTag("tone_chip_${tone.name}")
                    )
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                label = { Text("Write or paste text to translate") },
                placeholder = { Text("Type any sentence, paragraph, or upload a photo/file...") },
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { onInputTextChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear text")
                        }
                    }
                },
                supportingText = {
                    val wordCount = inputText.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
                    Text("$wordCount words · ${inputText.length} chars")
                },
                minLines = 3,
                maxLines = 7,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bridge_input_field")
            )

            // Zero-permission Photo & Document Upload Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onUploadPhotoClick,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("bridge_upload_photo_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Photo", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onUploadDocumentClick,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("bridge_upload_file_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload File", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QuickBridgePhrases) { sample ->
                    FilterChip(
                        selected = inputText == sample,
                        onClick = { onInputTextChange(sample) },
                        label = { Text(sample, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            Button(
                onClick = onTranslate,
                enabled = translationState !is BridgeTranslationUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("bridge_translate_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Translate & Coach Pronunciation")
            }
        }
    }

    // Translation Output
    item {
        when (translationState) {
            is BridgeTranslationUiState.Idle -> {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Precision Global & Ethiopian Translator",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Translate text, uploaded signs/photos, or TXT/PDF files across all 55 Ethiopian, African & Worldwide languages with dictionary synonyms, phonetic guides, polite/casual registers, audio playback, sharing, and automatic history saving.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is BridgeTranslationUiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(translationState.label)
                }
            }

            is BridgeTranslationUiState.Error -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = translationState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            is BridgeTranslationUiState.Ready -> {
                TranslationResultCard(
                    state = translationState,
                    onSpeakNormal = {
                        onSpeakText(translationState.result.translatedText, translationState.targetLanguage, false)
                    },
                    onSpeakSlow = {
                        onSpeakText(translationState.result.translatedText, translationState.targetLanguage, true)
                    },
                    onCopy = { onCopyText(translationState.result.translatedText) },
                    onShare = {
                        val shareBody = buildString {
                            append(translationState.input)
                            append("\n→ ")
                            append(translationState.result.translatedText)
                            if (translationState.result.romanization.isNotBlank() &&
                                translationState.result.romanization != translationState.result.translatedText
                            ) {
                                append("\n(${translationState.result.romanization})")
                            }
                        }
                        onShareText(
                            "Translation to ${translationState.targetLanguage.displayName}",
                            shareBody
                        )
                    },
                    onToggleStar = onToggleStarCurrent,
                    onEdit = onOpenEditCurrent,
                    onSaveToPhrasebook = onSaveToPhrasebook,
                    onPracticeInLiveVoice = { onPracticeInLiveVoice(translationState.targetLanguage) },
                    onCopyAlternative = onCopyText
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranslationResultCard(
    state: BridgeTranslationUiState.Ready,
    onSpeakNormal: () -> Unit,
    onSpeakSlow: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onToggleStar: () -> Unit,
    onEdit: () -> Unit,
    onSaveToPhrasebook: () -> Unit,
    onPracticeInLiveVoice: () -> Unit,
    onCopyAlternative: (String) -> Unit
) {
    val res = state.result
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = buildString {
                            append("${res.detectedSourceLanguage} → ${state.targetLanguage.displayName}")
                            if (res.partOfSpeech.isNotBlank()) append(" · ${res.partOfSpeech}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.testTag("bridge_star_button")
                ) {
                    Icon(
                        imageVector = if (state.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = if (state.isStarred) "Saved" else "Save translation",
                        tint = if (state.isStarred) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = res.translatedText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("bridge_translated_output")
            )

            if (res.romanization.isNotBlank() && res.romanization != res.translatedText) {
                Text(
                    text = res.romanization,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Full Action Strip: Listen, Slow, Copy, Share, Edit, Save Phrasebook
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = onSpeakNormal,
                    label = { Text("Listen") },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("bridge_speak_button")
                )
                AssistChip(
                    onClick = onSpeakSlow,
                    label = { Text("0.7x") },
                    modifier = Modifier.testTag("bridge_speak_slow_button")
                )
                AssistChip(
                    onClick = onCopy,
                    label = { Text("Copy") },
                    leadingIcon = {
                        Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("bridge_copy_button")
                )
                AssistChip(
                    onClick = onShare,
                    label = { Text("Share") },
                    leadingIcon = {
                        Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("bridge_share_button")
                )
                AssistChip(
                    onClick = onEdit,
                    label = { Text("Edit") },
                    leadingIcon = {
                        Icon(Icons.Filled.Edit, null, Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("bridge_edit_button")
                )
                AssistChip(
                    onClick = onSaveToPhrasebook,
                    enabled = !state.isSavedToPhrasebook,
                    label = { Text(if (state.isSavedToPhrasebook) "In Phrasebook" else "Phrasebook") },
                    leadingIcon = {
                        Icon(
                            if (state.isSavedToPhrasebook) Icons.Filled.CheckCircle else Icons.Filled.BookmarkAdd,
                            null,
                            Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("bridge_save_phrase_button")
                )
            }

            if (res.alternativeTranslations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Alternative Ways to Say It (tap to copy)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        res.alternativeTranslations.forEach { alt ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onCopyAlternative(alt) }
                            ) {
                                Text(
                                    text = alt,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (res.phoneticGuide.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "How to Pronounce",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = res.phoneticGuide,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (res.politeVariant.isNotBlank() || res.casualVariant.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (res.politeVariant.isNotBlank()) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Polite / Formal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = res.politeVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    if (res.casualVariant.isNotBlank()) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Casual / Everyday",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = res.casualVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (res.exampleSentence.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Real-World Example",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = res.exampleSentence,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (res.exampleTranslation.isNotBlank()) {
                        Text(
                            text = res.exampleTranslation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (res.culturalNote.isNotBlank()) {
                Text(
                    text = "Note: ${res.culturalNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onPracticeInLiveVoice,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Practice ${state.targetLanguage.displayName} in Live Voice")
            }
        }
    }
}

@Composable
fun EditTranslationResultDialog(
    initialTranslatedText: String,
    initialNote: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var translated by remember { mutableStateOf(initialTranslatedText) }
    var note by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Translation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = translated,
                    onValueChange = { translated = it },
                    label = { Text("Translated Text") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Personal / Cultural Note") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(translated, note) },
                enabled = translated.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
