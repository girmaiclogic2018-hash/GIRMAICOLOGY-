package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LinguaRoomPersistenceTest {

    private lateinit var database: LinguaDatabase
    private lateinit var repository: LinguaRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LinguaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LinguaRepository(database.linguaDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `save and observe practice sessions ordered newest first`() = runBlocking {
        val now = System.currentTimeMillis()
        val olderId = repository.saveSession(
            SessionRecordEntity(
                languageCode = "es",
                languageName = "Spanish",
                scenarioTitle = "Café & Dining",
                levelLabel = "Beginner",
                durationSeconds = 120L,
                turnCount = 8,
                aiSummaryFeedback = "Good ordering vocabulary.",
                transcriptText = "Partner: ¡Hola!\nYou: Un café por favor.",
                createdAt = now - 60_000L
            )
        )
        val newerId = repository.saveSession(
            SessionRecordEntity(
                languageCode = "fr",
                languageName = "French",
                scenarioTitle = "Travel & Airport",
                levelLabel = "Intermediate",
                durationSeconds = 180L,
                turnCount = 12,
                aiSummaryFeedback = "Smooth polite requests.",
                transcriptText = "Partner: Bonjour!\nYou: Où est la gare ?",
                createdAt = now
            )
        )

        val sessions = repository.allSessions.first()
        assertEquals(2, sessions.size)
        assertEquals(newerId, sessions[0].id)
        assertEquals(olderId, sessions[1].id)
    }

    @Test
    fun `updateSessionReview persists strengths and grammar tip`() = runBlocking {
        val id = repository.saveSession(
            SessionRecordEntity(
                languageCode = "ja",
                languageName = "Japanese",
                scenarioTitle = "Free Conversation",
                levelLabel = "Beginner",
                durationSeconds = 95L,
                turnCount = 6,
                aiSummaryFeedback = "Initial summary",
                transcriptText = "Partner: こんにちは\nYou: こんにちは"
            )
        )

        repository.updateSessionReview(
            id = id,
            feedback = "Clear polite greetings.",
            strengths = "• Natural です/ます endings",
            grammarTip = "Use をお願いします when ordering."
        )

        val updated = repository.allSessions.first().single()
        assertEquals("Clear polite greetings.", updated.aiSummaryFeedback)
        assertEquals("• Natural です/ます endings", updated.strengthsSummary)
        assertEquals("Use をお願いします when ordering.", updated.grammarTip)
    }

    @Test
    fun `observeStats aggregates practice time turns streak and language breakdown`() = runBlocking {
        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60L * 60L * 1000L

        repository.saveSession(
            SessionRecordEntity(
                languageCode = "es",
                languageName = "Spanish",
                scenarioTitle = "Café & Dining",
                levelLabel = "Beginner",
                durationSeconds = 180L,
                turnCount = 10,
                aiSummaryFeedback = "Great start",
                transcriptText = "You: Hola",
                createdAt = now
            )
        )
        repository.saveSession(
            SessionRecordEntity(
                languageCode = "es",
                languageName = "Spanish",
                scenarioTitle = "Local Market",
                levelLabel = "Beginner",
                durationSeconds = 120L,
                turnCount = 6,
                aiSummaryFeedback = "Good bargaining",
                transcriptText = "You: ¿Cuánto cuesta?",
                createdAt = now - oneDayMs
            )
        )

        val stats = repository.observeStats().first()
        assertEquals(2, stats.totalSessions)
        assertEquals(5, stats.totalMinutes)
        assertEquals(3, stats.todayMinutes)
        assertEquals(16, stats.totalTurns)
        assertEquals(2, stats.streakDays)
        assertEquals(1, stats.languageBreakdown.size)
        assertEquals("Spanish", stats.languageBreakdown.first().languageName)
    }

    @Test
    fun `deleteSession removes record from Room`() = runBlocking {
        val id = repository.saveSession(
            SessionRecordEntity(
                languageCode = "de",
                languageName = "German",
                scenarioTitle = "Job Interview",
                levelLabel = "Advanced",
                durationSeconds = 240L,
                turnCount = 14,
                aiSummaryFeedback = "Confident phrasing",
                transcriptText = "You: Guten Tag"
            )
        )

        repository.deleteSession(id)
        val remaining = repository.allSessions.first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun `expanded language catalog includes Ethiopian African and Global languages with unique codes and saves to Room`() = runBlocking {
        val entries = com.example.model.TargetLanguage.entries
        assertEquals(entries.size, entries.map { it.code }.distinct().size)
        assertEquals(entries.size, entries.map { it.shortBadge }.distinct().size)

        val ethiopian = entries.filter { it.category == com.example.model.LanguageCategory.ETHIOPIAN }
        val african = entries.filter { it.category == com.example.model.LanguageCategory.AFRICAN }
        val global = entries.filter { it.category == com.example.model.LanguageCategory.GLOBAL }

        assertTrue(ethiopian.size >= 20)
        assertTrue(african.size >= 12)
        assertTrue(global.size >= 15)

        val sampleLanguages = listOf(
            com.example.model.TargetLanguage.AMHARIC,
            com.example.model.TargetLanguage.AFAAN_OROMO,
            com.example.model.TargetLanguage.TIGRIGNA,
            com.example.model.TargetLanguage.SWAHILI,
            com.example.model.TargetLanguage.YORUBA,
            com.example.model.TargetLanguage.ENGLISH,
            com.example.model.TargetLanguage.MANDARIN,
            com.example.model.TargetLanguage.HINDI
        )
        sampleLanguages.forEach { lang ->
            repository.saveSession(
                SessionRecordEntity(
                    languageCode = lang.code,
                    languageName = lang.displayName,
                    scenarioTitle = "Free Conversation",
                    levelLabel = "Beginner",
                    durationSeconds = 60L,
                    turnCount = 4,
                    aiSummaryFeedback = "Practiced ${lang.displayName}",
                    transcriptText = "Partner: ${lang.sampleGreeting}"
                )
            )
        }

        val stats = repository.observeStats().first()
        assertEquals(sampleLanguages.size, stats.totalSessions)
        assertEquals(sampleLanguages.size, stats.languageBreakdown.size)
    }

    @Test
    fun `translation records support save star ordering edit update and delete in Room`() = runBlocking {
        val now = System.currentTimeMillis()
        val firstId = repository.saveTranslation(
            TranslationRecordEntity(
                sourceText = "Good morning",
                translatedText = "እንደምን አደርክ",
                sourceLanguageCode = "en",
                sourceLanguageName = "English",
                targetLanguageCode = "am",
                targetLanguageName = "Amharic",
                romanization = "Indemin aderk",
                entryType = "TEXT",
                isStarred = false,
                createdAt = now,
                updatedAt = now
            )
        )
        val secondId = repository.saveTranslation(
            TranslationRecordEntity(
                sourceText = "Thank you",
                translatedText = "Asante sana",
                sourceLanguageCode = "en",
                sourceLanguageName = "English",
                targetLanguageCode = "sw",
                targetLanguageName = "Swahili",
                entryType = "PHOTO",
                isStarred = true,
                createdAt = now - 10_000L,
                updatedAt = now - 10_000L
            )
        )

        val ordered = repository.allTranslations.first()
        assertEquals(2, ordered.size)
        // Starred translation appears first
        assertEquals(secondId, ordered[0].id)
        assertEquals(firstId, ordered[1].id)

        // Edit / update first translation
        val edited = ordered[1].copy(
            translatedText = "እንደምን አደሩ",
            note = "Polite form",
            isStarred = true,
            updatedAt = now + 5_000L
        )
        repository.updateTranslation(edited)
        val afterUpdate = repository.allTranslations.first()
        assertEquals("እንደምን አደሩ", afterUpdate.first().translatedText)
        assertEquals("Polite form", afterUpdate.first().note)

        // Delete second translation
        repository.deleteTranslation(secondId)
        val afterDelete = repository.allTranslations.first()
        assertEquals(1, afterDelete.size)
        assertEquals(firstId, afterDelete.single().id)
    }
}
