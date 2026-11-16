package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferences
import com.example.model.ThemePreference
import kotlin.math.roundToInt

private val NativeLanguages = listOf(
    "English",
    "Amharic (አማርኛ)",
    "Afaan Oromo",
    "Tigrigna (ትግርኛ)",
    "Somali",
    "Swahili",
    "Yoruba",
    "Hausa",
    "Arabic",
    "French",
    "Spanish",
    "Mandarin Chinese",
    "Hindi",
    "Portuguese",
    "Bengali",
    "Russian",
    "Japanese",
    "German",
    "Korean",
    "Indonesian",
    "Turkish",
    "Italian",
    "Urdu",
    "Vietnamese"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onNativeLanguageChange: (String) -> Unit,
    onGentleCorrectionsChange: (Boolean) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nativeExpanded by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Tutor Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Customize how your Gemini Live language partner coaches you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Native Coaching Language
        item {
            SettingsCard(title = "Native Explanation Language") {
                ExposedDropdownMenuBox(
                    expanded = nativeExpanded,
                    onExpandedChange = { nativeExpanded = !nativeExpanded }
                ) {
                    OutlinedTextField(
                        value = preferences.nativeLanguageName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nativeExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("native_language_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = nativeExpanded,
                        onDismissRequest = { nativeExpanded = false }
                    ) {
                        NativeLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    onNativeLanguageChange(lang)
                                    nativeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Gentle Corrections Switch
        item {
            SettingsCard(title = "Coaching Style") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Gentle Live Corrections",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Partner offers quick recasts and grammar tips during conversation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.gentleCorrections,
                        onCheckedChange = onGentleCorrectionsChange,
                        modifier = Modifier.testTag("gentle_corrections_switch")
                    )
                }
            }
        }

        // Daily Goal Slider
        item {
            SettingsCard(title = "Daily Practice Goal") {
                Text(
                    text = "${preferences.dailyGoalMinutes} minutes per day",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = preferences.dailyGoalMinutes.toFloat(),
                    onValueChange = { onDailyGoalChange((it / 5f).roundToInt() * 5) },
                    valueRange = 5f..60f,
                    steps = 10,
                    modifier = Modifier.testTag("daily_goal_slider")
                )
            }
        }

        // Appearance Theme
        item {
            SettingsCard(title = "Studio Appearance") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemePreference.entries.forEach { theme ->
                        FilterChip(
                            selected = preferences.themePreference == theme,
                            onClick = { onThemeChange(theme) },
                            label = { Text(theme.label) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_chip_${theme.name}")
                        )
                    }
                }
            }
        }

        // Data Management
        item {
            SettingsCard(title = "Local Storage") {
                Text(
                    text = "Saved phrases and session transcripts are stored privately on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("clear_data_button")
                ) {
                    Text("Clear Phrasebook & History")
                }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Local Data?") },
            text = { Text("This will permanently remove all saved phrases and conversation history on this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            content()
        }
    }
}
