package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LinguaDao {
    @Query("SELECT * FROM phrases ORDER BY createdAt DESC")
    fun observeAllPhrases(): Flow<List<PhraseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: PhraseEntity): Long

    @Update
    suspend fun updatePhrase(phrase: PhraseEntity)

    @Query("DELETE FROM phrases WHERE id = :id")
    suspend fun deletePhraseById(id: Long)

    @Query("DELETE FROM phrases")
    suspend fun clearAllPhrases()

    @Query("SELECT * FROM session_records ORDER BY createdAt DESC")
    fun observeAllSessions(): Flow<List<SessionRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionRecordEntity): Long

    @Query(
        "UPDATE session_records SET aiSummaryFeedback = :feedback, strengthsSummary = :strengths, grammarTip = :grammarTip WHERE id = :id"
    )
    suspend fun updateSessionReview(id: Long, feedback: String, strengths: String, grammarTip: String)

    @Query("DELETE FROM session_records WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM session_records")
    suspend fun clearAllSessions()

    @Query("SELECT * FROM translation_records ORDER BY isStarred DESC, updatedAt DESC")
    fun observeAllTranslations(): Flow<List<TranslationRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(record: TranslationRecordEntity): Long

    @Update
    suspend fun updateTranslation(record: TranslationRecordEntity)

    @Query("DELETE FROM translation_records WHERE id = :id")
    suspend fun deleteTranslationById(id: Long)

    @Query("DELETE FROM translation_records")
    suspend fun clearAllTranslations()
}
