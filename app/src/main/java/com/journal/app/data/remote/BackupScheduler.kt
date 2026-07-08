// ==========================================
// FILE: data/remote/BackupScheduler.kt
// DESCRIPTION: Enqueues unique WorkManager backup jobs with network constraints.
// ==========================================
package com.journal.app.data.remote

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object BackupScheduler {

    const val UNIQUE_WORK_NAME = "journal_cloud_backup"
    const val KEY_DATE_ISO = "date_iso_string"
    const val KEY_FORCE_FULL_SYNC = "force_full_sync"

    fun enqueue(context: Context, dateIsoString: String? = null, forceFullSync: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            KEY_DATE_ISO to dateIsoString,
            KEY_FORCE_FULL_SYNC to forceFullSync
        )

        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BackupWorker.BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }
}
