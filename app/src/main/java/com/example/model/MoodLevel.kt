package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MoodColor1
import com.example.ui.theme.MoodColor2
import com.example.ui.theme.MoodColor3
import com.example.ui.theme.MoodColor4
import com.example.ui.theme.MoodColor5

enum class MoodLevel(
  val score: Int,
  val label: String,
  val shortLabel: String,
  val color: Color,
) {
  VERY_LOW(1, "Very Low", "Heavy", MoodColor1),
  LOW(2, "Low", "Low", MoodColor2),
  OKAY(3, "Okay", "Steady", MoodColor3),
  GOOD(4, "Good", "Warm", MoodColor4),
  GREAT(5, "Great", "Radiant", MoodColor5);

  companion object {
    fun fromScore(score: Int): MoodLevel =
      entries.firstOrNull { it.score == score } ?: OKAY
  }
}
