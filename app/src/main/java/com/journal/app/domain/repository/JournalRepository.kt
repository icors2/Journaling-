// ==========================================
// FILE: domain/repository/JournalRepository.kt
// DESCRIPTION: Repository interface decoupling presentation from data implementation.
// ==========================================
package com.journal.app.domain.repository

import com.journal.app.domain.model.JournalDay
import com.journal.app.domain.model.VoiceEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {

    fun observeJournalTimeline(): Flow<List<JournalDay>>

    suspend fun appendTextEntry(userText: String)

    suspend fun saveVoiceEntry(localFilePath: String): VoiceEntry

    suspend fun cleanupOrphanVoiceEntries()

    fun getSignedInAccountEmail(): String?

    fun setSignedInAccountEmail(email: String?)

    fun scheduleBackup(dateIsoString: String? = null, forceFullSync: Boolean = false)
}
