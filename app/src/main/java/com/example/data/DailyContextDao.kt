package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyContextDao {

  @Query("SELECT * FROM daily_context")
  fun observeAll(): Flow<List<DailyContextEntity>>

  @Query("SELECT * FROM daily_context")
  suspend fun getAllOnce(): List<DailyContextEntity>

  @Query("SELECT * FROM daily_context WHERE dayKey = :dayKey LIMIT 1")
  suspend fun getByDayKey(dayKey: String): DailyContextEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(context: DailyContextEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(contexts: List<DailyContextEntity>)

  @Query("DELETE FROM daily_context WHERE dayKey = :dayKey")
  suspend fun deleteByDayKey(dayKey: String)

  @Query("DELETE FROM daily_context")
  suspend fun deleteAll()

  @Transaction
  suspend fun replaceAll(contexts: List<DailyContextEntity>) {
    deleteAll()
    upsertAll(contexts)
  }
}
