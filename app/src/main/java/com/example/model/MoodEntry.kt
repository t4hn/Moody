package com.example.model

import java.util.UUID

enum class PhysicalActivityLevel(val label: String) {
  NONE("None"),
  LIGHT("Light"),
  MODERATE("Moderate"),
  HEAVY("Heavy");

  companion object {
    fun fromNameOrNull(raw: String?): PhysicalActivityLevel? {
      if (raw.isNullOrBlank()) return null
      return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
    }
  }
}

data class DailyContext(
  val dayKey: String, // "yyyy-MM-dd" local calendar date
  val sleepDurationMinutes: Int? = null,
  val sleepQuality: Int? = null, // 1..5
  val updatedAt: Long = System.currentTimeMillis(),
) {
  val hasAnyData: Boolean
    get() = sleepDurationMinutes != null || sleepQuality != null
}

data class MoodEntry(
  val id: String = UUID.randomUUID().toString(),
  val mood: Int,
  val timestamp: Long,
  val note: String = "",
  val tags: List<String> = emptyList(),
  val sleepDurationMinutes: Int? = null,
  val sleepQuality: Int? = null,
  val energyLevel: Int? = null,
  val stressLevel: Int? = null,
  val anxietyLevel: Int? = null,
  val physicalActivity: PhysicalActivityLevel? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
) {
  val moodLevel: MoodLevel
    get() = MoodLevel.fromScore(mood)

  val hasAnyContext: Boolean
    get() = sleepDurationMinutes != null ||
      sleepQuality != null ||
      energyLevel != null ||
      stressLevel != null ||
      anxietyLevel != null ||
      physicalActivity != null
}
