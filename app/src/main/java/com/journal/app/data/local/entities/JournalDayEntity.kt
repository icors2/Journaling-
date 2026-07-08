// ==========================================
// FILE: data/local/entities/JournalDayEntity.kt
// DESCRIPTION: Stores the consolidated text payload for a calendar date.
// ==========================================
package com.journal.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_days",
    indices = [Index(value = ["dateIsoString"], unique = true)]
)
data class JournalDayEntity(
    @PrimaryKey
    val dateIsoString: String,
    val consolidatedText: String,
    val lastModifiedLong: Long
)
