package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CoachFeedbackService
import com.example.data.LinguaRepository
import com.example.data.PhraseEntity
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import com.example.model.PhraseInsight
import com.example.model.TargetLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MasteryFilter(val label: String) {
    ALL("All"),
    LEARNING("Learning"),
    MASTERED("Mastered")
}

sealed interface InsightUiState {
    data object Hidden : InsightUiState
    data class Loading(val phrase: PhraseEntity) : InsightUiState
    data class Ready(val phrase: PhraseEntity, val insight: PhraseInsight) : InsightUiState
    data class Error(val phrase: PhraseEntity, val message: String) : InsightUiState
}

class PhrasebookViewModel(
    private val repository: LinguaRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val coachService: CoachFeedbackService = CoachFeedbackService()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLanguageFilter = MutableStateFlow<String?>(null)
    val selectedLanguageFilter: StateFlow<String?> = _selectedLanguageFilter.asStateFlow()

    private val _masteryFilter = MutableStateFlow(MasteryFilter.ALL)
    val masteryFilter: StateFlow<MasteryFilter> = _masteryFilter.asStateFlow()

    private val _insightState = MutableStateFlow<InsightUiState>(InsightUiState.Hidden)
    val insightState: StateFlow<InsightUiState> = _insightState.asStateFlow()

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserPreferences())

    val availableLanguages: StateFlow<List<Pair<String, String>>> = repository.allPhrases
        .map { phrases ->
            phrases
                .map { it.languageCode to it.languageName }
                .distinctBy { it.first }
                .sortedBy { it.second }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val filteredPhrases: StateFlow<List<PhraseEntity>> = combine(
        repository.allPhrases,
        _searchQuery,
        _selectedLanguageFilter,
        _masteryFilter
    ) { phrases, query, langCode, mastery ->
        val validLangCode = if (langCode != null && phrases.none { it.languageCode == langCode }) {
            null
        } else {
            langCode
        }
        phrases.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.phrase.contains(query, ignoreCase = true) ||
                item.translation.contains(query, ignoreCase = true) ||
                item.contextNote.contains(query, ignoreCase = true)
            val matchesLang = validLangCode == null || item.languageCode == validLangCode
            val matchesMastery = when (mastery) {
                MasteryFilter.ALL -> true
                MasteryFilter.LEARNING -> item.masteryLevel < 3
                MasteryFilter.MASTERED -> item.masteryLevel >= 3
            }
            matchesQuery && matchesLang && matchesMastery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLanguageFilter(code: String?) {
        _selectedLanguageFilter.value = code
    }

    fun setMasteryFilter(filter: MasteryFilter) {
        _masteryFilter.value = filter
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedLanguageFilter.value = null
        _masteryFilter.value = MasteryFilter.ALL
    }

    fun addManualPhrase(
        phrase: String,
        translation: String,
        language: TargetLanguage,
        contextNote: String
    ) {
        if (phrase.isBlank() || translation.isBlank()) return
        viewModelScope.launch {
            repository.savePhrase(
                PhraseEntity(
                    phrase = phrase.trim(),
                    translation = translation.trim(),
                    languageCode = language.code,
                    languageName = language.displayName,
                    contextNote = contextNote.trim()
                )
            )
        }
    }

    fun updatePhrase(
        existing: PhraseEntity,
        phrase: String,
        translation: String,
        language: TargetLanguage,
        contextNote: String
    ) {
        if (phrase.isBlank() || translation.isBlank()) return
        viewModelScope.launch {
            repository.updatePhrase(
                existing.copy(
                    phrase = phrase.trim(),
                    translation = translation.trim(),
                    languageCode = language.code,
                    languageName = language.displayName,
                    contextNote = contextNote.trim()
                )
            )
        }
    }

    fun markReviewed(phrase: PhraseEntity, remembered: Boolean) {
        val nextLevel = if (remembered) {
            (phrase.masteryLevel + 1).coerceAtMost(3)
        } else {
            (phrase.masteryLevel - 1).coerceAtLeast(0)
        }
        viewModelScope.launch {
            repository.updatePhrase(
                phrase.copy(
                    masteryLevel = nextLevel,
                    lastReviewedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deletePhrase(id: Long) {
        viewModelScope.launch {
            repository.deletePhrase(id)
        }
    }

    fun requestPhraseInsight(phrase: PhraseEntity) {
        _insightState.value = InsightUiState.Loading(phrase)
        val nativeLang = preferences.value.nativeLanguageName
        viewModelScope.launch {
            val result = coachService.explainPhrase(
                phrase = phrase.phrase,
                languageName = phrase.languageName,
                nativeLanguageName = nativeLang
            )
            result.onSuccess { insight ->
                _insightState.value = InsightUiState.Ready(phrase, insight)
            }.onFailure { error ->
                _insightState.value = InsightUiState.Error(
                    phrase = phrase,
                    message = error.localizedMessage ?: "Failed to load phrase breakdown."
                )
            }
        }
    }

    fun dismissInsight() {
        _insightState.value = InsightUiState.Hidden
    }
}
