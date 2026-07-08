// ==========================================
// FILE: data/remote/BackupWorker.kt
// DESCRIPTION: WorkManager CoroutineWorker for background Google Drive synchronization.
// ==========================================
package com.journal.app.data.remote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.journal.app.data.local.JournalDatabase
import com.journal.app.data.local.entities.VoiceEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * SECTION: Backup Worker
 * Synchronizes local Room data to Google Drive with conflict resolution and orphan cleanup.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val BACKOFF_DELAY_SECONDS = 30L
        private const val MAX_RETRY_COUNT = 5
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val accountEmail = applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCOUNT_EMAIL, null)

        if (accountEmail.isNullOrBlank()) {
            return@withContext Result.failure()
        }

        val dao = JournalDatabase.getInstance(applicationContext).journalDao()
        val driveClient = GoogleDriveClient(applicationContext, accountEmail)

        val targetDate = inputData.getString(BackupScheduler.KEY_DATE_ISO)
        val forceFullSync = inputData.getBoolean(BackupScheduler.KEY_FORCE_FULL_SYNC, false)

        return@withContext try {
            syncTextDays(dao, driveClient, targetDate, forceFullSync)
            syncPendingAudio(dao, driveClient)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun syncTextDays(
        dao: com.journal.app.data.local.JournalDao,
        driveClient: GoogleDriveClient,
        targetDate: String?,
        forceFullSync: Boolean
    ) {
        val days = when {
            !targetDate.isNullOrBlank() -> {
                dao.getDayByDate(targetDate)?.let { listOf(it) } ?: emptyList()
            }
            forceFullSync -> dao.getAllDaysSnapshot()
            else -> dao.getAllDaysSnapshot()
        }

        for (day in days) {
            val result = driveClient.syncTextFile(
                dateIsoString = day.dateIsoString,
                consolidatedText = day.consolidatedText,
                localLastModified = day.lastModifiedLong
            )
            if (result.isFailure) {
                throw result.exceptionOrNull() ?: Exception("Text sync failed for ${day.dateIsoString}")
            }
        }
    }

    private suspend fun syncPendingAudio(
        dao: com.journal.app.data.local.JournalDao,
        driveClient: GoogleDriveClient
    ) {
        val pendingEntries = dao.getVoiceEntriesPendingUpload()

        for (entry in pendingEntries) {
            processVoiceEntry(dao, driveClient, entry)
        }
    }

    private suspend fun processVoiceEntry(
        dao: com.journal.app.data.local.JournalDao,
        driveClient: GoogleDriveClient,
        entry: VoiceEntryEntity
    ) {
        val localFile = File(entry.localFilePath)

        if (!localFile.exists()) {
            dao.deleteVoiceEntryById(entry.id)
            return
        }

        val remoteFileName = "voice_${entry.parentDateIsoString}_${entry.id}.m4a"
        val uploadResult = driveClient.uploadAudioFile(entry.localFilePath, remoteFileName)

        if (uploadResult.isSuccess) {
            dao.updateVoiceDriveFileId(entry.id, uploadResult.getOrNull())
        } else {
            dao.updateVoiceDriveFileId(entry.id, null)
            throw uploadResult.exceptionOrNull() ?: Exception("Audio upload failed for ${entry.id}")
        }
    }
}

private const val PREFS_NAME = "journal_prefs"
private const val KEY_ACCOUNT_EMAIL = "account_email"
