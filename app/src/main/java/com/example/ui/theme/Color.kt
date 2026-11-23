package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Core Dark & Moody Surfaces
val ObsidianBg = Color(0xFF07080C)
val CharcoalBase = Color(0xFF0C0E14)
val GlassSurface = Color(0xFF131722)
val GlassSurfaceElevated = Color(0xFF1B2030)
val GlassBorder = Color(0x18FFFFFF)
val GlassBorderBright = Color(0x2EFFFFFF)

// Typography
val TextPrimary = Color(0xFFF2F4F8)
val TextSecondary = Color(0xFF99A2B8)
val TextMuted = Color(0xFF60687C)

// Mood Spectrum Gradient:
// 1 (Very Low) Cool Violet/Indigo -> 2 Cool Blue -> 3 Teal -> 4 Amber -> 5 Warm Rose/Orange
val MoodColor1 = Color(0xFF7B6BFF) // Deep Twilight Violet-Blue
val MoodColor2 = Color(0xFF3FA7D6) // Cool Dusk Blue
val MoodColor3 = Color(0xFF2DD4BF) // Serene Teal
val MoodColor4 = Color(0xFFF5A623) // Golden Amber
val MoodColor5 = Color(0xFFFF6E6C) // Warm Rose-Coral

val MoodSpectrumStops = listOf(
  MoodColor1,
  MoodColor2,
  MoodColor3,
  MoodColor4,
  MoodColor5,
)

val AccentPrimary = MoodColor3
val AccentWarm = MoodColor5
val DangerRose = Color(0xFFEF5350)
val SuccessTeal = Color(0xFF2DD4BF)

fun moodColorForLevel(level: Int): Color =
  when (level.coerceIn(1, 5)) {
    1 -> MoodColor1
    2 -> MoodColor2
    3 -> MoodColor3
    4 -> MoodColor4
    else -> MoodColor5
  }

fun moodColorInterpolated(value: Float): Color {
  val clamped = value.coerceIn(1f, 5f)
  val index = (clamped - 1f).toInt().coerceIn(0, 3)
  val fraction = (clamped - 1f) - index
  return lerp(MoodSpectrumStops[index], MoodSpectrumStops[index + 1], fraction)
}
