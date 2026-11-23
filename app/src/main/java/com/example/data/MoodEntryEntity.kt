package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.MoodEntry
import org.json.JSONArray

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
  @PrimaryKey val id: String,
  val mood: Int,
  val timestamp: Long,
  val note: String,
  val tagsJson: String,
  val isDemo: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
)

fun MoodEntryEntity.toDomain(): MoodEntry =
  MoodEntry(
    id = id,
    mood = mood.coerceIn(1, 5),
    timestamp = timestamp,
    note = note,
    tags = decodeTags(tagsJson),
    isDemo = isDemo,
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
    isDemo = isDemo,
    createdAt = createdAt,
    updatedAt = updatedAt,
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
