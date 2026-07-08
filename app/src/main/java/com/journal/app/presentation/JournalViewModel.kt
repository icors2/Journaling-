// ==========================================
// FILE: presentation/JournalViewModel.kt
// DESCRIPTION: Business state handler; triggers local saves and cloud backup scheduling.
// ==========================================
package com.journal.app.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.journal.app.audio.AudioPlayer
import com.journal.app.audio.AudioRecorder
import com.journal.app.domain.model.JournalDay
import com.journal.app.domain.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * SECTION: ViewModel
 * Exposes reactive journal timeline state and coordinates recording/playback actions.
 */
class JournalViewModel(
    application: Application,
    private val repository: JournalRepository
) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorder(application)
    private val audioPlayer = AudioPlayer()

    val journalDays: StateFlow<List<JournalDay>> = repository
        .observeJournalTimeline()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _playingEntryId = MutableStateFlow<String?>(null)
    val playingEntryId: StateFlow<String?> = _playingEntryId.asStateFlow()

    private val _signedInEmail = MutableStateFlow(repository.getSignedInAccountEmail())
    val signedInEmail: StateFlow<String?> = _signedInEmail.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun onSignedIn(email: String, showMessage: Boolean = true) {
        repository.setSignedInAccountEmail(email)
        _signedInEmail.value = email
        repository.scheduleBackup(forceFullSync = true)
        if (showMessage) {
            _statusMessage.value = "Signed in. Cloud backup enabled."
        }
    }

    fun onSignOut() {
        repository.setSignedInAccountEmail(null)
        _signedInEmail.value = null
        _statusMessage.value = "Signed out."
    }

    fun appendTextEntry(text: String) {
        viewModelScope.launch {
            repository.appendTextEntry(text)
            _statusMessage.value = "Entry saved."
        }
    }

    fun startVoiceRecording() {
        viewModelScope.launch {
            try {
                audioRecorder.startRecording()
                _isRecording.value = true
            } catch (e: Exception) {
                _statusMessage.value = "Failed to start recording: ${e.message}"
            }
        }
    }

    fun stopVoiceRecording() {
        viewModelScope.launch {
            try {
                val path = audioRecorder.stopRecording()
                _isRecording.value = false
                if (path != null) {
                    repository.saveVoiceEntry(path)
                    _statusMessage.value = "Voice note saved."
                }
            } catch (e: Exception) {
                _isRecording.value = false
                _statusMessage.value = "Failed to save recording: ${e.message}"
            }
        }
    }

    fun toggleVoicePlayback(entryId: String, filePath: String) {
        viewModelScope.launch {
            val isPlaying = audioPlayer.togglePlayback(entryId, filePath)
            _playingEntryId.value = if (isPlaying) entryId else null
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
