// ==========================================
// FILE: audio/AudioPlayer.kt
// DESCRIPTION: MediaPlayer wrapper for voice note playback with play/pause control.
// ==========================================
package com.journal.app.audio

import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * SECTION: Audio Player
 * Manages single-instance MediaPlayer playback for voice journal entries.
 */
class AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var currentEntryId: String? = null

    val playingEntryId: String?
        get() = if (mediaPlayer?.isPlaying == true) currentEntryId else null

    suspend fun togglePlayback(entryId: String, filePath: String): Boolean = withContext(Dispatchers.IO) {
        if (currentEntryId == entryId && mediaPlayer?.isPlaying == true) {
            pause()
            return@withContext false
        }

        stopInternal()

        if (!File(filePath).exists()) {
            return@withContext false
        }

        val player = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
            setOnCompletionListener {
                stopInternal()
            }
        }

        mediaPlayer = player
        currentEntryId = entryId
        true
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun stop() {
        stopInternal()
    }

    private fun stopInternal() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {
            // Player may already be released.
        } finally {
            mediaPlayer = null
            currentEntryId = null
        }
    }
}
