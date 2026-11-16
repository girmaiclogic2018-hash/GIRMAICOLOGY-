package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PhraseEntity
import com.example.model.LanguageCategory
import com.example.model.TargetLanguage
import com.example.ui.util.ShareAndCopyHelper
import com.example.ui.viewmodel.InsightUiState
import com.example.ui.viewmodel.MasteryFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhrasebookScreen(
    phrases: List<PhraseEntity>,
    availableLanguages: List<Pair<String, String>>,
    searchQuery: String,
    selectedLanguageFilter: String?,
    masteryFilter: MasteryFilter,
    defaultLanguage: TargetLanguage,
    insightState: InsightUiState,
    onSearchQueryChange: (String) -> Unit,
    onLanguageFilterChange: (String?) -> Unit,
    onMasteryFilterChange: (MasteryFilter) -> Unit,
    onAddPhrase: (phrase: String, translation: String, language: TargetLanguage, note: String) -> Unit,
    onUpdatePhrase: (existing: PhraseEntity, phrase: String, translation: String, language: TargetLanguage, note: String) -> Unit,
    onMarkReviewed: (PhraseEntity, Boolean) -> Unit,
    onDeletePhrase: (Long) -> Unit,
    onExplainPhrase: (PhraseEntity) -> Unit,
    onSpeakPhrase: (PhraseEntity) -> Unit,
    onShowMessage: (String) -> Unit,
    onClearFilters: () -> Unit,
    onDismissInsight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var flashcardMode by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPhrase by remember { mutableStateOf<PhraseEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Phrasebook",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${phrases.size} saved expressions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = !flashcardMode,
                        onClick = { flashcardMode = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("phrasebook_list_tab")
                    ) {
                        Text(stringResource(R.string.list_mode))
                    }
                    SegmentedButton(
                        selected = flashcardMode,
                        onClick = { flashcardMode = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("phrasebook_flashcard_tab")
                    ) {
                        Text(stringResource(R.string.flashcard_mode))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search phrases or translations...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phrasebook_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filters
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MasteryFilter.entries) { filter ->
                    FilterChip(
                        selected = masteryFilter == filter,
                        onClick = { onMasteryFilterChange(filter) },
                        label = { Text(filter.label) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedLanguageFilter == null,
                        onClick = { onLanguageFilterChange(null) },
                        label = { Text("All Languages") }
                    )
                }
                items(availableLanguages, key = { it.first }) { (code, name) ->
                    FilterChip(
                        selected = selectedLanguageFilter == code,
                        onClick = {
                            onLanguageFilterChange(
                                if (selectedLanguageFilter == code) null else code
                            )
                        },
                        label = { Text(name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (phrases.isEmpty()) {
                val hasActiveFilters = searchQuery.isNotBlank() ||
                    selectedLanguageFilter != null ||
                    masteryFilter != MasteryFilter.ALL
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (hasActiveFilters) {
                                "No Matching Phrases"
                            } else {
                                stringResource(R.string.no_phrases_title)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (hasActiveFilters) {
                                "Try clearing your search or language/mastery filters."
                            } else {
                                stringResource(R.string.no_phrases_body)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (hasActiveFilters) {
                            OutlinedButton(onClick = onClearFilters) {
                                Text("Clear Filters")
                            }
                        } else {
                            Button(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.testTag("empty_add_phrase_button")
                            ) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.add_phrase))
                            }
                        }
                    }
                }
            } else if (flashcardMode) {
                FlashcardDeck(
                    phrases = phrases,
                    onSpeakPhrase = onSpeakPhrase,
                    onMarkReviewed = onMarkReviewed,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 96.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(phrases, key = { it.id }) { item ->
                        PhraseCard(
                            phrase = item,
                            onSpeak = { onSpeakPhrase(item) },
                            onCopy = {
                                if (ShareAndCopyHelper.copyToClipboard(context, "Phrase", item.phrase)) {
                                    onShowMessage("Copied \"${item.phrase}\"")
                                }
                            },
                            onShare = {
                                ShareAndCopyHelper.shareText(
                                    context,
                                    "${item.languageName} Phrase",
                                    "${item.phrase}\nMeaning: ${item.translation}" +
                                        if (item.contextNote.isNotBlank()) "\nNote: ${item.contextNote}" else ""
                                )
                            },
                            onEdit = { editingPhrase = item },
                            onExplain = { onExplainPhrase(item) },
                            onDelete = { onDeletePhrase(item.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .testTag("add_phrase_fab")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.add_phrase)
            )
        }
    }

    if (showAddDialog) {
        AddPhraseDialog(
            title = stringResource(R.string.add_phrase),
            initialPhrase = "",
            initialTranslation = "",
            initialNote = "",
            defaultLanguage = defaultLanguage,
            onDismiss = { showAddDialog = false },
            onSave = { phrase, translation, lang, note ->
                onAddPhrase(phrase, translation, lang, note)
                showAddDialog = false
            }
        )
    }

    editingPhrase?.let { existing ->
        val matchedLang = TargetLanguage.entries.firstOrNull { it.code == existing.languageCode }
            ?: defaultLanguage
        AddPhraseDialog(
            title = "Edit Phrase",
            initialPhrase = existing.phrase,
            initialTranslation = existing.translation,
            initialNote = existing.contextNote,
            defaultLanguage = matchedLang,
            onDismiss = { editingPhrase = null },
            onSave = { phrase, translation, lang, note ->
                onUpdatePhrase(existing, phrase, translation, lang, note)
                editingPhrase = null
            }
        )
    }

    if (insightState !is InsightUiState.Hidden) {
        ModalBottomSheet(onDismissRequest = onDismissInsight) {
            PhraseInsightSheetContent(
                state = insightState,
                onClose = onDismissInsight
            )
        }
    }
}

@Composable
private fun PhraseCard(
    phrase: PhraseEntity,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onExplain: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = phrase.languageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    MasteryDots(level = phrase.masteryLevel)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier
                            .size(38.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("speak_phrase_${phrase.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Hear pronunciation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(38.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("edit_phrase_${phrase.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit phrase",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(38.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("delete_phrase_${phrase.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete phrase",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = phrase.phrase,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = phrase.translation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (phrase.contextNote.isNotBlank()) {
                Text(
                    text = phrase.contextNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    TextButton(
                        onClick = onCopy,
                        modifier = Modifier.testTag("copy_phrase_${phrase.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    TextButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("share_phrase_${phrase.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }

                TextButton(
                    onClick = onExplain,
                    modifier = Modifier.testTag("explain_phrase_${phrase.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Breakdown")
                }
            }
        }
    }
}

@Composable
private fun MasteryDots(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { idx ->
            val filled = idx < level
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
private fun FlashcardDeck(
    phrases: List<PhraseEntity>,
    onSpeakPhrase: (PhraseEntity) -> Unit,
    onMarkReviewed: (PhraseEntity, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember(phrases.size) { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, (phrases.size - 1).coerceAtLeast(0))
    val current = phrases[safeIndex]
    var flipped by remember(current.id) { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 360),
        label = "flashcard_flip"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Card ${safeIndex + 1} of ${phrases.size} · Tap card to flip",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { flipped = !flipped }
                .testTag("flashcard_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (rotation <= 90f) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .graphicsLayer {
                        if (rotation > 90f) rotationY = 180f
                    },
                contentAlignment = Alignment.Center
            ) {
                if (rotation <= 90f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = current.languageName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = current.phrase,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onSpeakPhrase(current) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Hear phrase",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.FlipCameraAndroid,
                                contentDescription = "Tap to reveal translation",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "TRANSLATION",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = current.translation,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (current.contextNote.isNotBlank()) {
                            Text(
                                text = current.contextNote,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onMarkReviewed(current, false)
                    flipped = false
                    currentIndex = (safeIndex + 1) % phrases.size
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("flashcard_again_button")
            ) {
                Text("Still Learning")
            }
            Button(
                onClick = {
                    onMarkReviewed(current, true)
                    flipped = false
                    currentIndex = (safeIndex + 1) % phrases.size
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("flashcard_got_it_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Got It!")
            }
        }
    }
}

@Composable
private fun AddPhraseDialog(
    title: String,
    initialPhrase: String,
    initialTranslation: String,
    initialNote: String,
    defaultLanguage: TargetLanguage,
    onDismiss: () -> Unit,
    onSave: (phrase: String, translation: String, language: TargetLanguage, note: String) -> Unit
) {
    var phrase by remember { mutableStateOf(initialPhrase) }
    var translation by remember { mutableStateOf(initialTranslation) }
    var note by remember { mutableStateOf(initialNote) }
    var selectedLang by remember { mutableStateOf(defaultLanguage) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showLanguagePicker = true }
                        .testTag("add_phrase_language_selector"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
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
                                text = "Language",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${selectedLang.displayName} (${selectedLang.nativeName})",
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = "Choose language"
                        )
                    }
                }

                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text("Phrase or Expression") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_phrase_input")
                )
                OutlinedTextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text("Translation") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_translation_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Usage Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(phrase, translation, selectedLang, note) },
                enabled = phrase.isNotBlank() && translation.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_phrase")
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

    if (showLanguagePicker) {
        var langQuery by remember { mutableStateOf("") }
        var langCategory by remember { mutableStateOf<LanguageCategory?>(null) }
        val filtered = remember(langQuery, langCategory) {
            TargetLanguage.entries.filter { lang ->
                (langCategory == null || lang.category == langCategory) && lang.matchesQuery(langQuery)
            }
        }

        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text("Select Language") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = langQuery,
                        onValueChange = { langQuery = it },
                        placeholder = { Text("Search language...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = langCategory == null,
                                onClick = { langCategory = null },
                                label = { Text("All") }
                            )
                        }
                        items(LanguageCategory.entries) { cat ->
                            FilterChip(
                                selected = langCategory == cat,
                                onClick = { langCategory = if (langCategory == cat) null else cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered, key = { it.name }) { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedLang = lang
                                        showLanguagePicker = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${lang.displayName} (${lang.nativeName})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = lang.regionHint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (lang == selectedLang) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun PhraseInsightSheetContent(
    state: InsightUiState,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            is InsightUiState.Loading -> {
                Text(
                    text = state.phrase.phrase,
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Generating usage & pronunciation breakdown...")
                }
            }

            is InsightUiState.Error -> {
                Text(
                    text = state.phrase.phrase,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is InsightUiState.Ready -> {
                Text(
                    text = state.phrase.phrase,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = state.insight.literalMeaning,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Pronunciation",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = state.insight.pronunciationGuide,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Example in Context",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = state.insight.exampleSentence,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = state.insight.exampleTranslation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Tip: ${state.insight.culturalTip}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            InsightUiState.Hidden -> Unit
        }

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
        ) {
            Text("Close")
        }
    }
}
