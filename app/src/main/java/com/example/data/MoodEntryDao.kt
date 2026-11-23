package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodEntryDao {

  @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
  fun observeAllEntries(): Flow<List<MoodEntryEntity>>

  @Query("SELECT COUNT(*) FROM mood_entries WHERE isDemo = 1")
  fun observeDemoCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM mood_entries")
  suspend fun countEntries(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entry: MoodEntryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(entries: List<MoodEntryEntity>)

  @Query("DELETE FROM mood_entries WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM mood_entries WHERE isDemo = 1")
  suspend fun deleteDemoEntries()

  @Query("DELETE FROM mood_entries")
  suspend fun deleteAll()

  @Transaction
  suspend fun replaceAll(entries: List<MoodEntryEntity>) {
    deleteAll()
    upsertAll(entries)
  }
}
