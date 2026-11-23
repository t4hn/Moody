package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MoodyDarkColorScheme = darkColorScheme(
  primary = MoodColor3,
  onPrimary = ObsidianBg,
  primaryContainer = GlassSurfaceElevated,
  onPrimaryContainer = TextPrimary,
  secondary = MoodColor2,
  onSecondary = ObsidianBg,
  secondaryContainer = GlassSurface,
  onSecondaryContainer = TextPrimary,
  tertiary = MoodColor5,
  onTertiary = ObsidianBg,
  background = ObsidianBg,
  onBackground = TextPrimary,
  surface = CharcoalBase,
  onSurface = TextPrimary,
  surfaceVariant = GlassSurface,
  onSurfaceVariant = TextSecondary,
  surfaceContainerLow = CharcoalBase,
  surfaceContainer = GlassSurface,
  surfaceContainerHigh = GlassSurfaceElevated,
  outline = GlassBorderBright,
  outlineVariant = GlassBorder,
  error = DangerRose,
  onError = TextPrimary,
)

val MoodyShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun MoodyTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = MoodyDarkColorScheme,
    typography = MoodyTypography,
    shapes = MoodyShapes,
    content = content,
  )
}
