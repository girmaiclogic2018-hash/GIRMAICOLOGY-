package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LanguageProgress
import com.example.data.PracticeStats
import com.example.data.SessionRecordEntity
import com.example.ui.components.formatDuration
import com.example.ui.util.ShareAndCopyHelper
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    sessions: List<SessionRecordEntity>,
    progressStats: PracticeStats,
    searchQuery: String,
    selectedLanguageFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onLanguageFilterChange: (String?) -> Unit,
    onPracticeAgain: (SessionRecordEntity) -> Unit,
    onDeleteSession: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSession by remember { mutableStateOf<SessionRecordEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 760.dp)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Progress & History",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "${progressStats.totalSessions} saved voice sessions · ${progressStats.totalMinutes} min total",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Progress Overview Metrics Strip (computed from Room)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProgressMetricTile(
                icon = Icons.Filled.LocalFireDepartment,
                value = "${progressStats.streakDays}d",
                label = "Streak",
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_streak")
            )
            ProgressMetricTile(
                icon = Icons.Filled.Timer,
                value = "${progressStats.totalMinutes}m",
                label = "Practiced",
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_minutes")
            )
            ProgressMetricTile(
                icon = Icons.Filled.ChatBubbleOutline,
                value = "${progressStats.totalTurns}",
                label = "Turns",
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_turns")
            )
        }

        if (progressStats.languageBreakdown.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedLanguageFilter == null,
                        onClick = { onLanguageFilterChange(null) },
                        label = { Text("All (${progressStats.totalSessions})") },
                        modifier = Modifier.testTag("history_filter_all")
                    )
                }
                items(progressStats.languageBreakdown, key = { it.languageCode }) { langStat ->
                    LanguageProgressChip(
                        progress = langStat,
                        selected = selectedLanguageFilter == langStat.languageCode,
                        onClick = {
                            onLanguageFilterChange(
                                if (selectedLanguageFilter == langStat.languageCode) null
                                else langStat.languageCode
                            )
                        }
                    )
                }
            }
        }

        if (progressStats.totalSessions > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search scenarios, feedback, or transcripts...") },
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
                    .testTag("history_search_input")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (progressStats.totalSessions == 0) {
                            stringResource(R.string.no_history_title)
                        } else {
                            "No Matching Sessions"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (progressStats.totalSessions == 0) {
                            stringResource(R.string.no_history_body)
                        } else {
                            "Try clearing your search query or language filter."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionHistoryCard(
                        session = session,
                        onClick = { selectedSession = session },
                        onDelete = { onDeleteSession(session.id) }
                    )
                }
            }
        }
    }

    selectedSession?.let { session ->
        ModalBottomSheet(onDismissRequest = { selectedSession = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${session.languageName} · ${session.scenarioTitle}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "${session.levelLabel} · ${formatDuration(session.durationSeconds)} · ${session.turnCount} turns",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "AI Coaching Feedback",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = session.aiSummaryFeedback,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (session.strengthsSummary.isNotBlank()) {
                            Text(
                                text = "Strengths",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = session.strengthsSummary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (session.grammarTip.isNotBlank()) {
                            Text(
                                text = "Grammar & Phrasing Tip",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = session.grammarTip,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (session.transcriptText.isNotBlank()) {
                    Text(
                        text = "Conversation Transcript",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = session.transcriptText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val summary = buildString {
                                append("Girmaicology Practice: ${session.languageName} · ${session.scenarioTitle}\n")
                                append("Feedback: ${session.aiSummaryFeedback}\n")
                                if (session.transcriptText.isNotBlank()) {
                                    append("\nTranscript:\n${session.transcriptText}")
                                }
                            }
                            ShareAndCopyHelper.shareText(
                                context,
                                "${session.languageName} Practice Transcript",
                                summary
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Share")
                    }
                    Button(
                        onClick = {
                            onPracticeAgain(session)
                            selectedSession = null
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("practice_again_button")
                    ) {
                        Text("Practice Again")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressMetricTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LanguageProgressChip(
    progress: LanguageProgress,
    selected: Boolean,
    onClick: () -> Unit
) {
    val mins = ((progress.totalSeconds + 30) / 60).toInt()
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text("${progress.languageName} · ${progress.sessionCount} (${mins}m)")
        },
        modifier = Modifier.testTag("history_filter_${progress.languageCode}")
    )
}

@Composable
private fun SessionHistoryCard(
    session: SessionRecordEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateLabel = remember(session.createdAt) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(session.createdAt))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("session_card_${session.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        text = "${session.languageName} · ${session.levelLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("delete_session_${session.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete session record",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = session.scenarioTitle,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = session.aiSummaryFeedback,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${formatDuration(session.durationSeconds)} · ${session.turnCount} turns · $dateLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
