package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LinguaRepository
import com.example.data.PracticeStats
import com.example.data.SessionRecordEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: LinguaRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLanguageFilter = MutableStateFlow<String?>(null)
    val selectedLanguageFilter: StateFlow<String?> = _selectedLanguageFilter.asStateFlow()

    val progressStats: StateFlow<PracticeStats> = combine(
        repository.observeStats(),
        repository.allPhrases
    ) { stats, phrases ->
        stats.copy(totalSavedPhrases = phrases.size)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PracticeStats()
    )

    val filteredSessions: StateFlow<List<SessionRecordEntity>> = combine(
        repository.allSessions,
        _searchQuery,
        _selectedLanguageFilter
    ) { sessions, query, langCode ->
        sessions.filter { session ->
            val matchesLang = langCode == null || session.languageCode == langCode
            val matchesQuery = query.isBlank() ||
                session.scenarioTitle.contains(query, ignoreCase = true) ||
                session.languageName.contains(query, ignoreCase = true) ||
                session.aiSummaryFeedback.contains(query, ignoreCase = true) ||
                session.transcriptText.contains(query, ignoreCase = true)
            matchesLang && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLanguageFilter(languageCode: String?) {
        _selectedLanguageFilter.value = languageCode
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            repository.deleteSession(id)
        }
    }
}
