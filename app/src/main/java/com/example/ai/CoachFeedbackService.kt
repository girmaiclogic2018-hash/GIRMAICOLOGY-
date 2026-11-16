package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.PhraseInsight
import com.example.model.PracticeDrillSet
import com.example.model.PracticeQuestion
import com.example.model.ProficiencyLevel
import com.example.model.SessionReview
import com.example.model.SuggestedPhrase
import com.example.model.TargetLanguage
import com.example.model.TranscriptEntry
import com.example.model.TranslationCoachResult
import com.example.model.TranslationTone
import com.example.model.WritingPolishResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "CoachFeedbackService"
private const val FLASH_MODEL_NAME = "gemini-3.8-flash"
private const val REQUEST_TIMEOUT_MS = 60_000L

class CoachFeedbackService {

    private val reviewSchema = Schema.obj(
        properties = mapOf(
            "fluencySummary" to Schema.string(description = "2-sentence encouraging summary of how the learner performed"),
            "strengths" to Schema.array(
                items = Schema.string(),
                description = "2 to 3 specific strengths observed in the learner's turns"
            ),
            "grammarTip" to Schema.string(description = "1 concise, actionable grammar or phrasing tip based on the conversation"),
            "suggestedPhrases" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "phrase" to Schema.string(description = "Useful expression in the target language"),
                        "translation" to Schema.string(description = "Natural translation in the learner's native language"),
                        "note" to Schema.string(description = "Brief usage or pronunciation note")
                    )
                ),
                description = "3 useful phrases from or relevant to this conversation"
            )
        )
    )

    private val phraseInsightSchema = Schema.obj(
        properties = mapOf(
            "literalMeaning" to Schema.string(description = "Natural meaning plus literal breakdown"),
            "pronunciationGuide" to Schema.string(description = "Clear phonetic syllable guide"),
            "exampleSentence" to Schema.string(description = "A natural everyday example sentence in the target language"),
            "exampleTranslation" to Schema.string(description = "Translation of the example sentence"),
            "culturalTip" to Schema.string(description = "Short register or cultural nuance tip")
        )
    )

    private val quickTranslateSchema = Schema.obj(
        properties = mapOf(
            "phrase" to Schema.string(description = "The key phrase or sentence in the target language"),
            "translation" to Schema.string(description = "Concise translation in the learner's native language"),
            "contextNote" to Schema.string(description = "Short usage or grammar note")
        )
    )

    private val bridgeTranslationSchema = Schema.obj(
        properties = mapOf(
            "detectedSourceLanguage" to Schema.string(description = "The identified source language name"),
            "extractedSourceText" to Schema.string(description = "Original source text (or transcribed text from image/document)"),
            "translatedText" to Schema.string(description = "Accurate, natural translation in the target language using standard native script"),
            "romanization" to Schema.string(description = "Latin-script transliteration/romanization (or same as translatedText if already Latin script)"),
            "partOfSpeech" to Schema.string(description = "Short grammatical category or expression type such as Noun, Verb Phrase, Greeting, Question, Paragraph"),
            "alternativeTranslations" to Schema.array(
                items = Schema.string(),
                description = "2 to 4 accurate synonyms or alternative ways to express the same meaning in the target language"
            ),
            "phoneticGuide" to Schema.string(description = "Easy syllable-by-syllable pronunciation guide for learners"),
            "politeVariant" to Schema.string(description = "Respectful or formal phrasing in the target language"),
            "casualVariant" to Schema.string(description = "Friendly everyday phrasing in the target language"),
            "exampleSentence" to Schema.string(description = "A natural example sentence using the translated expression"),
            "exampleTranslation" to Schema.string(description = "Translation of the example sentence in the explanation language"),
            "culturalNote" to Schema.string(description = "Concise cultural, grammar, or regional usage note")
        ),
        optionalProperties = listOf("extractedSourceText", "partOfSpeech", "alternativeTranslations")
    )

    private val writingPolishSchema = Schema.obj(
        properties = mapOf(
            "correctedText" to Schema.string(description = "Proofread, grammatically accurate, natural text in the target language"),
            "nativeTranslation" to Schema.string(description = "Clear translation of the polished text in the learner's explanation language"),
            "romanization" to Schema.string(description = "Latin-script phonetic reading of correctedText"),
            "toneSummary" to Schema.string(description = "Brief note describing how the tone and flow read"),
            "improvements" to Schema.array(
                items = Schema.string(),
                description = "2 to 4 concise explanations of grammar, spelling, or phrasing fixes made"
            ),
            "alternativeVersion" to Schema.string(description = "An optional second stylistic phrasing in the target language")
        ),
        optionalProperties = listOf("romanization", "alternativeVersion")
    )

    private val skillDrillSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "prompt" to Schema.string(description = "Clear real-world question or situation prompt in the learner's explanation language"),
                        "targetExpression" to Schema.string(description = "Short expression or clue in the target language (native script)"),
                        "romanization" to Schema.string(description = "Phonetic reading or transliteration of targetExpression"),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 plausible answer choices"
                        ),
                        "correctIndex" to Schema.integer(description = "0-based index (0..3) of the best answer in options"),
                        "explanation" to Schema.string(description = "Short, clear explanation of why the correct option works best")
                    )
                ),
                description = "4 engaging multiple-choice practice questions"
            )
        )
    )

    private val reviewModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FLASH_MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reviewSchema
            }
        )
    }

    private val phraseInsightModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FLASH_MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = phraseInsightSchema
            }
        )
    }

    private val quickTranslateModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FLASH_MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quickTranslateSchema
            }
        )
    }

    private val bridgeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FLASH_MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = bridgeTranslationSchema
            }
        )
    }

    private val drillModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FLASH_MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = skillDrillSchema
            }
        )
    }

    private val writingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FLASH_MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = writingPolishSchema
            }
        )
    }

    suspend fun generateSessionReview(
        language: TargetLanguage,
        nativeLanguageName: String,
        scenarioTitle: String,
        transcript: List<TranscriptEntry>
    ): Result<SessionReview> {
        if (transcript.isEmpty()) {
            return Result.failure(IllegalArgumentException("No conversation turns to review."))
        }
        val formattedTranscript = transcript.joinToString("\n") { entry ->
            val speaker = if (entry.isUser) "Learner" else "Partner"
            "$speaker: ${entry.text}"
        }
        val prompt = """
            Analyze this ${language.displayName} (${language.nativeName}, ${language.regionHint}) language practice conversation
            (scenario: "$scenarioTitle") for a learner whose native language is $nativeLanguageName.
            Provide encouraging, specific feedback and 3 high-value phrases to save to their phrasebook.

            Transcript:
            $formattedTranscript
        """.trimIndent()

        return try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                reviewModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Model returned an empty review."))
            val json = JSONObject(jsonText)
            val strengthsArr = json.optJSONArray("strengths") ?: JSONArray()
            val strengths = (0 until strengthsArr.length())
                .mapNotNull { strengthsArr.optString(it).takeIf { s -> s.isNotBlank() } }
            val phrasesArr = json.optJSONArray("suggestedPhrases") ?: JSONArray()
            val phrases = (0 until phrasesArr.length()).mapNotNull { idx ->
                val obj = phrasesArr.optJSONObject(idx) ?: return@mapNotNull null
                val phraseText = obj.optString("phrase").trim()
                val translationText = obj.optString("translation").trim()
                if (phraseText.isEmpty() || translationText.isEmpty()) null
                else SuggestedPhrase(
                    phrase = phraseText,
                    translation = translationText,
                    note = obj.optString("note").trim()
                )
            }
            Result.success(
                SessionReview(
                    fluencySummary = json.optString("fluencySummary", "Great speaking practice!"),
                    strengths = strengths,
                    grammarTip = json.optString("grammarTip", "Keep practicing natural everyday sentences."),
                    suggestedPhrases = phrases
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Session review timed out", e)
            Result.failure(Exception("Session review timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate session review", e)
            Result.failure(e)
        }
    }

    suspend fun extractPhraseBookmark(
        utterance: String,
        language: TargetLanguage,
        nativeLanguageName: String
    ): Result<SuggestedPhrase> {
        val prompt = """
            From this ${language.displayName} (${language.nativeName}) utterance, extract the single most useful conversational phrase or keep the sentence concise, and translate it into $nativeLanguageName:
            "$utterance"
        """.trimIndent()

        return try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                quickTranslateModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Empty response from model."))
            val json = JSONObject(jsonText)
            val phrase = json.optString("phrase").ifBlank { utterance.take(120) }
            val translation = json.optString("translation").ifBlank { "Saved expression" }
            Result.success(
                SuggestedPhrase(
                    phrase = phrase,
                    translation = translation,
                    note = json.optString("contextNote")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Bookmark extraction timed out", e)
            Result.failure(Exception("Translation timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract phrase bookmark", e)
            Result.failure(e)
        }
    }

    suspend fun explainPhrase(
        phrase: String,
        languageName: String,
        nativeLanguageName: String
    ): Result<PhraseInsight> {
        val prompt = """
            Explain the $languageName phrase "$phrase" to a $nativeLanguageName speaker.
            Include its natural & literal meaning, clear phonetic pronunciation guide, a fresh example sentence with translation, and a cultural usage tip.
        """.trimIndent()

        return try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                phraseInsightModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Model returned empty insight."))
            val json = JSONObject(jsonText)
            Result.success(
                PhraseInsight(
                    literalMeaning = json.optString("literalMeaning"),
                    pronunciationGuide = json.optString("pronunciationGuide"),
                    exampleSentence = json.optString("exampleSentence"),
                    exampleTranslation = json.optString("exampleTranslation"),
                    culturalTip = json.optString("culturalTip")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Phrase insight timed out", e)
            Result.failure(Exception("Phrase breakdown timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate phrase insight", e)
            Result.failure(e)
        }
    }

    suspend fun translateWithCoaching(
        text: String,
        sourceLanguageName: String?,
        targetLanguage: TargetLanguage,
        tone: TranslationTone = TranslationTone.STANDARD,
        explanationLanguageName: String
    ): Result<TranslationCoachResult> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a word or sentence to translate."))
        }
        val sourceClause = if (sourceLanguageName.isNullOrBlank()) {
            "Detect the source language automatically."
        } else {
            "Source language: $sourceLanguageName."
        }

        val prompt = """
            You are Girmaicology's flagship multilingual translator (precision comparable to top global translators) and pronunciation coach.
            $sourceClause
            Target language: ${targetLanguage.displayName} (${targetLanguage.nativeName}, ${targetLanguage.regionHint}).
            Requested translation tone/register: ${tone.label} — ${tone.instruction}
            Provide all explanations, partOfSpeech, and exampleTranslation in $explanationLanguageName.
            Translate the input with complete semantic accuracy, idiomatic fluency, and authentic script orthography (Fidel/Ge'ez, Arabic, Devanagari, CJK, Cyrillic, Latin, etc.).
            Also provide 2-4 alternativeTranslations (synonyms/contextual variants), romanization, syllable phoneticGuide, politeVariant, casualVariant, a natural exampleSentence, and a culturalNote.
            Set extractedSourceText to the original input text.

            Input text:
            "$trimmed"
        """.trimIndent()

        return try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                bridgeModel.generateContent(prompt)
            }
            parseBridgeTranslation(response.text, sourceLanguageName ?: "Detected", trimmed)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Bridge translation timed out", e)
            Result.failure(Exception("Translation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Bridge translation failed", e)
            Result.failure(e)
        }
    }

    suspend fun translateUploadedImage(
        bitmap: Bitmap,
        sourceLanguageName: String?,
        targetLanguage: TargetLanguage,
        tone: TranslationTone = TranslationTone.STANDARD,
        explanationLanguageName: String
    ): Result<TranslationCoachResult> {
        val sourceClause = if (sourceLanguageName.isNullOrBlank()) {
            "Detect the language shown in the image automatically."
        } else {
            "Expected source language in the image: $sourceLanguageName."
        }

        val prompt = """
            Transcribe all readable text from this uploaded image accurately into extractedSourceText, then translate it into ${targetLanguage.displayName} (${targetLanguage.nativeName}, ${targetLanguage.regionHint}).
            $sourceClause
            Requested tone: ${tone.label} — ${tone.instruction}
            Provide all explanations and exampleTranslation in $explanationLanguageName.
            If there is no readable text in the image, describe briefly what text/signs appear missing in culturalNote and leave translatedText empty.
        """.trimIndent()

        return try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                bridgeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
            }
            parseBridgeTranslation(response.text, sourceLanguageName ?: "Image Text", "")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image translation timed out", e)
            Result.failure(Exception("Image translation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image translation failed", e)
            Result.failure(e)
        }
    }

    suspend fun translateUploadedDocument(
        bytes: ByteArray,
        mimeType: String,
        fileName: String,
        sourceLanguageName: String?,
        targetLanguage: TargetLanguage,
        tone: TranslationTone = TranslationTone.STANDARD,
        explanationLanguageName: String
    ): Result<TranslationCoachResult> {
        val sourceClause = if (sourceLanguageName.isNullOrBlank()) {
            "Detect the source language of the document automatically."
        } else {
            "Expected source language: $sourceLanguageName."
        }

        val prompt = """
            Read this uploaded document ("$fileName"), extract its key text into extractedSourceText (summarizing or quoting concisely if long), and translate it faithfully into ${targetLanguage.displayName} (${targetLanguage.nativeName}, ${targetLanguage.regionHint}).
            $sourceClause
            Requested tone: ${tone.label} — ${tone.instruction}
            Provide explanations and exampleTranslation in $explanationLanguageName.
        """.trimIndent()

        return try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                bridgeModel.generateContent(
                    content {
                        inlineData(bytes, mimeType)
                        text(prompt)
                    }
                )
            }
            parseBridgeTranslation(response.text, sourceLanguageName ?: "Document", fileName)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Document translation timed out", e)
            Result.failure(Exception("Document translation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Document translation failed", e)
            Result.failure(e)
        }
    }

    suspend fun proofreadAndPolishWriting(
        draftText: String,
        targetLanguage: TargetLanguage,
        tone: TranslationTone,
        explanationLanguageName: String
    ): Result<WritingPolishResult> {
        val trimmed = draftText.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Write or paste a draft first."))
        }

        val prompt = """
            You are Girmaicology's expert writing coach for ${targetLanguage.displayName} (${targetLanguage.nativeName}, ${targetLanguage.regionHint}).
            A learner wrote this draft:
            "$trimmed"

            If the draft is in another language (e.g., $explanationLanguageName) or has spelling/grammar errors, rewrite and polish it cleanly into authentic, native-grade ${targetLanguage.displayName} in ${tone.label} tone (${tone.instruction}).
            If the draft is already in ${targetLanguage.displayName}, fix grammar, spelling, diacritics/Fidel characters, and natural phrasing.
            Provide nativeTranslation, toneSummary, and 2-4 improvements explanations in $explanationLanguageName, plus romanization and an alternativeVersion.
        """.trimIndent()

        return try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                writingModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Writing coach returned an empty response."))
            val json = JSONObject(jsonText)
            val corrected = json.optString("correctedText").trim()
            if (corrected.isEmpty()) {
                return Result.failure(IllegalStateException("Could not polish this draft. Please try again."))
            }
            val impArr = json.optJSONArray("improvements") ?: JSONArray()
            val improvements = (0 until impArr.length())
                .mapNotNull { impArr.optString(it).trim().takeIf { s -> s.isNotEmpty() } }

            Result.success(
                WritingPolishResult(
                    correctedText = corrected,
                    nativeTranslation = json.optString("nativeTranslation").trim(),
                    romanization = json.optString("romanization").trim(),
                    toneSummary = json.optString("toneSummary").trim(),
                    improvements = improvements,
                    alternativeVersion = json.optString("alternativeVersion").trim()
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Writing polish timed out", e)
            Result.failure(Exception("Writing review timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Writing polish failed", e)
            Result.failure(e)
        }
    }

    private fun parseBridgeTranslation(
        jsonText: String?,
        fallbackSource: String,
        fallbackExtracted: String
    ): Result<TranslationCoachResult> {
        val raw = jsonText ?: return Result.failure(IllegalStateException("Translation returned an empty response."))
        val json = JSONObject(raw)
        val translated = json.optString("translatedText").trim()
        if (translated.isEmpty()) {
            return Result.failure(IllegalStateException("Could not extract or produce a translation from that input."))
        }
        val altArray = json.optJSONArray("alternativeTranslations") ?: JSONArray()
        val alternatives = (0 until altArray.length())
            .mapNotNull { altArray.optString(it).trim().takeIf { s -> s.isNotEmpty() && s != translated } }
            .distinct()

        return Result.success(
            TranslationCoachResult(
                detectedSourceLanguage = json.optString("detectedSourceLanguage", fallbackSource).ifBlank { fallbackSource },
                translatedText = translated,
                romanization = json.optString("romanization", translated),
                partOfSpeech = json.optString("partOfSpeech").trim(),
                alternativeTranslations = alternatives,
                phoneticGuide = json.optString("phoneticGuide").trim(),
                politeVariant = json.optString("politeVariant").trim(),
                casualVariant = json.optString("casualVariant").trim(),
                exampleSentence = json.optString("exampleSentence").trim(),
                exampleTranslation = json.optString("exampleTranslation").trim(),
                culturalNote = json.optString("culturalNote").trim(),
                extractedSourceText = json.optString("extractedSourceText", fallbackExtracted).trim()
            )
        )
    }

    suspend fun generateSkillDrill(
        language: TargetLanguage,
        level: ProficiencyLevel,
        topic: String,
        explanationLanguageName: String
    ): Result<PracticeDrillSet> {
        val prompt = """
            Create a 4-question interactive language practice drill for Girmaicology.
            Target language: ${language.displayName} (${language.nativeName}, ${language.regionHint}).
            Learner proficiency: ${level.label} (${level.cefrTag}).
            Topic: $topic.
            Write the question prompts, answer explanations, and English/native-language choices clearly in $explanationLanguageName, while featuring authentic ${language.displayName} expressions in standard script plus romanization so learners master real spoken & written communication.
            Ensure each question has exactly 4 options and a valid 0..3 correctIndex.
        """.trimIndent()

        return try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                drillModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Practice drill returned an empty response."))
            val json = JSONObject(jsonText)
            val arr = json.optJSONArray("questions") ?: JSONArray()
            val questions = (0 until arr.length()).mapNotNull { idx ->
                val item = arr.optJSONObject(idx) ?: return@mapNotNull null
                val promptText = item.optString("prompt").trim()
                val optionsArr = item.optJSONArray("options") ?: JSONArray()
                val options = (0 until optionsArr.length()).map { optionsArr.optString(it).trim() }.filter { it.isNotEmpty() }
                if (promptText.isEmpty() || options.size < 2) return@mapNotNull null
                val rawIndex = item.optInt("correctIndex", 0)
                val safeIndex = rawIndex.coerceIn(0, options.lastIndex)
                PracticeQuestion(
                    prompt = promptText,
                    targetExpression = item.optString("targetExpression").trim(),
                    romanization = item.optString("romanization").trim(),
                    options = options,
                    correctIndex = safeIndex,
                    explanation = item.optString("explanation").trim()
                )
            }
            if (questions.isEmpty()) {
                return Result.failure(IllegalStateException("No practice questions could be generated. Please retry."))
            }
            Result.success(
                PracticeDrillSet(
                    languageName = language.displayName,
                    topic = topic,
                    questions = questions
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Skill drill generation timed out", e)
            Result.failure(Exception("Skill drill timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed generating skill drill", e)
            Result.failure(e)
        }
    }
}
