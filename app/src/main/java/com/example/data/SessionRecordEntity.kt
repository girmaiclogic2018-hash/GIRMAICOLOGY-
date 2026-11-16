package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_records")
data class SessionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val languageCode: String,
    val languageName: String,
    val scenarioTitle: String,
    val levelLabel: String,
    val durationSeconds: Long,
    val turnCount: Int,
    val aiSummaryFeedback: String,
    val strengthsSummary: String = "",
    val grammarTip: String = "",
    val transcriptText: String,
    val createdAt: Long = System.currentTimeMillis()
)
