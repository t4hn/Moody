package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.DailyContext

@Entity(tableName = "daily_context")
data class DailyContextEntity(
  @PrimaryKey val dayKey: String, // "yyyy-MM-dd"
  val sleepDurationMinutes: Int? = null,
  val sleepQuality: Int? = null, // 1..5
  val updatedAt: Long = System.currentTimeMillis(),
)

fun DailyContextEntity.toDomain(): DailyContext =
  DailyContext(
    dayKey = dayKey,
    sleepDurationMinutes = sleepDurationMinutes?.takeIf { it >= 0 },
    sleepQuality = sleepQuality?.takeIf { it in 1..5 },
    updatedAt = updatedAt,
  )

fun DailyContext.toEntity(): DailyContextEntity =
  DailyContextEntity(
    dayKey = dayKey,
    sleepDurationMinutes = sleepDurationMinutes?.coerceIn(0, 24 * 60),
    sleepQuality = sleepQuality?.coerceIn(1, 5),
    updatedAt = updatedAt,
  )
