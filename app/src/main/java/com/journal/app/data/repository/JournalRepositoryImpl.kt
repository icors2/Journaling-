// ==========================================
// FILE: data/repository/JournalRepositoryImpl.kt
// DESCRIPTION: Concrete repository managing local Room storage and remote backup orchestration.
// ==========================================
package com.journal.app.data.repository

import android.content.Context
import com.journal.app.data.local.JournalDao
import com.journal.app.data.local.entities.JournalDayEntity
import com.journal.app.data.local.entities.VoiceEntryEntity
import com.journal.app.data.remote.BackupScheduler
import com.journal.app.domain.model.JournalDay
import com.journal.app.domain.model.VoiceEntry
import com.journal.app.domain.repository.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * SECTION: Repository Implementation
 * Orchestrates append-only journal writes, voice metadata persistence, and backup scheduling.
 */
class JournalRepositoryImpl(
    private val context: Context,
    private val journalDao: JournalDao
) : JournalRepository {

    companion object {
        private const val PREFS_NAME = "journal_prefs"
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private val ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
        private val READABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mma", Locale.getDefault())
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun observeJournalTimeline(): Flow<List<JournalDay>> {
        return combine(
            journalDao.observeAllDays(),
            journalDao.observeAllVoiceEntries()
        ) { days, allVoiceEntries ->
            val voiceByDate = allVoiceEntries.groupBy { it.parentDateIsoString }
            days.map { dayEntity ->
                val voices = voiceByDate[dayEntity.dateIsoString]
                    ?.sortedBy { it.epochTimestamp }
                    ?.map { it.toDomain() }
                    ?: emptyList()
                dayEntity.toDomain(voices)
            }
        }
    }

    override suspend fun appendTextEntry(userText: String) = withContext(Dispatchers.IO) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return@withContext

        val now = LocalDateTime.now()
        val dateIso = now.format(ISO_DATE_FORMATTER)
        val readableDate = now.format(READABLE_DATE_FORMATTER)
        val formattedTime = now.format(TIME_FORMATTER).lowercase(Locale.getDefault())
        val entryBlock = "$readableDate $formattedTime\n$trimmed\n"

        val existing = journalDao.getDayByDate(dateIso)
        val updatedText = if (existing == null) {
            entryBlock
        } else {
            existing.consolidatedText + "\n" + entryBlock
        }

        val entity = JournalDayEntity(
            dateIsoString = dateIso,
            consolidatedText = updatedText,
            lastModifiedLong = System.currentTimeMillis()
        )

        journalDao.upsertDay(entity)
        scheduleBackup(dateIso)
    }

    override suspend fun saveVoiceEntry(localFilePath: String): VoiceEntry = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now()
        val dateIso = now.format(ISO_DATE_FORMATTER)
        val formattedTime = now.format(TIME_FORMATTER).lowercase(Locale.getDefault())
        val epoch = System.currentTimeMillis()

        ensureDayExists(dateIso)

        val entry = VoiceEntryEntity(
            id = UUID.randomUUID().toString(),
            parentDateIsoString = dateIso,
            timestampFormatted = formattedTime,
            localFilePath = localFilePath,
            googleDriveFileId = null,
            epochTimestamp = epoch
        )

        journalDao.insertVoiceEntry(entry)
        scheduleBackup(dateIso)

        entry.toDomain()
    }

    override suspend fun cleanupOrphanVoiceEntries() = withContext(Dispatchers.IO) {
        val pending = journalDao.getVoiceEntriesPendingUpload()
        for (entry in pending) {
            if (!File(entry.localFilePath).exists()) {
                journalDao.deleteVoiceEntryById(entry.id)
            }
        }
    }

    override fun getSignedInAccountEmail(): String? {
        return prefs.getString(KEY_ACCOUNT_EMAIL, null)
    }

    override fun setSignedInAccountEmail(email: String?) {
        prefs.edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
    }

    override fun scheduleBackup(dateIsoString: String?, forceFullSync: Boolean) {
        BackupScheduler.enqueue(context, dateIsoString, forceFullSync)
    }

    private suspend fun ensureDayExists(dateIso: String) {
        val existing = journalDao.getDayByDate(dateIso)
        if (existing == null) {
            journalDao.upsertDay(
                JournalDayEntity(
                    dateIsoString = dateIso,
                    consolidatedText = "",
                    lastModifiedLong = System.currentTimeMillis()
                )
            )
        }
    }
}
