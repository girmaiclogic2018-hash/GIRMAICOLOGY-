package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.PartnerVoice
import com.example.model.ProficiencyLevel
import com.example.model.TargetLanguage
import com.example.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.linguaDataStore: DataStore<Preferences> by preferencesDataStore(name = "lingua_prefs")

data class UserPreferences(
    val targetLanguage: TargetLanguage = TargetLanguage.SPANISH,
    val proficiencyLevel: ProficiencyLevel = ProficiencyLevel.BEGINNER,
    val partnerVoice: PartnerVoice = PartnerVoice.AOEDE,
    val nativeLanguageName: String = "English",
    val gentleCorrections: Boolean = true,
    val dailyGoalMinutes: Int = 15,
    val themePreference: ThemePreference = ThemePreference.SYSTEM
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val TARGET_LANG = stringPreferencesKey("target_language")
        val LEVEL = stringPreferencesKey("proficiency_level")
        val VOICE = stringPreferencesKey("partner_voice")
        val NATIVE_LANG = stringPreferencesKey("native_language")
        val GENTLE_CORRECTIONS = booleanPreferencesKey("gentle_corrections")
        val DAILY_GOAL_MIN = intPreferencesKey("daily_goal_minutes")
        val THEME_PREF = stringPreferencesKey("theme_preference")
    }

    val preferencesFlow: Flow<UserPreferences> = context.linguaDataStore.data.map { prefs ->
        UserPreferences(
            targetLanguage = prefs[Keys.TARGET_LANG]?.let { name ->
                TargetLanguage.entries.firstOrNull { it.name == name }
            } ?: TargetLanguage.SPANISH,
            proficiencyLevel = prefs[Keys.LEVEL]?.let { name ->
                ProficiencyLevel.entries.firstOrNull { it.name == name }
            } ?: ProficiencyLevel.BEGINNER,
            partnerVoice = prefs[Keys.VOICE]?.let { name ->
                PartnerVoice.entries.firstOrNull { it.name == name }
            } ?: PartnerVoice.AOEDE,
            nativeLanguageName = prefs[Keys.NATIVE_LANG] ?: "English",
            gentleCorrections = prefs[Keys.GENTLE_CORRECTIONS] ?: true,
            dailyGoalMinutes = prefs[Keys.DAILY_GOAL_MIN] ?: 15,
            themePreference = prefs[Keys.THEME_PREF]?.let { name ->
                ThemePreference.entries.firstOrNull { it.name == name }
            } ?: ThemePreference.SYSTEM
        )
    }

    suspend fun setTargetLanguage(language: TargetLanguage) {
        context.linguaDataStore.edit { it[Keys.TARGET_LANG] = language.name }
    }

    suspend fun setProficiencyLevel(level: ProficiencyLevel) {
        context.linguaDataStore.edit { it[Keys.LEVEL] = level.name }
    }

    suspend fun setPartnerVoice(voice: PartnerVoice) {
        context.linguaDataStore.edit { it[Keys.VOICE] = voice.name }
    }

    suspend fun setNativeLanguage(nativeLanguage: String) {
        context.linguaDataStore.edit { it[Keys.NATIVE_LANG] = nativeLanguage }
    }

    suspend fun setGentleCorrections(enabled: Boolean) {
        context.linguaDataStore.edit { it[Keys.GENTLE_CORRECTIONS] = enabled }
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.linguaDataStore.edit { it[Keys.DAILY_GOAL_MIN] = minutes.coerceIn(5, 60) }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.linguaDataStore.edit { it[Keys.THEME_PREF] = theme.name }
    }
}
