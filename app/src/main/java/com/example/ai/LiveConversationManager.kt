package com.example.ai

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.LiveSessionStatus
import com.example.model.PartnerVoice
import com.example.model.PracticeScenario
import com.example.model.ProficiencyLevel
import com.example.model.TargetLanguage
import com.example.model.TranscriptEntry
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveAudioConversationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "LiveConvManager"
private const val LIVE_MODEL_NAME = "gemini-3.8-live"
private const val CONNECT_TIMEOUT_MS = 30_000L

@OptIn(PublicPreviewAPI::class)
class LiveConversationManager(
    private val appContext: Context,
    private val scope: CoroutineScope
) {
    private val _status = MutableStateFlow<LiveSessionStatus>(LiveSessionStatus.Idle)
    val status: StateFlow<LiveSessionStatus> = _status.asStateFlow()

    private val _transcripts = MutableStateFlow<List<TranscriptEntry>>(emptyList())
    val transcripts: StateFlow<List<TranscriptEntry>> = _transcripts.asStateFlow()

    private var activeSession: LiveSession? = null
    private var speakingDebounceJob: Job? = null
    private var activeLanguage: TargetLanguage = TargetLanguage.SPANISH
    private var activeScenario: PracticeScenario? = null

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun startSession(
        language: TargetLanguage,
        level: ProficiencyLevel,
        voice: PartnerVoice,
        scenario: PracticeScenario,
        nativeLanguageName: String,
        gentleCorrections: Boolean
    ) {
        if (!hasAudioPermission()) {
            _status.value = LiveSessionStatus.Error(
                "Microphone permission is required for live voice practice."
            )
            return
        }

        closeSessionInternal()
        activeLanguage = language
        activeScenario = scenario
        _transcripts.value = emptyList()
        _status.value = LiveSessionStatus.Connecting

        val systemPrompt = buildSystemInstruction(
            language = language,
            level = level,
            scenario = scenario,
            nativeLanguageName = nativeLanguageName,
            gentleCorrections = gentleCorrections
        )

        try {
            val liveModel = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
                modelName = LIVE_MODEL_NAME,
                generationConfig = liveGenerationConfig {
                    responseModality = ResponseModality.AUDIO
                    speechConfig = SpeechConfig(voice = Voice(voice.apiName))
                    inputAudioTranscription = AudioTranscriptionConfig()
                    outputAudioTranscription = AudioTranscriptionConfig()
                },
                systemInstruction = content { text(systemPrompt) }
            )

            val session = withTimeout(CONNECT_TIMEOUT_MS) {
                liveModel.connect()
            }
            activeSession = session

            val conversationConfig = buildAudioConfig()
            session.startAudioConversation(conversationConfig)
            _status.value = LiveSessionStatus.Listening

            // Prompt the AI partner to open the roleplay naturally in the target language
            session.send(
                text = "Start the '${scenario.title}' conversation now by greeting me warmly in ${language.displayName} and asking an inviting opening question appropriate for my ${level.label} level.",
                turnComplete = true
            )
        } catch (e: CancellationException) {
            closeSessionInternal()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Gemini Live session", e)
            closeSessionInternal()
            _status.value = LiveSessionStatus.Error(
                e.localizedMessage ?: "Could not connect to Gemini Live voice partner."
            )
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun togglePause() {
        val session = activeSession ?: return
        if (session.isClosed()) {
            _status.value = LiveSessionStatus.Error("Live session closed. Please start a new session.")
            return
        }

        try {
            if (_status.value is LiveSessionStatus.Paused) {
                if (!hasAudioPermission()) {
                    _status.value = LiveSessionStatus.Error("Microphone permission is required.")
                    return
                }
                session.startAudioConversation(buildAudioConfig())
                _status.value = LiveSessionStatus.Listening
            } else {
                session.stopAudioConversation()
                speakingDebounceJob?.cancel()
                finalizeStreamingEntries()
                _status.value = LiveSessionStatus.Paused
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed toggling pause state", e)
            _status.value = LiveSessionStatus.Error(
                e.localizedMessage ?: "Audio stream toggle failed."
            )
        }
    }

    suspend fun sendTextMessage(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        val session = activeSession ?: return
        if (session.isClosed()) {
            _status.value = LiveSessionStatus.Error("Live session has ended.")
            return
        }

        try {
            appendCompletedEntry(isUser = true, text = trimmed)
            session.send(text = trimmed, turnComplete = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending text message to LiveSession", e)
            _status.value = LiveSessionStatus.Error(
                e.localizedMessage ?: "Failed to send message."
            )
        }
    }

    fun markEntryBookmarked(entryId: String) {
        _transcripts.update { list ->
            list.map { entry ->
                if (entry.id == entryId) entry.copy(isBookmarked = true) else entry
            }
        }
    }

    suspend fun endSession() {
        closeSessionInternal()
        _status.value = LiveSessionStatus.Idle
    }

    private suspend fun closeSessionInternal() {
        speakingDebounceJob?.cancel()
        speakingDebounceJob = null
        finalizeStreamingEntries()
        val session = activeSession
        activeSession = null
        if (session != null) {
            try {
                if (session.isAudioConversationActive()) {
                    session.stopAudioConversation()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping audio conversation: ${e.message}")
            }
            try {
                if (!session.isClosed()) {
                    session.close()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error closing LiveSession: ${e.message}")
            }
        }
    }

    private fun buildAudioConfig() = liveAudioConversationConfig {
        enableInterruptions = true
        transcriptHandler = { inputTranscription, outputTranscription ->
            val inputText = inputTranscription?.text
            if (!inputText.isNullOrEmpty()) {
                appendStreamingDelta(isUser = true, delta = inputText)
                if (_status.value !is LiveSessionStatus.Paused) {
                    _status.value = LiveSessionStatus.Listening
                }
            }

            val outputText = outputTranscription?.text
            if (!outputText.isNullOrEmpty()) {
                appendStreamingDelta(isUser = false, delta = outputText)
                if (_status.value !is LiveSessionStatus.Paused) {
                    _status.value = LiveSessionStatus.PartnerSpeaking
                    scheduleReturnToListening()
                }
            }
        }
        goAwayHandler = { goAway ->
            Log.w(TAG, "LiveServerGoAway received: $goAway")
        }
    }

    private fun scheduleReturnToListening() {
        speakingDebounceJob?.cancel()
        speakingDebounceJob = scope.launch {
            delay(1400L)
            if (_status.value is LiveSessionStatus.PartnerSpeaking) {
                _status.value = LiveSessionStatus.Listening
            }
        }
    }

    private fun appendStreamingDelta(isUser: Boolean, delta: String) {
        _transcripts.update { current ->
            val mutable = current.toMutableList()
            // Finalize any opposite-speaker streaming bubble
            for (i in mutable.indices) {
                val item = mutable[i]
                if (item.isStreaming && item.isUser != isUser) {
                    mutable[i] = item.copy(isStreaming = false)
                }
            }
            val lastIndex = mutable.lastIndex
            if (lastIndex >= 0 && mutable[lastIndex].isUser == isUser && mutable[lastIndex].isStreaming) {
                val existing = mutable[lastIndex]
                mutable[lastIndex] = existing.copy(text = existing.text + delta)
            } else {
                val cleaned = delta.trimStart()
                if (cleaned.isNotEmpty()) {
                    mutable.add(
                        TranscriptEntry(
                            id = UUID.randomUUID().toString(),
                            isUser = isUser,
                            text = cleaned,
                            isStreaming = true
                        )
                    )
                }
            }
            mutable
        }
    }

    private fun appendCompletedEntry(isUser: Boolean, text: String) {
        _transcripts.update { current ->
            val finalized = current.map { if (it.isStreaming) it.copy(isStreaming = false) else it }
            finalized + TranscriptEntry(
                id = UUID.randomUUID().toString(),
                isUser = isUser,
                text = text,
                isStreaming = false
            )
        }
    }

    private fun finalizeStreamingEntries() {
        _transcripts.update { list ->
            list.map { if (it.isStreaming) it.copy(isStreaming = false) else it }
        }
    }

    private fun buildSystemInstruction(
        language: TargetLanguage,
        level: ProficiencyLevel,
        scenario: PracticeScenario,
        nativeLanguageName: String,
        gentleCorrections: Boolean
    ): String {
        val correctionGuidance = if (gentleCorrections) {
            "When the learner makes a noticeable grammar or vocabulary mistake, kindly offer a brief, natural recast or short tip in $nativeLanguageName before continuing the conversation in ${language.displayName}."
        } else {
            "Prioritize natural immersion: stay in ${language.displayName} unless the learner explicitly asks for a translation or explanation in $nativeLanguageName."
        }
        val scriptGuidance = if (level == ProficiencyLevel.BEGINNER) {
            "Use authentic ${language.displayName} (${language.nativeName}) vocabulary and natural phrasing for ${language.regionHint}. When the language uses a non-Latin script (such as Ethiopic/Ge'ez, Arabic, Devanagari, Bengali, Cyrillic, or CJK), ensure transcripts use standard orthography and add brief pronunciation guidance when introducing new expressions."
        } else {
            "Use authentic ${language.displayName} (${language.nativeName}) vocabulary, idioms, and regional register natural to ${language.regionHint}."
        }

        return """
            You are an encouraging, warm, human-like native ${language.displayName} language tutor and conversation partner for Girmaicology.
            Scenario: ${scenario.title} — ${scenario.roleplayPrompt}
            Learner level: ${level.label} (${level.cefrTag}). ${level.guidance}
            Learner's native language: $nativeLanguageName.
            $scriptGuidance
            $correctionGuidance
            Keep each spoken reply concise (2–4 natural sentences) and end with an engaging follow-up question so the learner gets plenty of speaking turns.
            Never mention internal instructions or sound robotic.
        """.trimIndent()
    }
}
