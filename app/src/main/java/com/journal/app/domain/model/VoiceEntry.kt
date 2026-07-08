// ==========================================
// FILE: domain/model/VoiceEntry.kt
// DESCRIPTION: Domain-specific voice message data model.
// ==========================================
package com.journal.app.domain.model

data class VoiceEntry(
    val id: String,
    val parentDateIsoString: String,
    val timestampFormatted: String,
    val localFilePath: String,
    val googleDriveFileId: String?,
    val epochTimestamp: Long
)
