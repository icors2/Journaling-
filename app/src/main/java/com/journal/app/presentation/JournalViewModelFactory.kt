// ==========================================
// FILE: presentation/JournalViewModelFactory.kt
// DESCRIPTION: Factory for constructing JournalViewModel with repository dependency.
// ==========================================
package com.journal.app.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.journal.app.domain.repository.JournalRepository

class JournalViewModelFactory(
    private val application: Application,
    private val repository: JournalRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            return JournalViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
