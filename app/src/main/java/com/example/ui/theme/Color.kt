package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Fixed Mood Spectrum Gradient (remains consistent across all themes & palettes):
// 1 Red -> 2 Orange -> 3 Yellow -> 4 Lime Green -> 5 Green
val MoodColor1 = Color(0xFFE5484D) // Red (Very poor mood)
val MoodColor2 = Color(0xFFE88A36) // Orange (Poor mood)
val MoodColor3 = Color(0xFFE4C34B) // Yellow (Neutral / middle)
val MoodColor4 = Color(0xFF91BC52) // Lime Green (Good mood)
val MoodColor5 = Color(0xFF28A66A) // Green (Very good mood)

val MoodSpectrumStops = listOf(
  MoodColor1,
  MoodColor2,
  MoodColor3,
  MoodColor4,
  MoodColor5,
)

fun moodColorForLevel(level: Int): Color =
  when (level.coerceIn(1, 5)) {
    1 -> MoodColor1
    2 -> MoodColor2
    3 -> MoodColor3
    4 -> MoodColor4
    else -> MoodColor5
  }

fun moodColorInterpolated(value: Float, invertScale: Boolean = false): Color {
  val mapped = if (invertScale) 6f - value else value
  val clamped = mapped.coerceIn(1f, 5f)
  val index = (clamped - 1f).toInt().coerceIn(0, 3)
  val fraction = (clamped - 1f) - index
  return lerp(MoodSpectrumStops[index], MoodSpectrumStops[index + 1], fraction)
}

enum class AppearanceMode(val label: String) {
  SYSTEM("System Default"),
  LIGHT("Light"),
  DARK("Dark");

  companion object {
    fun fromNameOrDefault(raw: String?): AppearanceMode =
      entries.firstOrNull { it.name == raw } ?: DARK
  }
}

enum class MoodyPalette(
  val title: String,
  val subtitle: String,
  val previewDarkBg: Color,
  val previewLightBg: Color,
  val previewAccentDark: Color,
  val previewAccentLight: Color,
) {
  TEAL_OBSIDIAN(
    title = "Teal / Obsidian",
    subtitle = "Near-black canvas with turquoise & teal accents",
    previewDarkBg = Color(0xFF07080C),
    previewLightBg = Color(0xFFF4F7F9),
    previewAccentDark = Color(0xFF2DD4BF),
    previewAccentLight = Color(0xFF0E9488),
  ),
  SAGE_DAYLIGHT(
    title = "Sage / Daylight",
    subtitle = "Warm neutral canvas with sage green accents",
    previewDarkBg = Color(0xFF0C110E),
    previewLightBg = Color(0xFFF6F4EE),
    previewAccentDark = Color(0xFF7BB38A),
    previewAccentLight = Color(0xFF477855),
  ),
  VIOLET_MIDNIGHT(
    title = "Violet / Midnight",
    subtitle = "Deep navy canvas with violet & purple accents",
    previewDarkBg = Color(0xFF070A17),
    previewLightBg = Color(0xFFF4F3FA),
    previewAccentDark = Color(0xFF9D7BFF),
    previewAccentLight = Color(0xFF6B48DB),
  ),
  GREEN_OBSIDIAN(
    title = "Green / Obsidian",
    subtitle = "Near-black canvas with muted moss green accents",
    previewDarkBg = Color(0xFF070908),
    previewLightBg = Color(0xFFF2F6F3),
    previewAccentDark = Color(0xFF6DA87A),
    previewAccentLight = Color(0xFF3A6E47),
  );

  companion object {
    fun fromNameOrDefault(raw: String?): MoodyPalette =
      entries.firstOrNull { it.name == raw } ?: TEAL_OBSIDIAN
  }
}

@Immutable
data class MoodyColors(
  val isDark: Boolean,
  val background: Color,
  val surfaceBase: Color,
  val cardSurface: Color,
  val cardSurfaceElevated: Color,
  val border: Color,
  val borderBright: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textMuted: Color,
  val accentPrimary: Color,
  val onAccentPrimary: Color,
  val accentSecondary: Color,
  val danger: Color,
  val chartGrid: Color,
  val chartTrack: Color,
)

fun resolveMoodyColors(
  palette: MoodyPalette,
  isDark: Boolean,
): MoodyColors =
  when (palette) {
    MoodyPalette.TEAL_OBSIDIAN -> if (isDark) {
      MoodyColors(
        isDark = true,
        background = Color(0xFF07080C),
        surfaceBase = Color(0xFF0C0E14),
        cardSurface = Color(0xFF131722),
        cardSurfaceElevated = Color(0xFF1B2030),
        border = Color(0x18FFFFFF),
        borderBright = Color(0x2EFFFFFF),
        textPrimary = Color(0xFFF2F4F8),
        textSecondary = Color(0xFF99A2B8),
        textMuted = Color(0xFF60687C),
        accentPrimary = Color(0xFF2DD4BF),
        onAccentPrimary = Color(0xFF07080C),
        accentSecondary = Color(0xFF3FA7D6),
        danger = Color(0xFFEF5350),
        chartGrid = Color(0x12FFFFFF),
        chartTrack = Color(0x0DFFFFFF),
      )
    } else {
      MoodyColors(
        isDark = false,
        background = Color(0xFFF4F7F9),
        surfaceBase = Color(0xFFFFFFFF),
        cardSurface = Color(0xFFFFFFFF),
        cardSurfaceElevated = Color(0xFFE7EFF2),
        border = Color(0x1A0F172A),
        borderBright = Color(0x300F172A),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF64748B),
        accentPrimary = Color(0xFF0E9488),
        onAccentPrimary = Color(0xFFFFFFFF),
        accentSecondary = Color(0xFF2578A5),
        danger = Color(0xFFD32F2F),
        chartGrid = Color(0x140F172A),
        chartTrack = Color(0x0D0F172A),
      )
    }

    MoodyPalette.SAGE_DAYLIGHT -> if (isDark) {
      MoodyColors(
        isDark = true,
        background = Color(0xFF0C110E),
        surfaceBase = Color(0xFF121814),
        cardSurface = Color(0xFF18211B),
        cardSurfaceElevated = Color(0xFF212D25),
        border = Color(0x1AFFFFFF),
        borderBright = Color(0x2EFFFFFF),
        textPrimary = Color(0xFFEEF3EF),
        textSecondary = Color(0xFFA0B0A4),
        textMuted = Color(0xFF6A7A6E),
        accentPrimary = Color(0xFF7BB38A),
        onAccentPrimary = Color(0xFF0C110E),
        accentSecondary = Color(0xFF5D926B),
        danger = Color(0xFFEF5350),
        chartGrid = Color(0x12FFFFFF),
        chartTrack = Color(0x0DFFFFFF),
      )
    } else {
      MoodyColors(
        isDark = false,
        background = Color(0xFFF6F4EE),
        surfaceBase = Color(0xFFFAF8F3),
        cardSurface = Color(0xFFFFFFFF),
        cardSurfaceElevated = Color(0xFFECE8DD),
        border = Color(0x1C283329),
        borderBright = Color(0x33283329),
        textPrimary = Color(0xFF1B241D),
        textSecondary = Color(0xFF4A584D),
        textMuted = Color(0xFF6C7A6F),
        accentPrimary = Color(0xFF477855),
        onAccentPrimary = Color(0xFFFFFFFF),
        accentSecondary = Color(0xFF689373),
        danger = Color(0xFFC93B3B),
        chartGrid = Color(0x141B241D),
        chartTrack = Color(0x0D1B241D),
      )
    }

    MoodyPalette.VIOLET_MIDNIGHT -> if (isDark) {
      MoodyColors(
        isDark = true,
        background = Color(0xFF070A17),
        surfaceBase = Color(0xFF0D1226),
        cardSurface = Color(0xFF141B36),
        cardSurfaceElevated = Color(0xFF1D264A),
        border = Color(0x1CFFFFFF),
        borderBright = Color(0x33FFFFFF),
        textPrimary = Color(0xFFF1F2FA),
        textSecondary = Color(0xFFA0A7C9),
        textMuted = Color(0xFF666E94),
        accentPrimary = Color(0xFF9D7BFF),
        onAccentPrimary = Color(0xFF070A17),
        accentSecondary = Color(0xFFC084FC),
        danger = Color(0xFFEF5350),
        chartGrid = Color(0x14FFFFFF),
        chartTrack = Color(0x0EFFFFFF),
      )
    } else {
      MoodyColors(
        isDark = false,
        background = Color(0xFFF4F3FA),
        surfaceBase = Color(0xFFFAF9FE),
        cardSurface = Color(0xFFFFFFFF),
        cardSurfaceElevated = Color(0xFFE9E6F7),
        border = Color(0x1C1E1B4B),
        borderBright = Color(0x331E1B4B),
        textPrimary = Color(0xFF16142E),
        textSecondary = Color(0xFF4B4870),
        textMuted = Color(0xFF716E94),
        accentPrimary = Color(0xFF6B48DB),
        onAccentPrimary = Color(0xFFFFFFFF),
        accentSecondary = Color(0xFF8B5CF6),
        danger = Color(0xFFD32F2F),
        chartGrid = Color(0x1416142E),
        chartTrack = Color(0x0D16142E),
      )
    }

    MoodyPalette.GREEN_OBSIDIAN -> if (isDark) {
      MoodyColors(
        isDark = true,
        background = Color(0xFF070908),
        surfaceBase = Color(0xFF0C100E),
        cardSurface = Color(0xFF131A16),
        cardSurfaceElevated = Color(0xFF1B251F),
        border = Color(0x18FFFFFF),
        borderBright = Color(0x2EFFFFFF),
        textPrimary = Color(0xFFEFF3F0),
        textSecondary = Color(0xFF96A59A),
        textMuted = Color(0xFF5F6E63),
        accentPrimary = Color(0xFF6DA87A),
        onAccentPrimary = Color(0xFF070908),
        accentSecondary = Color(0xFF87B891),
        danger = Color(0xFFEF5350),
        chartGrid = Color(0x12FFFFFF),
        chartTrack = Color(0x0DFFFFFF),
      )
    } else {
      MoodyColors(
        isDark = false,
        background = Color(0xFFF2F6F3),
        surfaceBase = Color(0xFFF9FBFA),
        cardSurface = Color(0xFFFFFFFF),
        cardSurfaceElevated = Color(0xFFE3ECE6),
        border = Color(0x1C14261A),
        borderBright = Color(0x3314261A),
        textPrimary = Color(0xFF111C15),
        textSecondary = Color(0xFF425448),
        textMuted = Color(0xFF66796C),
        accentPrimary = Color(0xFF3A6E47),
        onAccentPrimary = Color(0xFFFFFFFF),
        accentSecondary = Color(0xFF578B63),
        danger = Color(0xFFD32F2F),
        chartGrid = Color(0x14111C15),
        chartTrack = Color(0x0D111C15),
      )
    }
  }
