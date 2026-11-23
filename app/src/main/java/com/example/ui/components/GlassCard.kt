package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MoodyTheme

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  glowColor: Color? = null,
  cornerRadius: Dp = 22.dp,
  borderColor: Color = MoodyTheme.colors.border,
  containerColor: Color = MoodyTheme.colors.cardSurface.copy(alpha = if (MoodyTheme.colors.isDark) 0.85f else 0.98f),
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val shape = RoundedCornerShape(cornerRadius)
  val cardModifier = modifier.clip(shape)
  val isDark = MoodyTheme.colors.isDark

  val innerContent: @Composable () -> Unit = {
    Box(
      modifier = Modifier.background(
        brush = if (glowColor != null) {
          Brush.radialGradient(
            colors = listOf(
              glowColor.copy(alpha = if (isDark) 0.14f else 0.10f),
              containerColor,
            ),
            center = Offset(120f, 60f),
            radius = 550f,
          )
        } else {
          Brush.verticalGradient(
            colors = listOf(
              containerColor,
              containerColor.copy(alpha = if (isDark) 0.92f else 1f),
            ),
          )
        },
      ),
      content = content,
    )
  }

  if (onClick != null) {
    Surface(
      onClick = onClick,
      modifier = cardModifier,
      shape = shape,
      color = Color.Transparent,
      border = BorderStroke(1.dp, borderColor),
    ) {
      innerContent()
    }
  } else {
    Surface(
      modifier = cardModifier,
      shape = shape,
      color = Color.Transparent,
      border = BorderStroke(1.dp, borderColor),
    ) {
      innerContent()
    }
  }
}

@Composable
fun SectionHeader(
  title: String,
  trailing: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.padding(horizontal = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = MoodyTheme.colors.textPrimary,
      modifier = Modifier.weight(1f),
    )
    if (trailing != null) {
      trailing()
    }
  }
}
