// ==========================================
// FILE: data/local/entities/VoiceEntryEntity.kt
// DESCRIPTION: Holds metadata and file paths for audio recordings mapped to a day.
// ==========================================
package com.journal.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "voice_entries",
    foreignKeys = [
        ForeignKey(
            entity = JournalDayEntity::class,
            parentColumns = ["dateIsoString"],
            childColumns = ["parentDateIsoString"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentDateIsoString"]),
        Index(value = ["googleDriveFileId"])
    ]
)
data class VoiceEntryEntity(
    @PrimaryKey
    val id: String,
    val parentDateIsoString: String,
    val timestampFormatted: String,
    val localFilePath: String,
    val googleDriveFileId: String?,
    val epochTimestamp: Long
)
