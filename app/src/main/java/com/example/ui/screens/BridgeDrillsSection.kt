package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferences
import com.example.model.TargetLanguage
import com.example.ui.viewmodel.DrillTopics
import com.example.ui.viewmodel.SkillDrillUiState

fun LazyListScope.bridgeDrillItems(
    preferences: UserPreferences,
    selectedTopic: String,
    drillState: SkillDrillUiState,
    onOpenTargetPicker: () -> Unit,
    onSelectTopic: (String) -> Unit,
    onGenerateDrill: () -> Unit,
    onSelectAnswer: (Int, Int) -> Unit,
    onSaveDrillExpression: (String, String, String) -> Unit,
    onSpeakText: (String, TargetLanguage, Boolean) -> Unit
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Target: ${preferences.targetLanguage.displayName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${preferences.proficiencyLevel.label} (${preferences.proficiencyLevel.cefrTag})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onOpenTargetPicker) {
                    Text("Change Language")
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DrillTopics) { topic ->
                    FilterChip(
                        selected = selectedTopic == topic,
                        onClick = { onSelectTopic(topic) },
                        label = { Text(topic) }
                    )
                }
            }

            Button(
                onClick = onGenerateDrill,
                enabled = drillState !is SkillDrillUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_drill_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (drillState is SkillDrillUiState.Ready) "New 4-Question Challenge"
                    else "Start Interactive Drill"
                )
            }
        }
    }

    item {
        when (drillState) {
            is SkillDrillUiState.Idle -> {
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
                            text = "Active Scenario Challenges",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Practice real greetings, polite register, market bargaining, and cultural idioms in ${preferences.targetLanguage.displayName} with instant explanations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is SkillDrillUiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Crafting ${preferences.targetLanguage.displayName} drill...")
                }
            }

            is SkillDrillUiState.Error -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = drillState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            is SkillDrillUiState.Ready -> {
                val questions = drillState.drillSet.questions
                val answeredCount = drillState.selectedAnswers.size
                val correctCount = drillState.selectedAnswers.count { (qIdx, optIdx) ->
                    questions.getOrNull(qIdx)?.correctIndex == optIdx
                }

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${drillState.drillSet.languageName} · ${drillState.drillSet.topic}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Score: $correctCount/$answeredCount",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    questions.forEachIndexed { qIndex, question ->
                        val chosenIndex = drillState.selectedAnswers[qIndex]
                        val isSaved = drillState.savedExpressions.contains(question.targetExpression)

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
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Question ${qIndex + 1} of ${questions.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = question.prompt,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (question.targetExpression.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = question.targetExpression,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                if (question.romanization.isNotBlank()) {
                                                    Text(
                                                        text = question.romanization,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    onSpeakText(
                                                        question.targetExpression,
                                                        drillState.targetLanguage,
                                                        true
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Hear expression"
                                                )
                                            }
                                        }
                                    }
                                }

                                question.options.forEachIndexed { optIdx, optionText ->
                                    val isChosen = chosenIndex == optIdx
                                    val isCorrectOption = optIdx == question.correctIndex
                                    val bg = when {
                                        chosenIndex == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        isCorrectOption -> MaterialTheme.colorScheme.primaryContainer
                                        isChosen -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = chosenIndex == null) {
                                                onSelectAnswer(qIndex, optIdx)
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = bg,
                                        border = if (isChosen || (chosenIndex != null && isCorrectOption)) {
                                            BorderStroke(
                                                1.5.dp,
                                                if (isCorrectOption) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.error
                                            )
                                        } else null
                                    ) {
                                        Text(
                                            text = optionText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                        )
                                    }
                                }

                                if (chosenIndex != null) {
                                    Text(
                                        text = question.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (question.targetExpression.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                val answerMeaning = question.options.getOrElse(question.correctIndex) { question.prompt }
                                                onSaveDrillExpression(
                                                    question.targetExpression,
                                                    answerMeaning,
                                                    question.romanization
                                                )
                                            },
                                            enabled = !isSaved
                                        ) {
                                            Icon(
                                                imageVector = if (isSaved) Icons.Filled.CheckCircle else Icons.Filled.BookmarkAdd,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isSaved) "Saved to Phrasebook" else "Save Expression")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
