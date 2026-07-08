// ==========================================
// FILE: data/local/JournalDatabase.kt
// DESCRIPTION: Room database initialization and singleton access.
// ==========================================
package com.journal.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.journal.app.data.local.entities.JournalDayEntity
import com.journal.app.data.local.entities.VoiceEntryEntity

@Database(
    entities = [JournalDayEntity::class, VoiceEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class JournalDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao

    companion object {
        private const val DATABASE_NAME = "journal_database"

        @Volatile
        private var INSTANCE: JournalDatabase? = null

        fun getInstance(context: Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    DATABASE_NAME
                ).build().also { INSTANCE = it }
            }
        }
    }
}
