package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CoachFeedbackService
import com.example.ai.PronunciationSpeaker
import com.example.ai.SpeechPlaybackResult
import com.example.data.LinguaRepository
import com.example.data.PhraseEntity
import com.example.data.TranslationRecordEntity
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import com.example.model.PracticeDrillSet
import com.example.model.TargetLanguage
import com.example.model.TranslationCoachResult
import com.example.model.TranslationTone
import com.example.model.WritingPolishResult
import com.example.ui.util.UploadMediaReader
import com.example.ui.util.UploadedDocumentPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VaultFilterMode(val label: String) {
    ALL("All"),
    STARRED("Saved"),
    WRITING("Writing"),
    UPLOADS("Uploads")
}

sealed interface BridgeTranslationUiState {
    data object Idle : BridgeTranslationUiState
    data class Loading(val label: String = "Translating...") : BridgeTranslationUiState
    data class Ready(
        val input: String,
        val targetLanguage: TargetLanguage,
        val tone: TranslationTone,
        val result: TranslationCoachResult,
        val recordId: Long? = null,
        val isStarred: Boolean = false,
        val isSavedToPhrasebook: Boolean = false,
        val entryType: String = "TEXT"
    ) : BridgeTranslationUiState
    data class Error(val message: String) : BridgeTranslationUiState
}

sealed interface WritingPolishUiState {
    data object Idle : WritingPolishUiState
    data object Loading : WritingPolishUiState
    data class Ready(
        val draft: String,
        val targetLanguage: TargetLanguage,
        val tone: TranslationTone,
        val result: WritingPolishResult,
        val isSavedToVault: Boolean = false,
        val isSavedToPhrasebook: Boolean = false
    ) : WritingPolishUiState
    data class Error(val message: String) : WritingPolishUiState
}

sealed interface SkillDrillUiState {
    data object Idle : SkillDrillUiState
    data object Loading : SkillDrillUiState
    data class Ready(
        val drillSet: PracticeDrillSet,
        val targetLanguage: TargetLanguage,
        val selectedAnswers: Map<Int, Int> = emptyMap(),
        val savedExpressions: Set<String> = emptySet()
    ) : SkillDrillUiState
    data class Error(val message: String) : SkillDrillUiState
}

val DrillTopics = listOf(
    "Everyday Greetings & Respect",
    "Travel, Transit & Directions",
    "Market, Dining & Coffee Ceremony",
    "Family & Cultural Expressions",
    "Work, Study & Introductions"
)

class BridgeViewModel(
    appContext: Context,
    private val repository: LinguaRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val coachService: CoachFeedbackService = CoachFeedbackService()
) : ViewModel() {

    private val speaker = PronunciationSpeaker(appContext.applicationContext)

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserPreferences())

    private val _sourceLanguage = MutableStateFlow<TargetLanguage?>(null) // null = Auto-detect
    val sourceLanguage: StateFlow<TargetLanguage?> = _sourceLanguage.asStateFlow()

    private val _selectedTone = MutableStateFlow(TranslationTone.STANDARD)
    val selectedTone: StateFlow<TranslationTone> = _selectedTone.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _translationState = MutableStateFlow<BridgeTranslationUiState>(BridgeTranslationUiState.Idle)
    val translationState: StateFlow<BridgeTranslationUiState> = _translationState.asStateFlow()

    private val _writingDraft = MutableStateFlow("")
    val writingDraft: StateFlow<String> = _writingDraft.asStateFlow()

    private val _writingState = MutableStateFlow<WritingPolishUiState>(WritingPolishUiState.Idle)
    val writingState: StateFlow<WritingPolishUiState> = _writingState.asStateFlow()

    private val _vaultSearchQuery = MutableStateFlow("")
    val vaultSearchQuery: StateFlow<String> = _vaultSearchQuery.asStateFlow()

    private val _vaultFilterMode = MutableStateFlow(VaultFilterMode.ALL)
    val vaultFilterMode: StateFlow<VaultFilterMode> = _vaultFilterMode.asStateFlow()

    private val _vaultLanguageFilter = MutableStateFlow<String?>(null)
    val vaultLanguageFilter: StateFlow<String?> = _vaultLanguageFilter.asStateFlow()

    val allTranslations: StateFlow<List<TranslationRecordEntity>> = repository.allTranslations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val filteredTranslations: StateFlow<List<TranslationRecordEntity>> = combine(
        repository.allTranslations,
        _vaultSearchQuery,
        _vaultFilterMode,
        _vaultLanguageFilter
    ) { records, query, mode, langCode ->
        val activeLang = if (langCode != null && records.none { it.targetLanguageCode == langCode }) {
            null
        } else {
            langCode
        }
        records.filter { rec ->
            val matchesMode = when (mode) {
                VaultFilterMode.ALL -> true
                VaultFilterMode.STARRED -> rec.isStarred
                VaultFilterMode.WRITING -> rec.entryType == "WRITING"
                VaultFilterMode.UPLOADS -> rec.entryType == "PHOTO" || rec.entryType == "DOCUMENT"
            }
            val matchesLang = activeLang == null || rec.targetLanguageCode == activeLang
            val matchesQuery = query.isBlank() ||
                rec.sourceText.contains(query, ignoreCase = true) ||
                rec.translatedText.contains(query, ignoreCase = true) ||
                rec.targetLanguageName.contains(query, ignoreCase = true) ||
                rec.sourceLanguageName.contains(query, ignoreCase = true) ||
                rec.note.contains(query, ignoreCase = true)
            matchesMode && matchesLang && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedTopic = MutableStateFlow(DrillTopics.first())
    val selectedTopic: StateFlow<String> = _selectedTopic.asStateFlow()

    private val _drillState = MutableStateFlow<SkillDrillUiState>(SkillDrillUiState.Idle)
    val drillState: StateFlow<SkillDrillUiState> = _drillState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun setSourceLanguage(language: TargetLanguage?) {
        _sourceLanguage.value = language
    }

    fun setTargetLanguage(language: TargetLanguage) {
        viewModelScope.launch { prefsRepo.setTargetLanguage(language) }
    }

    fun setTranslationTone(tone: TranslationTone) {
        _selectedTone.value = tone
    }

    fun swapLanguages(currentTarget: TargetLanguage) {
        val currentSource = _sourceLanguage.value ?: TargetLanguage.ENGLISH
        if (currentSource == currentTarget) return
        _sourceLanguage.value = currentTarget
        viewModelScope.launch { prefsRepo.setTargetLanguage(currentSource) }
        val currentReady = _translationState.value as? BridgeTranslationUiState.Ready
        if (currentReady != null) {
            _inputText.value = currentReady.result.translatedText
        }
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun translateNow() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) {
            _translationState.value = BridgeTranslationUiState.Error("Write or paste a word, sentence, or paragraph to translate.")
            return
        }
        val prefs = preferences.value
        val target = prefs.targetLanguage
        val tone = _selectedTone.value
        val sourceLang = _sourceLanguage.value

        _translationState.value = BridgeTranslationUiState.Loading("Translating to ${target.displayName}...")
        viewModelScope.launch {
            val result = coachService.translateWithCoaching(
                text = text,
                sourceLanguageName = sourceLang?.displayName,
                targetLanguage = target,
                tone = tone,
                explanationLanguageName = prefs.nativeLanguageName
            )
            result.onSuccess { coachResult ->
                publishTranslationSuccess(
                    sourceText = coachResult.extractedSourceText.ifBlank { text },
                    sourceLang = sourceLang,
                    target = target,
                    tone = tone,
                    result = coachResult,
                    entryType = "TEXT"
                )
            }.onFailure { err ->
                _translationState.value = BridgeTranslationUiState.Error(
                    err.localizedMessage ?: "Translation failed. Please try again."
                )
            }
        }
    }

    fun translateImageUri(context: Context, uri: Uri) {
        val prefs = preferences.value
        val target = prefs.targetLanguage
        val tone = _selectedTone.value
        val sourceLang = _sourceLanguage.value

        _translationState.value = BridgeTranslationUiState.Loading("Reading & translating uploaded photo...")
        viewModelScope.launch {
            val bitmapResult = withContext(Dispatchers.IO) {
                UploadMediaReader.readScaledBitmap(context.applicationContext, uri)
            }
            val bitmap = bitmapResult.getOrElse { err ->
                _translationState.value = BridgeTranslationUiState.Error(
                    err.localizedMessage ?: "Could not open selected photo."
                )
                return@launch
            }

            val aiResult = coachService.translateUploadedImage(
                bitmap = bitmap,
                sourceLanguageName = sourceLang?.displayName,
                targetLanguage = target,
                tone = tone,
                explanationLanguageName = prefs.nativeLanguageName
            )
            aiResult.onSuccess { coachResult ->
                val extracted = coachResult.extractedSourceText.ifBlank { "Uploaded image text" }
                _inputText.value = extracted
                publishTranslationSuccess(
                    sourceText = extracted,
                    sourceLang = sourceLang,
                    target = target,
                    tone = tone,
                    result = coachResult,
                    entryType = "PHOTO"
                )
            }.onFailure { err ->
                _translationState.value = BridgeTranslationUiState.Error(
                    err.localizedMessage ?: "Photo translation failed."
                )
            }
        }
    }

    fun translateDocumentUri(context: Context, uri: Uri) {
        val prefs = preferences.value
        val target = prefs.targetLanguage
        val tone = _selectedTone.value
        val sourceLang = _sourceLanguage.value

        _translationState.value = BridgeTranslationUiState.Loading("Reading & translating uploaded file...")
        viewModelScope.launch {
            val payloadResult = withContext(Dispatchers.IO) {
                UploadMediaReader.readDocument(context.applicationContext, uri)
            }
            val payload = payloadResult.getOrElse { err ->
                _translationState.value = BridgeTranslationUiState.Error(
                    err.localizedMessage ?: "Could not read selected file."
                )
                return@launch
            }

            when (payload) {
                is UploadedDocumentPayload.PlainText -> {
                    _inputText.value = payload.text
                    val aiResult = coachService.translateWithCoaching(
                        text = payload.text,
                        sourceLanguageName = sourceLang?.displayName,
                        targetLanguage = target,
                        tone = tone,
                        explanationLanguageName = prefs.nativeLanguageName
                    )
                    aiResult.onSuccess { coachResult ->
                        publishTranslationSuccess(
                            sourceText = payload.text,
                            sourceLang = sourceLang,
                            target = target,
                            tone = tone,
                            result = coachResult.copy(extractedSourceText = payload.text),
                            entryType = "DOCUMENT",
                            noteOverride = "File: ${payload.fileName}"
                        )
                    }.onFailure { err ->
                        _translationState.value = BridgeTranslationUiState.Error(
                            err.localizedMessage ?: "Document translation failed."
                        )
                    }
                }

                is UploadedDocumentPayload.BinaryDocument -> {
                    val aiResult = coachService.translateUploadedDocument(
                        bytes = payload.bytes,
                        mimeType = payload.mimeType,
                        fileName = payload.fileName,
                        sourceLanguageName = sourceLang?.displayName,
                        targetLanguage = target,
                        tone = tone,
                        explanationLanguageName = prefs.nativeLanguageName
                    )
                    aiResult.onSuccess { coachResult ->
                        val extracted = coachResult.extractedSourceText.ifBlank { payload.fileName }
                        _inputText.value = extracted
                        publishTranslationSuccess(
                            sourceText = extracted,
                            sourceLang = sourceLang,
                            target = target,
                            tone = tone,
                            result = coachResult,
                            entryType = "DOCUMENT",
                            noteOverride = "File: ${payload.fileName}"
                        )
                    }.onFailure { err ->
                        _translationState.value = BridgeTranslationUiState.Error(
                            err.localizedMessage ?: "Document translation failed."
                        )
                    }
                }
            }
        }
    }

    private suspend fun publishTranslationSuccess(
        sourceText: String,
        sourceLang: TargetLanguage?,
        target: TargetLanguage,
        tone: TranslationTone,
        result: TranslationCoachResult,
        entryType: String,
        noteOverride: String = ""
    ) {
        val noteText = listOfNotNull(
            noteOverride.takeIf { it.isNotBlank() },
            result.culturalNote.takeIf { it.isNotBlank() }
        ).joinToString(" · ")

        val id = repository.saveTranslation(
            TranslationRecordEntity(
                sourceText = sourceText,
                translatedText = result.translatedText,
                sourceLanguageCode = sourceLang?.code ?: "auto",
                sourceLanguageName = result.detectedSourceLanguage,
                targetLanguageCode = target.code,
                targetLanguageName = target.displayName,
                romanization = result.romanization,
                toneLabel = tone.label,
                note = noteText,
                entryType = entryType,
                isStarred = false
            )
        )
        _translationState.value = BridgeTranslationUiState.Ready(
            input = sourceText,
            targetLanguage = target,
            tone = tone,
            result = result,
            recordId = id,
            isStarred = false,
            isSavedToPhrasebook = false,
            entryType = entryType
        )
    }

    fun toggleStarCurrentTranslation() {
        val ready = _translationState.value as? BridgeTranslationUiState.Ready ?: return
        val recordId = ready.recordId ?: return
        val nextStarred = !ready.isStarred
        viewModelScope.launch {
            val existing = allTranslations.value.firstOrNull { it.id == recordId }
            if (existing != null) {
                repository.updateTranslation(
                    existing.copy(isStarred = nextStarred, updatedAt = System.currentTimeMillis())
                )
            }
            _translationState.update { state ->
                if (state is BridgeTranslationUiState.Ready && state.recordId == recordId) {
                    state.copy(isStarred = nextStarred)
                } else state
            }
            _statusMessage.value = if (nextStarred) "Saved to Translations Vault" else "Removed from Saved"
        }
    }

    fun editCurrentTranslation(newTranslatedText: String, newNote: String) {
        val ready = _translationState.value as? BridgeTranslationUiState.Ready ?: return
        val trimmedText = newTranslatedText.trim()
        if (trimmedText.isEmpty()) return
        val trimmedNote = newNote.trim()

        viewModelScope.launch {
            ready.recordId?.let { id ->
                val existing = allTranslations.value.firstOrNull { it.id == id }
                if (existing != null) {
                    repository.updateTranslation(
                        existing.copy(
                            translatedText = trimmedText,
                            note = trimmedNote,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            _translationState.update { state ->
                if (state is BridgeTranslationUiState.Ready) {
                    state.copy(
                        result = state.result.copy(
                            translatedText = trimmedText,
                            culturalNote = trimmedNote
                        )
                    )
                } else state
            }
            _statusMessage.value = "Translation updated"
        }
    }

    fun saveTranslationToPhrasebook() {
        val ready = _translationState.value as? BridgeTranslationUiState.Ready ?: return
        if (ready.isSavedToPhrasebook) return
        viewModelScope.launch {
            repository.savePhrase(
                PhraseEntity(
                    phrase = ready.result.translatedText,
                    translation = ready.input,
                    languageCode = ready.targetLanguage.code,
                    languageName = ready.targetLanguage.displayName,
                    contextNote = buildString {
                        if (ready.result.romanization.isNotBlank() &&
                            ready.result.romanization != ready.result.translatedText
                        ) {
                            append(ready.result.romanization)
                        }
                        if (ready.result.culturalNote.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(ready.result.culturalNote)
                        }
                    }
                )
            )
            _translationState.update { state ->
                if (state is BridgeTranslationUiState.Ready) state.copy(isSavedToPhrasebook = true) else state
            }
            _statusMessage.value = "Saved \"${ready.result.translatedText}\" to Phrasebook"
        }
    }

    // Writing Composer & Grammar Coach
    fun setWritingDraft(text: String) {
        _writingDraft.value = text
    }

    fun polishWritingNow() {
        val draft = _writingDraft.value.trim()
        if (draft.isEmpty()) {
            _writingState.value = WritingPolishUiState.Error("Write or paste text in any language to proofread or compose.")
            return
        }
        val prefs = preferences.value
        val target = prefs.targetLanguage
        val tone = _selectedTone.value

        _writingState.value = WritingPolishUiState.Loading
        viewModelScope.launch {
            val result = coachService.proofreadAndPolishWriting(
                draftText = draft,
                targetLanguage = target,
                tone = tone,
                explanationLanguageName = prefs.nativeLanguageName
            )
            result.onSuccess { polished ->
                _writingState.value = WritingPolishUiState.Ready(
                    draft = draft,
                    targetLanguage = target,
                    tone = tone,
                    result = polished
                )
            }.onFailure { err ->
                _writingState.value = WritingPolishUiState.Error(
                    err.localizedMessage ?: "Writing coach failed. Please try again."
                )
            }
        }
    }

    fun saveWritingToVault() {
        val ready = _writingState.value as? WritingPolishUiState.Ready ?: return
        if (ready.isSavedToVault) return
        viewModelScope.launch {
            repository.saveTranslation(
                TranslationRecordEntity(
                    sourceText = ready.draft,
                    translatedText = ready.result.correctedText,
                    sourceLanguageCode = "draft",
                    sourceLanguageName = "Writing Draft",
                    targetLanguageCode = ready.targetLanguage.code,
                    targetLanguageName = ready.targetLanguage.displayName,
                    romanization = ready.result.romanization,
                    toneLabel = ready.tone.label,
                    note = ready.result.nativeTranslation,
                    entryType = "WRITING",
                    isStarred = true
                )
            )
            _writingState.update { state ->
                if (state is WritingPolishUiState.Ready) state.copy(isSavedToVault = true) else state
            }
            _statusMessage.value = "Saved polished writing to Vault"
        }
    }

    fun saveWritingToPhrasebook() {
        val ready = _writingState.value as? WritingPolishUiState.Ready ?: return
        if (ready.isSavedToPhrasebook) return
        viewModelScope.launch {
            repository.savePhrase(
                PhraseEntity(
                    phrase = ready.result.correctedText,
                    translation = ready.result.nativeTranslation.ifBlank { ready.draft },
                    languageCode = ready.targetLanguage.code,
                    languageName = ready.targetLanguage.displayName,
                    contextNote = ready.result.romanization
                )
            )
            _writingState.update { state ->
                if (state is WritingPolishUiState.Ready) state.copy(isSavedToPhrasebook = true) else state
            }
            _statusMessage.value = "Saved polished expression to Phrasebook"
        }
    }

    // Vault Search, Save/Star, Edit, Delete, Restore
    fun setVaultSearchQuery(query: String) {
        _vaultSearchQuery.value = query
    }

    fun setVaultFilterMode(mode: VaultFilterMode) {
        _vaultFilterMode.value = mode
    }

    fun setVaultLanguageFilter(code: String?) {
        _vaultLanguageFilter.value = code
    }

    fun clearVaultFilters() {
        _vaultSearchQuery.value = ""
        _vaultFilterMode.value = VaultFilterMode.ALL
        _vaultLanguageFilter.value = null
    }

    fun toggleStarRecord(record: TranslationRecordEntity) {
        viewModelScope.launch {
            repository.updateTranslation(
                record.copy(isStarred = !record.isStarred, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun updateSavedRecord(
        record: TranslationRecordEntity,
        newSource: String,
        newTranslated: String,
        newRomanization: String,
        newNote: String
    ) {
        if (newSource.isBlank() || newTranslated.isBlank()) return
        viewModelScope.launch {
            repository.updateTranslation(
                record.copy(
                    sourceText = newSource.trim(),
                    translatedText = newTranslated.trim(),
                    romanization = newRomanization.trim(),
                    note = newNote.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _statusMessage.value = "Saved translation updated"
        }
    }

    fun deleteSavedRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteTranslation(id)
            _statusMessage.value = "Translation deleted"
        }
    }

    fun saveRecordToPhrasebook(record: TranslationRecordEntity) {
        viewModelScope.launch {
            repository.savePhrase(
                PhraseEntity(
                    phrase = record.translatedText,
                    translation = record.sourceText,
                    languageCode = record.targetLanguageCode,
                    languageName = record.targetLanguageName,
                    contextNote = listOf(record.romanization, record.note)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                )
            )
            _statusMessage.value = "Saved to Phrasebook"
        }
    }

    fun loadRecordIntoTranslator(record: TranslationRecordEntity) {
        _inputText.value = record.sourceText
        val targetLang = TargetLanguage.entries.firstOrNull { it.code == record.targetLanguageCode }
        if (targetLang != null) {
            viewModelScope.launch { prefsRepo.setTargetLanguage(targetLang) }
        }
    }

    fun speakText(text: String, language: TargetLanguage, slowRate: Boolean = false) {
        when (val result = speaker.speak(text, language.code, language.displayName, slowRate)) {
            is SpeechPlaybackResult.Speaking -> Unit
            is SpeechPlaybackResult.Unavailable -> {
                _statusMessage.value = result.reason
            }
        }
    }

    fun setDrillTopic(topic: String) {
        _selectedTopic.value = topic
    }

    fun generateDrill() {
        val prefs = preferences.value
        val target = prefs.targetLanguage
        val topic = _selectedTopic.value

        _drillState.value = SkillDrillUiState.Loading
        viewModelScope.launch {
            val result = coachService.generateSkillDrill(
                language = target,
                level = prefs.proficiencyLevel,
                topic = topic,
                explanationLanguageName = prefs.nativeLanguageName
            )
            result.onSuccess { set ->
                _drillState.value = SkillDrillUiState.Ready(
                    drillSet = set,
                    targetLanguage = target
                )
            }.onFailure { err ->
                _drillState.value = SkillDrillUiState.Error(
                    err.localizedMessage ?: "Could not generate skill drill."
                )
            }
        }
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        _drillState.update { state ->
            if (state is SkillDrillUiState.Ready && !state.selectedAnswers.containsKey(questionIndex)) {
                state.copy(selectedAnswers = state.selectedAnswers + (questionIndex to optionIndex))
            } else state
        }
    }

    fun saveDrillExpression(expression: String, meaning: String, note: String) {
        val ready = _drillState.value as? SkillDrillUiState.Ready ?: return
        if (expression.isBlank() || ready.savedExpressions.contains(expression)) return
        viewModelScope.launch {
            repository.savePhrase(
                PhraseEntity(
                    phrase = expression,
                    translation = meaning,
                    languageCode = ready.targetLanguage.code,
                    languageName = ready.targetLanguage.displayName,
                    contextNote = note
                )
            )
            _drillState.update { state ->
                if (state is SkillDrillUiState.Ready) {
                    state.copy(savedExpressions = state.savedExpressions + expression)
                } else state
            }
            _statusMessage.value = "Saved \"$expression\" to Phrasebook"
        }
    }

    fun showStatusMessage(message: String) {
        _statusMessage.value = message
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        speaker.shutdown()
    }
}
