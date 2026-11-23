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
  val scaleDescription: String,
  val color: Color,
) {
  VERY_LOW(1, "Very Low", "Very poor mood", MoodColor1),
  LOW(2, "Low", "Poor mood", MoodColor2),
  OKAY(3, "Okay", "Neutral / middle", MoodColor3),
  GOOD(4, "Good", "Good mood", MoodColor4),
  GREAT(5, "Great", "Very good mood", MoodColor5);

  companion object {
    fun fromScore(score: Int): MoodLevel =
      entries.firstOrNull { it.score == score } ?: OKAY
  }
}
