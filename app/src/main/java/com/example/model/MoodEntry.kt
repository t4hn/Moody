package com.example.model

import java.util.UUID

data class MoodEntry(
  val id: String = UUID.randomUUID().toString(),
  val mood: Int,
  val timestamp: Long,
  val note: String = "",
  val tags: List<String> = emptyList(),
  val isDemo: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
) {
  val moodLevel: MoodLevel
    get() = MoodLevel.fromScore(mood)
}
