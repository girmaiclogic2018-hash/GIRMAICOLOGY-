package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.TranslationRecordEntity
import com.example.model.TargetLanguage
import com.example.ui.viewmodel.VaultFilterMode
import java.text.DateFormat
import java.util.Date

fun LazyListScope.bridgeVaultItems(
    records: List<TranslationRecordEntity>,
    allRecordsCount: Int,
    availableTargetLanguages: List<Pair<String, String>>,
    searchQuery: String,
    filterMode: VaultFilterMode,
    languageFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onFilterModeChange: (VaultFilterMode) -> Unit,
    onLanguageFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onToggleStar: (TranslationRecordEntity) -> Unit,
    onOpenEditDialog: (TranslationRecordEntity) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onSaveToPhrasebook: (TranslationRecordEntity) -> Unit,
    onLoadIntoTranslator: (TranslationRecordEntity) -> Unit,
    onSpeakText: (String, TargetLanguage, Boolean) -> Unit,
    onCopyText: (String) -> Unit,
    onShareText: (String, String) -> Unit
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search saved translations, languages, notes...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vault_search_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(VaultFilterMode.entries) { mode ->
                    FilterChip(
                        selected = filterMode == mode,
                        onClick = { onFilterModeChange(mode) },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("vault_mode_${mode.name}")
                    )
                }
                if (availableTargetLanguages.isNotEmpty()) {
                    item {
                        FilterChip(
                            selected = languageFilter == null,
                            onClick = { onLanguageFilterChange(null) },
                            label = { Text("All Languages") }
                        )
                    }
                    items(availableTargetLanguages, key = { it.first }) { (code, name) ->
                        FilterChip(
                            selected = languageFilter == code,
                            onClick = {
                                onLanguageFilterChange(if (languageFilter == code) null else code)
                            },
                            label = { Text(name) }
                        )
                    }
                }
            }
        }
    }

    if (records.isEmpty()) {
        item {
            val hasFilters = searchQuery.isNotBlank() ||
                filterMode != VaultFilterMode.ALL ||
                languageFilter != null
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = if (hasFilters || allRecordsCount > 0) "No Matching Translations" else "No Translations Saved Yet",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (hasFilters) {
                            "Clear your search query or filters to view all saved records."
                        } else {
                            "Every text, photo, file translation, or polished writing draft you create can be saved, searched, edited, shared, and revisited here."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (hasFilters) {
                        OutlinedButton(onClick = onClearFilters) {
                            Text("Clear Filters")
                        }
                    }
                }
            }
        }
    } else {
        items(records, key = { it.id }) { record ->
            val targetLang = remember(record.targetLanguageCode) {
                TargetLanguage.entries.firstOrNull { it.code == record.targetLanguageCode }
                    ?: TargetLanguage.ENGLISH
            }
            TranslationVaultCard(
                record = record,
                onToggleStar = { onToggleStar(record) },
                onEdit = { onOpenEditDialog(record) },
                onDelete = { onDeleteRecord(record.id) },
                onSaveToPhrasebook = { onSaveToPhrasebook(record) },
                onLoadInTranslator = { onLoadIntoTranslator(record) },
                onSpeak = { onSpeakText(record.translatedText, targetLang, false) },
                onCopy = { onCopyText(record.translatedText) },
                onShare = {
                    onShareText(
                        "${record.sourceLanguageName} → ${record.targetLanguageName}",
                        "${record.sourceText}\n→ ${record.translatedText}"
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranslationVaultCard(
    record: TranslationRecordEntity,
    onToggleStar: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSaveToPhrasebook: () -> Unit,
    onLoadInTranslator: () -> Unit,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val dateLabel = remember(record.updatedAt) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(record.updatedAt))
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (record.isStarred) 1.5.dp else 1.dp,
            color = if (record.isStarred) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
        ),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_card_${record.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${record.sourceLanguageName} → ${record.targetLanguageName} · ${record.entryType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleStar,
                        modifier = Modifier
                            .size(40.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("vault_star_${record.id}")
                    ) {
                        Icon(
                            imageVector = if (record.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Save translation",
                            tint = if (record.isStarred) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(40.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("vault_edit_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit translation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("vault_delete_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete translation",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = record.sourceText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = record.translatedText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (record.romanization.isNotBlank() && record.romanization != record.translatedText) {
                Text(
                    text = record.romanization,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (record.note.isNotBlank()) {
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${record.toneLabel} · $dateLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = onSpeak,
                    label = { Text("Listen") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onCopy,
                    label = { Text("Copy") },
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onShare,
                    label = { Text("Share") },
                    leadingIcon = { Icon(Icons.Filled.Share, null, Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("vault_share_${record.id}")
                )
                AssistChip(
                    onClick = onSaveToPhrasebook,
                    label = { Text("Phrasebook") },
                    leadingIcon = { Icon(Icons.Filled.BookmarkAdd, null, Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onLoadInTranslator,
                    label = { Text("Open") },
                    leadingIcon = { Icon(Icons.Filled.Translate, null, Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
fun EditVaultRecordDialog(
    record: TranslationRecordEntity,
    onDismiss: () -> Unit,
    onSave: (source: String, translated: String, romanization: String, note: String) -> Unit
) {
    var source by remember(record.id) { mutableStateOf(record.sourceText) }
    var translated by remember(record.id) { mutableStateOf(record.translatedText) }
    var romanization by remember(record.id) { mutableStateOf(record.romanization) }
    var note by remember(record.id) { mutableStateOf(record.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Saved Translation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Original (${record.sourceLanguageName})") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_vault_source")
                )
                OutlinedTextField(
                    value = translated,
                    onValueChange = { translated = it },
                    label = { Text("Translation (${record.targetLanguageName})") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_vault_translated")
                )
                OutlinedTextField(
                    value = romanization,
                    onValueChange = { romanization = it },
                    label = { Text("Pronunciation / Romanization") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Personal Note") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_vault_note")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(source, translated, romanization, note) },
                enabled = source.isNotBlank() && translated.isNotBlank(),
                modifier = Modifier.testTag("confirm_edit_vault")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
