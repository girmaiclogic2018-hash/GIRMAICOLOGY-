package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class LanguageProgress(
    val languageCode: String,
    val languageName: String,
    val sessionCount: Int,
    val totalSeconds: Long,
    val totalTurns: Int
)

data class PracticeStats(
    val todayMinutes: Int = 0,
    val totalMinutes: Int = 0,
    val streakDays: Int = 0,
    val totalSessions: Int = 0,
    val totalTurns: Int = 0,
    val totalSavedPhrases: Int = 0,
    val languageBreakdown: List<LanguageProgress> = emptyList()
)

class LinguaRepository(private val dao: LinguaDao) {

    val allPhrases: Flow<List<PhraseEntity>> = dao.observeAllPhrases()
    val allSessions: Flow<List<SessionRecordEntity>> = dao.observeAllSessions()
    val allTranslations: Flow<List<TranslationRecordEntity>> = dao.observeAllTranslations()

    fun observeStats(): Flow<PracticeStats> =
        dao.observeAllSessions().map { sessions ->
            computeStats(sessions)
        }

    suspend fun savePhrase(phrase: PhraseEntity): Long = dao.insertPhrase(phrase)

    suspend fun updatePhrase(phrase: PhraseEntity) = dao.updatePhrase(phrase)

    suspend fun deletePhrase(id: Long) = dao.deletePhraseById(id)

    suspend fun saveSession(session: SessionRecordEntity): Long = dao.insertSession(session)

    suspend fun updateSessionReview(
        id: Long,
        feedback: String,
        strengths: String,
        grammarTip: String
    ) = dao.updateSessionReview(id, feedback, strengths, grammarTip)

    suspend fun deleteSession(id: Long) = dao.deleteSessionById(id)

    suspend fun saveTranslation(record: TranslationRecordEntity): Long = dao.insertTranslation(record)

    suspend fun updateTranslation(record: TranslationRecordEntity) = dao.updateTranslation(record)

    suspend fun deleteTranslation(id: Long) = dao.deleteTranslationById(id)

    suspend fun clearAllData() {
        dao.clearAllPhrases()
        dao.clearAllSessions()
        dao.clearAllTranslations()
    }

    private fun computeStats(sessions: List<SessionRecordEntity>): PracticeStats {
        if (sessions.isEmpty()) return PracticeStats()

        val cal = Calendar.getInstance()
        val todayKey = dayKey(cal)

        val secondsToday = sessions
            .filter { session ->
                cal.timeInMillis = session.createdAt
                dayKey(cal) == todayKey
            }
            .sumOf { it.durationSeconds }

        val totalSeconds = sessions.sumOf { it.durationSeconds }
        val totalTurns = sessions.sumOf { it.turnCount }

        val activeDayKeys = sessions.map { session ->
            cal.timeInMillis = session.createdAt
            dayKey(cal)
        }.toSet()

        cal.timeInMillis = System.currentTimeMillis()
        var streak = 0
        if (!activeDayKeys.contains(dayKey(cal))) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        while (activeDayKeys.contains(dayKey(cal))) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val breakdown = sessions
            .groupBy { it.languageCode }
            .map { (code, group) ->
                LanguageProgress(
                    languageCode = code,
                    languageName = group.first().languageName,
                    sessionCount = group.size,
                    totalSeconds = group.sumOf { it.durationSeconds },
                    totalTurns = group.sumOf { it.turnCount }
                )
            }
            .sortedByDescending { it.totalSeconds }

        return PracticeStats(
            todayMinutes = ((secondsToday + 30) / 60).toInt(),
            totalMinutes = ((totalSeconds + 30) / 60).toInt(),
            streakDays = streak,
            totalSessions = sessions.size,
            totalTurns = totalTurns,
            languageBreakdown = breakdown
        )
    }

    private fun dayKey(cal: Calendar): Long {
        val year = cal.get(Calendar.YEAR)
        val day = cal.get(Calendar.DAY_OF_YEAR)
        return year * 1000L + day
    }
}
