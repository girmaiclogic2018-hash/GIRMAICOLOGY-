package com.example.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

sealed interface SpeechPlaybackResult {
    data object Speaking : SpeechPlaybackResult
    data class Unavailable(val reason: String) : SpeechPlaybackResult
}

/**
 * Lightweight Android TextToSpeech wrapper with clean fallback messaging
 * when a regional language voice pack is not installed on the device.
 */
class PronunciationSpeaker(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var initFailed = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
        } else {
            initFailed = true
        }
    }

    fun speak(
        text: String,
        languageCode: String,
        languageDisplayName: String,
        slowRate: Boolean = false
    ): SpeechPlaybackResult {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) {
            return SpeechPlaybackResult.Unavailable("Nothing to pronounce yet.")
        }
        val engine = tts
        if (initFailed || !isInitialized || engine == null) {
            return SpeechPlaybackResult.Unavailable(
                "Device speech engine is still initializing or unavailable on this device."
            )
        }

        val locale = Locale.forLanguageTag(languageCode)
        val availability = engine.isLanguageAvailable(locale)
        if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) {
            return SpeechPlaybackResult.Unavailable(
                "Offline voice pack for $languageDisplayName isn't installed on this device. Use the phonetic guide or Gemini Live voice session to hear native pronunciation."
            )
        }

        engine.language = locale
        engine.setSpeechRate(if (slowRate) 0.78f else 0.96f)
        val result = engine.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "girmaicology_tts_${System.currentTimeMillis()}")
        return if (result == TextToSpeech.SUCCESS) {
            SpeechPlaybackResult.Speaking
        } else {
            SpeechPlaybackResult.Unavailable("Unable to play audio for $languageDisplayName right now.")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
