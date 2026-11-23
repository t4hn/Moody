package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  glowColor: Color? = null,
  cornerRadius: Dp = 22.dp,
  borderColor: Color = GlassBorder,
  containerColor: Color = GlassSurface.copy(alpha = 0.85f),
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val shape = RoundedCornerShape(cornerRadius)
  val cardModifier = modifier.clip(shape)

  val innerContent: @Composable () -> Unit = {
    Box(
      modifier = Modifier.background(
        brush = if (glowColor != null) {
          Brush.radialGradient(
            colors = listOf(
              glowColor.copy(alpha = 0.14f),
              containerColor,
            ),
            center = Offset(120f, 60f),
            radius = 550f,
          )
        } else {
          Brush.verticalGradient(
            colors = listOf(
              containerColor,
              containerColor.copy(alpha = 0.92f),
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
  androidx.compose.foundation.layout.Row(
    modifier = modifier.padding(horizontal = 2.dp),
    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
  ) {
    androidx.compose.material3.Text(
      text = title,
      style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
      color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.weight(1f),
    )
    if (trailing != null) {
      trailing()
    }
  }
}
