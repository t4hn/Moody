package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.DailyContext
import com.example.model.MoodEntry
import com.example.model.PhysicalActivityLevel
import org.json.JSONArray

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
  @PrimaryKey val id: String,
  val mood: Int,
  val timestamp: Long,
  val note: String,
  val tagsJson: String,
  val createdAt: Long,
  val updatedAt: Long,
  val energyLevel: Int? = null,
  val stressLevel: Int? = null,
  val anxietyLevel: Int? = null,
  val physicalActivity: String? = null,
)

fun MoodEntryEntity.toDomain(dailyContext: DailyContext? = null): MoodEntry =
  MoodEntry(
    id = id,
    mood = mood.coerceIn(1, 5),
    timestamp = timestamp,
    note = note,
    tags = decodeTags(tagsJson),
    sleepDurationMinutes = dailyContext?.sleepDurationMinutes,
    sleepQuality = dailyContext?.sleepQuality,
    energyLevel = energyLevel?.takeIf { it in 1..5 },
    stressLevel = stressLevel?.takeIf { it in 1..5 },
    anxietyLevel = anxietyLevel?.takeIf { it in 1..5 },
    physicalActivity = PhysicalActivityLevel.fromNameOrNull(physicalActivity),
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun MoodEntry.toEntity(): MoodEntryEntity =
  MoodEntryEntity(
    id = id,
    mood = mood.coerceIn(1, 5),
    timestamp = timestamp,
    note = note,
    tagsJson = encodeTags(tags),
    createdAt = createdAt,
    updatedAt = updatedAt,
    energyLevel = energyLevel?.coerceIn(1, 5),
    stressLevel = stressLevel?.coerceIn(1, 5),
    anxietyLevel = anxietyLevel?.coerceIn(1, 5),
    physicalActivity = physicalActivity?.name,
  )

private fun encodeTags(tags: List<String>): String {
  val array = JSONArray()
  tags.forEach { tag ->
    val clean = tag.trim()
    if (clean.isNotEmpty()) array.put(clean)
  }
  return array.toString()
}

private fun decodeTags(json: String): List<String> {
  if (json.isBlank()) return emptyList()
  return try {
    val array = JSONArray(json)
    buildList {
      for (i in 0 until array.length()) {
        val value = array.optString(i).trim()
        if (value.isNotEmpty()) add(value)
      }
    }
  } catch (_: Exception) {
    emptyList()
  }
}
