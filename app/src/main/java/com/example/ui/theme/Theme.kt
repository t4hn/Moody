package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalMoodyColors = staticCompositionLocalOf {
  resolveMoodyColors(MoodyPalette.TEAL_OBSIDIAN, isDark = true)
}

object MoodyTheme {
  val colors: MoodyColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMoodyColors.current
}

val MoodyShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun MoodyTheme(
  appearanceMode: AppearanceMode = AppearanceMode.DARK,
  palette: MoodyPalette = MoodyPalette.TEAL_OBSIDIAN,
  content: @Composable () -> Unit,
) {
  val isDark = when (appearanceMode) {
    AppearanceMode.SYSTEM -> isSystemInDarkTheme()
    AppearanceMode.LIGHT -> false
    AppearanceMode.DARK -> true
  }
  val moodyColors = resolveMoodyColors(palette = palette, isDark = isDark)

  val colorScheme = if (isDark) {
    darkColorScheme(
      primary = moodyColors.accentPrimary,
      onPrimary = moodyColors.onAccentPrimary,
      primaryContainer = moodyColors.cardSurfaceElevated,
      onPrimaryContainer = moodyColors.textPrimary,
      secondary = moodyColors.accentSecondary,
      onSecondary = moodyColors.onAccentPrimary,
      secondaryContainer = moodyColors.cardSurface,
      onSecondaryContainer = moodyColors.textPrimary,
      tertiary = MoodColor5,
      onTertiary = moodyColors.onAccentPrimary,
      background = moodyColors.background,
      onBackground = moodyColors.textPrimary,
      surface = moodyColors.surfaceBase,
      onSurface = moodyColors.textPrimary,
      surfaceVariant = moodyColors.cardSurface,
      onSurfaceVariant = moodyColors.textSecondary,
      surfaceContainerLow = moodyColors.surfaceBase,
      surfaceContainer = moodyColors.cardSurface,
      surfaceContainerHigh = moodyColors.cardSurfaceElevated,
      outline = moodyColors.borderBright,
      outlineVariant = moodyColors.border,
      error = moodyColors.danger,
      onError = moodyColors.textPrimary,
    )
  } else {
    lightColorScheme(
      primary = moodyColors.accentPrimary,
      onPrimary = moodyColors.onAccentPrimary,
      primaryContainer = moodyColors.cardSurfaceElevated,
      onPrimaryContainer = moodyColors.textPrimary,
      secondary = moodyColors.accentSecondary,
      onSecondary = moodyColors.onAccentPrimary,
      secondaryContainer = moodyColors.cardSurface,
      onSecondaryContainer = moodyColors.textPrimary,
      tertiary = MoodColor5,
      onTertiary = moodyColors.onAccentPrimary,
      background = moodyColors.background,
      onBackground = moodyColors.textPrimary,
      surface = moodyColors.surfaceBase,
      onSurface = moodyColors.textPrimary,
      surfaceVariant = moodyColors.cardSurface,
      onSurfaceVariant = moodyColors.textSecondary,
      surfaceContainerLow = moodyColors.surfaceBase,
      surfaceContainer = moodyColors.cardSurface,
      surfaceContainerHigh = moodyColors.cardSurfaceElevated,
      outline = moodyColors.borderBright,
      outlineVariant = moodyColors.border,
      error = moodyColors.danger,
      onError = moodyColors.onAccentPrimary,
    )
  }

  CompositionLocalProvider(LocalMoodyColors provides moodyColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = MoodyTypography,
      shapes = MoodyShapes,
      content = content,
    )
  }
}
