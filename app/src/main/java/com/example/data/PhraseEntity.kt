package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phrases")
data class PhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val phrase: String,
    val translation: String,
    val languageCode: String,
    val languageName: String,
    val contextNote: String = "",
    val masteryLevel: Int = 0, // 0 = New, 1 = Learning, 2 = Familiar, 3 = Mastered
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null
)
