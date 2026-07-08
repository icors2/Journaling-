// ==========================================
// FILE: data/repository/JournalMappers.kt
// DESCRIPTION: Maps between Room entities and domain models.
// ==========================================
package com.journal.app.data.repository

import com.journal.app.data.local.entities.JournalDayEntity
import com.journal.app.data.local.entities.VoiceEntryEntity
import com.journal.app.domain.model.JournalDay
import com.journal.app.domain.model.VoiceEntry

internal fun JournalDayEntity.toDomain(voiceEntries: List<VoiceEntry> = emptyList()): JournalDay {
    return JournalDay(
        dateIsoString = dateIsoString,
        consolidatedText = consolidatedText,
        lastModifiedLong = lastModifiedLong,
        voiceEntries = voiceEntries
    )
}

internal fun VoiceEntryEntity.toDomain(): VoiceEntry {
    return VoiceEntry(
        id = id,
        parentDateIsoString = parentDateIsoString,
        timestampFormatted = timestampFormatted,
        localFilePath = localFilePath,
        googleDriveFileId = googleDriveFileId,
        epochTimestamp = epochTimestamp
    )
}

internal fun VoiceEntry.toEntity(): VoiceEntryEntity {
    return VoiceEntryEntity(
        id = id,
        parentDateIsoString = parentDateIsoString,
        timestampFormatted = timestampFormatted,
        localFilePath = localFilePath,
        googleDriveFileId = googleDriveFileId,
        epochTimestamp = epochTimestamp
    )
}
