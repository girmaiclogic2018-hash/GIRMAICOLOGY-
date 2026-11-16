package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PhraseEntity::class, SessionRecordEntity::class, TranslationRecordEntity::class],
    version = 3,
    exportSchema = false
)
abstract class LinguaDatabase : RoomDatabase() {
    abstract fun linguaDao(): LinguaDao

    companion object {
        @Volatile
        private var INSTANCE: LinguaDatabase? = null

        fun getInstance(context: Context): LinguaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LinguaDatabase::class.java,
                    "lingualive.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
