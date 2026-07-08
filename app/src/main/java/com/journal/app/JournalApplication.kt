// ==========================================
// FILE: JournalApplication.kt
// DESCRIPTION: Application entry point wiring database, repository, and WorkManager.
// ==========================================
package com.journal.app

import android.app.Application
import androidx.work.Configuration
import com.journal.app.data.local.JournalDatabase
import com.journal.app.data.repository.JournalRepositoryImpl
import com.journal.app.domain.repository.JournalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JournalApplication : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: JournalRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = JournalDatabase.getInstance(this)
        repository = JournalRepositoryImpl(
            context = this,
            journalDao = database.journalDao()
        )

        applicationScope.launch {
            repository.cleanupOrphanVoiceEntries()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
