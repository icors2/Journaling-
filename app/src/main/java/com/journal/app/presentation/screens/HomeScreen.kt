// ==========================================
// FILE: presentation/screens/HomeScreen.kt
// DESCRIPTION: Main timeline feed showing append logs and voice playback controls.
// ==========================================
package com.journal.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.journal.app.domain.model.JournalDay
import com.journal.app.domain.model.VoiceEntry
import com.journal.app.presentation.JournalViewModel

/**
 * SECTION: Home Screen
 * Primary UI surface for text append, voice capture, and timeline browsing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: JournalViewModel,
    onRequestSignIn: () -> Unit,
    onRequestRecordPermission: () -> Unit,
    hasRecordPermission: Boolean
) {
    val journalDays by viewModel.journalDays.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val playingEntryId by viewModel.playingEntryId.collectAsState()
    val signedInEmail by viewModel.signedInEmail.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var entryText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Journal") },
                actions = {
                    if (signedInEmail == null) {
                        Button(onClick = onRequestSignIn, modifier = Modifier.padding(end = 8.dp)) {
                            Text("Sign In")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud backup active",
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = entryText,
                onValueChange = { entryText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Write your journal entry") },
                minLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.appendTextEntry(entryText)
                        entryText = ""
                    },
                    enabled = entryText.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Entry")
                }

                IconButton(
                    onClick = {
                        if (!hasRecordPermission) {
                            onRequestRecordPermission()
                        } else if (isRecording) {
                            viewModel.stopVoiceRecording()
                        } else {
                            viewModel.startVoiceRecording()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop recording" else "Record voice note",
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isRecording) {
                Text(
                    text = "Recording…",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (journalDays.isEmpty()) {
                Text(
                    text = "No entries yet. Start writing or record a voice note.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(journalDays, key = { it.dateIsoString }) { day ->
                        JournalDayCard(
                            day = day,
                            playingEntryId = playingEntryId,
                            onTogglePlayback = viewModel::toggleVoicePlayback
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalDayCard(
    day: JournalDay,
    playingEntryId: String?,
    onTogglePlayback: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = day.dateIsoString,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (day.consolidatedText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = day.consolidatedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (day.voiceEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                day.voiceEntries.forEach { voice ->
                    VoiceEntryCard(
                        entry = voice,
                        isPlaying = playingEntryId == voice.id,
                        onTogglePlayback = onTogglePlayback
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceEntryCard(
    entry: VoiceEntry,
    isPlaying: Boolean,
    onTogglePlayback: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Voice note",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = entry.timestampFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.googleDriveFileId != null) {
                    Text(
                        text = "Backed up",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = { onTogglePlayback(entry.id, entry.localFilePath) }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
        }
    }
}
