// ==========================================
// FILE: audio/AudioRecorder.kt
// DESCRIPTION: MediaRecorder wrapper for voice recording to .m4a in app-private storage.
// ==========================================
package com.journal.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * SECTION: Audio Recorder
 * Captures AAC audio in MPEG-4 (.m4a) containers under the app's files directory.
 */
class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputPath: String? = null

    val isRecording: Boolean
        get() = mediaRecorder != null

    suspend fun startRecording(): String = withContext(Dispatchers.IO) {
        stopRecordingInternal(discard = true)

        val audioDir = File(context.filesDir, "audio").apply { mkdirs() }
        val outputFile = File(audioDir, "${UUID.randomUUID()}.m4a")
        currentOutputPath = outputFile.absolutePath

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }

        mediaRecorder = recorder
        outputFile.absolutePath
    }

    suspend fun stopRecording(): String? = withContext(Dispatchers.IO) {
        val path = currentOutputPath
        stopRecordingInternal(discard = false)
        path
    }

    private fun stopRecordingInternal(discard: Boolean) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            if (discard) {
                currentOutputPath?.let { File(it).delete() }
            }
        } finally {
            mediaRecorder = null
            if (discard) {
                currentOutputPath = null
            }
        }
    }
}
