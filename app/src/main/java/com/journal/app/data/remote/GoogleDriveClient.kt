// ==========================================
// FILE: data/remote/GoogleDriveClient.kt
// DESCRIPTION: OAuth2-backed Google Drive REST API client for journal backups.
// ==========================================
package com.journal.app.data.remote

import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * SECTION: Google Drive Client
 * Handles folder provisioning, optimistic-concurrency text sync, and binary audio uploads.
 */
class GoogleDriveClient(
    private val context: Context,
    accountEmail: String
) {

    companion object {
        const val JOURNAL_FOLDER_NAME = "My Android Cloud Journal"
        private const val TEXT_MIME_TYPE = "text/plain"
        private const val AUDIO_MIME_TYPE = "audio/mp4"
        private const val DRIVE_ROOT = "root"
    }

    private val driveService: Drive = buildDriveService(accountEmail)

    private var cachedFolderId: String? = null

    private fun buildDriveService(email: String): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccountName = email
        }

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Cloud Journal")
            .build()
    }

    // --- Folder Management ---

    suspend fun getOrCreateJournalFolder(): String = withContext(Dispatchers.IO) {
        cachedFolderId?.let { return@withContext it }

        val query = "mimeType='application/vnd.google-apps.folder' " +
            "and name='$JOURNAL_FOLDER_NAME' " +
            "and '$DRIVE_ROOT' in parents " +
            "and trashed=false"

        val existing = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()
            .files

        if (!existing.isNullOrEmpty()) {
            cachedFolderId = existing.first().id
            return@withContext cachedFolderId!!
        }

        val folderMetadata = File().apply {
            name = JOURNAL_FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
            parents = listOf(DRIVE_ROOT)
        }

        val created = driveService.files().create(folderMetadata)
            .setFields("id")
            .execute()

        cachedFolderId = created.id
        created.id
    }

    // --- Text Sync with Optimistic Concurrency ---

    suspend fun syncTextFile(
        dateIsoString: String,
        consolidatedText: String,
        localLastModified: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val folderId = getOrCreateJournalFolder()
            val fileName = "journal_$dateIsoString.txt"
            val remoteFile = findFileInFolder(folderId, fileName)

            if (remoteFile == null) {
                createTextFile(folderId, fileName, consolidatedText)
            } else {
                val remoteModified = remoteFile.modifiedTime?.value ?: 0L
                // Local Room entry is the absolute source of truth; force-overwrite when local is newer or equal.
                if (localLastModified >= remoteModified) {
                    updateTextFile(remoteFile.id, consolidatedText)
                } else {
                    updateTextFile(remoteFile.id, consolidatedText)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createTextFile(folderId: String, fileName: String, content: String) {
        val tempFile = java.io.File.createTempFile("journal_create_", ".txt", context.cacheDir)
        try {
            tempFile.writeText(content)
            val metadata = File().apply {
                name = fileName
                parents = listOf(folderId)
            }
            val fileContent = FileContent(TEXT_MIME_TYPE, tempFile)
            driveService.files().create(metadata, fileContent)
                .setFields("id, modifiedTime")
                .execute()
        } finally {
            tempFile.delete()
        }
    }

    private fun updateTextFile(remoteFileId: String, content: String) {
        val tempFile = java.io.File.createTempFile("journal_upload_", ".txt", context.cacheDir)
        try {
            tempFile.writeText(content)
            val fileContent = FileContent(TEXT_MIME_TYPE, tempFile)
            driveService.files().update(remoteFileId, null, fileContent).execute()
        } finally {
            tempFile.delete()
        }
    }

    // --- Audio Sync ---

    suspend fun uploadAudioFile(
        localFilePath: String,
        remoteFileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val localFile = java.io.File(localFilePath)
            if (!localFile.exists()) {
                return@withContext Result.failure(IOException("Local audio file missing: $localFilePath"))
            }

            val folderId = getOrCreateJournalFolder()
            val existing = findFileInFolder(folderId, remoteFileName)

            val fileContent = FileContent(AUDIO_MIME_TYPE, localFile)

            val fileId = if (existing != null) {
                driveService.files().update(existing.id, null, fileContent)
                    .setFields("id")
                    .execute()
                    .id
            } else {
                val metadata = File().apply {
                    name = remoteFileName
                    parents = listOf(folderId)
                }
                driveService.files().create(metadata, fileContent)
                    .setFields("id")
                    .execute()
                    .id
            }

            Result.success(fileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Helpers ---

    private fun findFileInFolder(folderId: String, fileName: String): File? {
        val escapedName = fileName.replace("'", "\\'")
        val query = "name='$escapedName' and '$folderId' in parents and trashed=false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name, modifiedTime)")
            .execute()

        return result.files?.firstOrNull()
    }
}
