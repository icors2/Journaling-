// ==========================================
// FILE: data/local/JournalDao.kt
// DESCRIPTION: Local CRUD queries for journal days and voice entries.
// ==========================================
package com.journal.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.journal.app.data.local.entities.JournalDayEntity
import com.journal.app.data.local.entities.VoiceEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    // --- Journal Day Queries ---

    @Query("SELECT * FROM journal_days ORDER BY dateIsoString DESC")
    fun observeAllDays(): Flow<List<JournalDayEntity>>

    @Query("SELECT * FROM journal_days WHERE dateIsoString = :dateIsoString LIMIT 1")
    suspend fun getDayByDate(dateIsoString: String): JournalDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDay(day: JournalDayEntity)

    @Query("SELECT * FROM journal_days")
    suspend fun getAllDaysSnapshot(): List<JournalDayEntity>

    // --- Voice Entry Queries ---

    @Query("SELECT * FROM voice_entries WHERE parentDateIsoString = :dateIsoString ORDER BY epochTimestamp ASC")
    fun observeVoiceEntriesForDay(dateIsoString: String): Flow<List<VoiceEntryEntity>>

    @Query("SELECT * FROM voice_entries ORDER BY epochTimestamp DESC")
    fun observeAllVoiceEntries(): Flow<List<VoiceEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceEntry(entry: VoiceEntryEntity)

    @Update
    suspend fun updateVoiceEntry(entry: VoiceEntryEntity)

    @Query("DELETE FROM voice_entries WHERE id = :entryId")
    suspend fun deleteVoiceEntryById(entryId: String)

    @Query("SELECT * FROM voice_entries WHERE googleDriveFileId IS NULL")
    suspend fun getVoiceEntriesPendingUpload(): List<VoiceEntryEntity>

    @Query("UPDATE voice_entries SET googleDriveFileId = :driveFileId WHERE id = :entryId")
    suspend fun updateVoiceDriveFileId(entryId: String, driveFileId: String?)
}
