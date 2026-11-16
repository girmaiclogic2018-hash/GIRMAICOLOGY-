package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CoachFeedbackService
import com.example.ai.LiveConversationManager
import com.example.data.LinguaRepository
import com.example.data.PhraseEntity
import com.example.data.PracticeStats
import com.example.data.SessionRecordEntity
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import com.example.model.LiveSessionStatus
import com.example.model.PartnerVoice
import com.example.model.PracticeScenario
import com.example.model.PresetScenarios
import com.example.model.ProficiencyLevel
import com.example.model.SessionReview
import com.example.model.SuggestedPhrase
import com.example.model.TargetLanguage
import com.example.model.ThemePreference
import com.example.model.TranscriptEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ReviewUiState {
    data object Hidden : ReviewUiState
    data object Loading : ReviewUiState
    data class Ready(
        val review: SessionReview,
        val savedPhrases: Set<String> = emptySet()
    ) : ReviewUiState
    data class Error(val message: String) : ReviewUiState
}

private const val TAG = "StudioViewModel"

class StudioViewModel(
    appContext: Context,
    private val repository: LinguaRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val coachService: CoachFeedbackService = CoachFeedbackService()
) : ViewModel() {

    private val liveManager = LiveConversationManager(
        appContext = appContext.applicationContext,
        scope = viewModelScope
    )

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferences()
        )

    val stats: StateFlow<PracticeStats> = combine(
        repository.observeStats(),
        repository.allPhrases
    ) { practiceStats, phrases ->
        practiceStats.copy(totalSavedPhrases = phrases.size)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PracticeStats()
    )

    private val _selectedScenario = MutableStateFlow(PresetScenarios.all.first())
    val selectedScenario: StateFlow<PracticeScenario> = _selectedScenario.asStateFlow()

    private val _isSessionVisible = MutableStateFlow(false)
    val isSessionVisible: StateFlow<Boolean> = _isSessionVisible.asStateFlow()

    val liveStatus: StateFlow<LiveSessionStatus> = liveManager.status
    val transcripts: StateFlow<List<TranscriptEntry>> = liveManager.transcripts

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _reviewState = MutableStateFlow<ReviewUiState>(ReviewUiState.Hidden)
    val reviewState: StateFlow<ReviewUiState> = _reviewState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var timerJob: Job? = null
    private var activeSessionLanguage: TargetLanguage = TargetLanguage.SPANISH
    private var activeSessionScenario: PracticeScenario = PresetScenarios.all.first()
    private var activeSessionLevel: ProficiencyLevel = ProficiencyLevel.BEGINNER

    fun hasAudioPermission(): Boolean = liveManager.hasAudioPermission()

    fun selectLanguage(language: TargetLanguage) {
        viewModelScope.launch { prefsRepo.setTargetLanguage(language) }
    }

    fun selectLevel(level: ProficiencyLevel) {
        viewModelScope.launch { prefsRepo.setProficiencyLevel(level) }
    }

    fun selectVoice(voice: PartnerVoice) {
        viewModelScope.launch { prefsRepo.setPartnerVoice(voice) }
    }

    fun selectScenario(scenario: PracticeScenario) {
        _selectedScenario.value = scenario
    }

    fun setNativeLanguage(name: String) {
        viewModelScope.launch { prefsRepo.setNativeLanguage(name) }
    }

    fun setGentleCorrections(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setGentleCorrections(enabled) }
    }

    fun setDailyGoalMinutes(minutes: Int) {
        viewModelScope.launch { prefsRepo.setDailyGoalMinutes(minutes) }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch { prefsRepo.setThemePreference(theme) }
    }

    fun openSessionScreen() {
        _isSessionVisible.value = true
    }

    fun launchPracticeFor(languageCode: String, scenarioTitle: String?) {
        val targetLang = TargetLanguage.entries.firstOrNull { it.code == languageCode }
        if (targetLang != null) {
            selectLanguage(targetLang)
        }
        if (!scenarioTitle.isNullOrBlank()) {
            val matchedScenario = PresetScenarios.all.firstOrNull { it.title == scenarioTitle }
            if (matchedScenario != null) {
                _selectedScenario.value = matchedScenario
            }
        }
    }

    fun startLiveSession(scenario: PracticeScenario = _selectedScenario.value) {
        if (_isSessionVisible.value && liveStatus.value is LiveSessionStatus.Connecting) {
            return
        }
        _selectedScenario.value = scenario
        _isSessionVisible.value = true
        _reviewState.value = ReviewUiState.Hidden
        _elapsedSeconds.value = 0L

        val currentPrefs = preferences.value
        activeSessionLanguage = currentPrefs.targetLanguage
        activeSessionScenario = scenario
        activeSessionLevel = currentPrefs.proficiencyLevel

        startTimer()
        viewModelScope.launch {
            liveManager.startSession(
                language = currentPrefs.targetLanguage,
                level = currentPrefs.proficiencyLevel,
                voice = currentPrefs.partnerVoice,
                scenario = scenario,
                nativeLanguageName = currentPrefs.nativeLanguageName,
                gentleCorrections = currentPrefs.gentleCorrections
            )
        }
    }

    fun togglePause() {
        viewModelScope.launch {
            liveManager.togglePause()
        }
    }

    fun sendTextMessage(message: String) {
        viewModelScope.launch {
            liveManager.sendTextMessage(message)
        }
    }

    fun bookmarkTranscriptEntry(entry: TranscriptEntry) {
        if (entry.isBookmarked) return
        liveManager.markEntryBookmarked(entry.id)
        val currentPrefs = preferences.value
        viewModelScope.launch {
            val result = coachService.extractPhraseBookmark(
                utterance = entry.text,
                language = activeSessionLanguage,
                nativeLanguageName = currentPrefs.nativeLanguageName
            )
            result.onSuccess { extracted ->
                repository.savePhrase(
                    PhraseEntity(
                        phrase = extracted.phrase,
                        translation = extracted.translation,
                        languageCode = activeSessionLanguage.code,
                        languageName = activeSessionLanguage.displayName,
                        contextNote = extracted.note
                    )
                )
                _snackbarMessage.value = "Saved \"${extracted.phrase}\" to Phrasebook"
            }.onFailure { error ->
                Log.w(TAG, "Bookmark extraction fallback", error)
                repository.savePhrase(
                    PhraseEntity(
                        phrase = entry.text.take(120),
                        translation = "Saved from ${activeSessionScenario.title}",
                        languageCode = activeSessionLanguage.code,
                        languageName = activeSessionLanguage.displayName,
                        contextNote = ""
                    )
                )
                _snackbarMessage.value = "Saved utterance to Phrasebook"
            }
        }
    }

    fun endLiveSession() {
        stopTimer()
        val snapshotTranscript = transcripts.value.map { it.copy(isStreaming = false) }
        val duration = _elapsedSeconds.value
        val language = activeSessionLanguage
        val scenario = activeSessionScenario
        val level = activeSessionLevel
        val nativeLang = preferences.value.nativeLanguageName

        viewModelScope.launch {
            liveManager.endSession()
            _isSessionVisible.value = false

            if (snapshotTranscript.isNotEmpty() || duration >= 5L) {
                val formattedTranscript = snapshotTranscript.joinToString("\n") { entry ->
                    val label = if (entry.isUser) "You" else "Partner"
                    "$label: ${entry.text}"
                }
                val sessionId = repository.saveSession(
                    SessionRecordEntity(
                        languageCode = language.code,
                        languageName = language.displayName,
                        scenarioTitle = scenario.title,
                        levelLabel = level.label,
                        durationSeconds = duration,
                        turnCount = snapshotTranscript.size,
                        aiSummaryFeedback = "Practice session (${snapshotTranscript.size} turns)",
                        transcriptText = formattedTranscript
                    )
                )

                if (snapshotTranscript.size >= 2) {
                    _reviewState.value = ReviewUiState.Loading
                    val reviewResult = coachService.generateSessionReview(
                        language = language,
                        nativeLanguageName = nativeLang,
                        scenarioTitle = scenario.title,
                        transcript = snapshotTranscript
                    )
                    reviewResult.onSuccess { review ->
                        try {
                            repository.updateSessionReview(
                                id = sessionId,
                                feedback = review.fluencySummary,
                                strengths = review.strengths.joinToString("\n• ", prefix = "• "),
                                grammarTip = review.grammarTip
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed updating session review", e)
                        }
                        _reviewState.value = ReviewUiState.Ready(review)
                    }.onFailure { err ->
                        _reviewState.value = ReviewUiState.Error(
                            err.localizedMessage ?: "Could not generate post-session review."
                        )
                    }
                }
            }
        }
    }

    fun closeSessionWithoutSaving() {
        stopTimer()
        viewModelScope.launch {
            liveManager.endSession()
            _isSessionVisible.value = false
        }
    }

    fun saveSuggestedPhrase(suggested: SuggestedPhrase) {
        val lang = activeSessionLanguage
        viewModelScope.launch {
            repository.savePhrase(
                PhraseEntity(
                    phrase = suggested.phrase,
                    translation = suggested.translation,
                    languageCode = lang.code,
                    languageName = lang.displayName,
                    contextNote = suggested.note
                )
            )
            _reviewState.update { state ->
                if (state is ReviewUiState.Ready) {
                    state.copy(savedPhrases = state.savedPhrases + suggested.phrase)
                } else state
            }
            _snackbarMessage.value = "Saved \"${suggested.phrase}\" to Phrasebook"
        }
    }

    fun dismissReview() {
        _reviewState.value = ReviewUiState.Hidden
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun clearAllLocalData() {
        viewModelScope.launch {
            repository.clearAllData()
            _snackbarMessage.value = "Cleared saved phrases and session history"
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val status = liveStatus.value
                if (status is LiveSessionStatus.Listening || status is LiveSessionStatus.PartnerSpeaking) {
                    _elapsedSeconds.update { it + 1L }
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
