package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferences
import com.example.model.TargetLanguage
import com.example.model.TranslationTone
import com.example.ui.viewmodel.WritingPolishUiState

private val WritingStarterPrompts = listOf(
    "Hello teacher, thank you for your patience with my homework today.",
    "I would like to invite my friends for traditional coffee this weekend.",
    "Could you please confirm our meeting schedule for tomorrow morning?"
)

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.bridgeWritingItems(
    preferences: UserPreferences,
    selectedTone: TranslationTone,
    writingDraft: String,
    writingState: WritingPolishUiState,
    onOpenTargetPicker: () -> Unit,
    onSelectTone: (TranslationTone) -> Unit,
    onWritingDraftChange: (String) -> Unit,
    onPolishWriting: () -> Unit,
    onSaveToVault: () -> Unit,
    onSaveToPhrasebook: () -> Unit,
    onSpeakText: (String, TargetLanguage, Boolean) -> Unit,
    onCopyText: (String) -> Unit,
    onShareText: (String, String) -> Unit
) {
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
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Writing Target Language",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${preferences.targetLanguage.displayName} (${preferences.targetLanguage.nativeName})",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = onOpenTargetPicker) {
                    Text("Change")
                }
            }
        }
    }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TranslationTone.entries) { tone ->
                    FilterChip(
                        selected = selectedTone == tone,
                        onClick = { onSelectTone(tone) },
                        label = { Text(tone.label) }
                    )
                }
            }

            OutlinedTextField(
                value = writingDraft,
                onValueChange = onWritingDraftChange,
                label = { Text("Compose, draft, or proofread") },
                placeholder = {
                    Text("Write in ${preferences.targetLanguage.displayName} to fix grammar, or write in ${preferences.nativeLanguageName} to craft a native-quality message...")
                },
                trailingIcon = {
                    if (writingDraft.isNotEmpty()) {
                        IconButton(onClick = { onWritingDraftChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear draft")
                        }
                    }
                },
                supportingText = {
                    val words = writingDraft.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
                    Text("$words words · ${writingDraft.length} characters")
                },
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bridge_writing_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WritingStarterPrompts) { prompt ->
                    FilterChip(
                        selected = writingDraft == prompt,
                        onClick = { onWritingDraftChange(prompt) },
                        label = { Text(prompt, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            Button(
                onClick = onPolishWriting,
                enabled = writingState !is WritingPolishUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("bridge_polish_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Proofread & Polish Writing")
            }
        }
    }

    item {
        when (writingState) {
            is WritingPolishUiState.Idle -> {
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
                            text = "Multilingual Writing Assistant",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Practice writing paragraphs, emails, greetings, and study notes in ${preferences.targetLanguage.displayName}. Get instant grammar fixes, native flow improvements, pronunciation guidance, and one-tap saving.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is WritingPolishUiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Polishing your ${preferences.targetLanguage.displayName} writing...")
                }
            }

            is WritingPolishUiState.Error -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = writingState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            is WritingPolishUiState.Ready -> {
                val res = writingState.result
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Polished ${writingState.targetLanguage.displayName} (${writingState.tone.label})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = res.correctedText,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (res.romanization.isNotBlank() && res.romanization != res.correctedText) {
                            Text(
                                text = res.romanization,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (res.nativeTranslation.isNotBlank()) {
                            Text(
                                text = "Meaning: ${res.nativeTranslation}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    onSpeakText(res.correctedText, writingState.targetLanguage, false)
                                },
                                label = { Text("Listen") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(16.dp))
                                }
                            )
                            AssistChip(
                                onClick = { onCopyText(res.correctedText) },
                                label = { Text("Copy") },
                                leadingIcon = {
                                    Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp))
                                }
                            )
                            AssistChip(
                                onClick = {
                                    onShareText(
                                        "${writingState.targetLanguage.displayName} Writing",
                                        "${res.correctedText}\n\n(${res.nativeTranslation})"
                                    )
                                },
                                label = { Text("Share") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                                }
                            )
                            AssistChip(
                                onClick = onSaveToVault,
                                enabled = !writingState.isSavedToVault,
                                label = { Text(if (writingState.isSavedToVault) "Saved" else "Save") },
                                leadingIcon = {
                                    Icon(
                                        if (writingState.isSavedToVault) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        null,
                                        Modifier.size(16.dp)
                                    )
                                }
                            )
                            AssistChip(
                                onClick = onSaveToPhrasebook,
                                enabled = !writingState.isSavedToPhrasebook,
                                label = { Text(if (writingState.isSavedToPhrasebook) "In Phrasebook" else "Phrasebook") },
                                leadingIcon = {
                                    Icon(
                                        if (writingState.isSavedToPhrasebook) Icons.Filled.CheckCircle else Icons.Filled.BookmarkAdd,
                                        null,
                                        Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        if (res.improvements.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Grammar & Style Improvements",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    res.improvements.forEach { tip ->
                                        Text(
                                            text = "• $tip",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        if (res.alternativeVersion.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Alternative Phrasing",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = res.alternativeVersion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
