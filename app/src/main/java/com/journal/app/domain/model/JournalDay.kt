// ==========================================
// FILE: domain/model/JournalDay.kt
// DESCRIPTION: Domain-specific text log data model with nested voice entries.
// ==========================================
package com.journal.app.domain.model

data class JournalDay(
    val dateIsoString: String,
    val consolidatedText: String,
    val lastModifiedLong: Long,
    val voiceEntries: List<VoiceEntry> = emptyList()
)
