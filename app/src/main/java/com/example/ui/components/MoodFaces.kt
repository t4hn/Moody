package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MoodLevel
import com.example.ui.theme.MoodyTheme

@Composable
fun MoodFaceOrb(
  level: MoodLevel,
  size: Dp = 44.dp,
  selected: Boolean = false,
  showGlow: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val accent = level.color
  val orbFillBottom = colors.cardSurfaceElevated.copy(alpha = if (colors.isDark) 0.92f else 0.96f)
  val faceStrokeColor = when {
    selected -> colors.textPrimary
    colors.isDark -> accent.copy(alpha = 0.95f)
    else -> lerp(accent, colors.textPrimary, 0.35f)
  }

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val radius = this.size.minDimension / 2f
      val center = Offset(this.size.width / 2f, this.size.height / 2f)

      if (showGlow) {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              accent.copy(alpha = if (selected) 0.42f else 0.22f),
              Color.Transparent,
            ),
            center = center,
            radius = radius * 1.15f,
          ),
          radius = radius * 1.15f,
          center = center,
        )
      }

      // Orb background
      drawCircle(
        brush = Brush.verticalGradient(
          colors = listOf(
            accent.copy(alpha = if (selected) 0.34f else 0.18f),
            orbFillBottom,
          ),
        ),
        radius = radius * 0.88f,
        center = center,
      )

      // Outer ring
      drawCircle(
        color = accent.copy(alpha = if (selected) 0.95f else 0.55f),
        radius = radius * 0.88f,
        center = center,
        style = Stroke(width = if (selected) radius * 0.11f else radius * 0.065f),
      )

      // Expressive face strokes
      val strokeW = (radius * 0.095f).coerceAtLeast(1.8f)
      val eyeY = center.y - radius * 0.12f
      val eyeOffsetX = radius * 0.27f
      val faceColor = faceStrokeColor

      when (level) {
        MoodLevel.VERY_LOW -> {
          drawLine(
            color = faceColor,
            start = Offset(center.x - eyeOffsetX - radius * 0.09f, eyeY - radius * 0.03f),
            end = Offset(center.x - eyeOffsetX + radius * 0.07f, eyeY + radius * 0.05f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round,
          )
          drawLine(
            color = faceColor,
            start = Offset(center.x + eyeOffsetX + radius * 0.09f, eyeY - radius * 0.03f),
            end = Offset(center.x + eyeOffsetX - radius * 0.07f, eyeY + radius * 0.05f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round,
          )
          val mouthPath = Path().apply {
            moveTo(center.x - radius * 0.28f, center.y + radius * 0.30f)
            quadraticTo(
              center.x,
              center.y + radius * 0.06f,
              center.x + radius * 0.28f,
              center.y + radius * 0.30f,
            )
          }
          drawPath(
            path = mouthPath,
            color = faceColor,
            style = Stroke(width = strokeW, cap = StrokeCap.Round),
          )
        }

        MoodLevel.LOW -> {
          drawCircle(color = faceColor, radius = radius * 0.065f, center = Offset(center.x - eyeOffsetX, eyeY))
          drawCircle(color = faceColor, radius = radius * 0.065f, center = Offset(center.x + eyeOffsetX, eyeY))
          val mouthPath = Path().apply {
            moveTo(center.x - radius * 0.24f, center.y + radius * 0.25f)
            quadraticTo(
              center.x,
              center.y + radius * 0.13f,
              center.x + radius * 0.24f,
              center.y + radius * 0.25f,
            )
          }
          drawPath(
            path = mouthPath,
            color = faceColor,
            style = Stroke(width = strokeW, cap = StrokeCap.Round),
          )
        }

        MoodLevel.OKAY -> {
          drawCircle(color = faceColor, radius = radius * 0.07f, center = Offset(center.x - eyeOffsetX, eyeY))
          drawCircle(color = faceColor, radius = radius * 0.07f, center = Offset(center.x + eyeOffsetX, eyeY))
          drawLine(
            color = faceColor,
            start = Offset(center.x - radius * 0.22f, center.y + radius * 0.20f),
            end = Offset(center.x + radius * 0.22f, center.y + radius * 0.20f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round,
          )
        }

        MoodLevel.GOOD -> {
          drawCircle(color = faceColor, radius = radius * 0.075f, center = Offset(center.x - eyeOffsetX, eyeY))
          drawCircle(color = faceColor, radius = radius * 0.075f, center = Offset(center.x + eyeOffsetX, eyeY))
          val mouthPath = Path().apply {
            moveTo(center.x - radius * 0.25f, center.y + radius * 0.15f)
            quadraticTo(
              center.x,
              center.y + radius * 0.34f,
              center.x + radius * 0.25f,
              center.y + radius * 0.15f,
            )
          }
          drawPath(
            path = mouthPath,
            color = faceColor,
            style = Stroke(width = strokeW, cap = StrokeCap.Round),
          )
        }

        MoodLevel.GREAT -> {
          drawArc(
            color = faceColor,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(center.x - eyeOffsetX - radius * 0.10f, eyeY - radius * 0.06f),
            size = Size(radius * 0.20f, radius * 0.14f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round),
          )
          drawArc(
            color = faceColor,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(center.x + eyeOffsetX - radius * 0.10f, eyeY - radius * 0.06f),
            size = Size(radius * 0.20f, radius * 0.14f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round),
          )
          val mouthPath = Path().apply {
            moveTo(center.x - radius * 0.29f, center.y + radius * 0.12f)
            quadraticTo(
              center.x,
              center.y + radius * 0.40f,
              center.x + radius * 0.29f,
              center.y + radius * 0.12f,
            )
          }
          drawPath(
            path = mouthPath,
            color = faceColor,
            style = Stroke(width = strokeW, cap = StrokeCap.Round),
          )
        }
      }
    }
  }
}

@Composable
fun MoodFaceSelectorRow(
  selectedMood: Int? = null,
  onSelectMood: (Int) -> Unit,
  pulseTrigger: Int = 0,
  orbSize: Dp = 54.dp,
  showLabels: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  var tappedMood by remember { mutableIntStateOf(-1) }
  val pulseProgress = remember { Animatable(0f) }

  LaunchedEffect(pulseTrigger) {
    if (pulseTrigger > 0 && tappedMood in 1..5) {
      pulseProgress.snapTo(0f)
      pulseProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
      )
    }
  }

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MoodLevel.entries.forEach { level ->
      val isSelected = selectedMood == level.score
      val isPulsing = tappedMood == level.score && pulseProgress.value in 0.01f..0.99f
      val scale by animateFloatAsState(
        targetValue = when {
          isPulsing -> 1f + 0.12f * kotlin.math.sin(pulseProgress.value * Math.PI).toFloat()
          isSelected -> 1.08f
          else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mood_scale_${level.score}",
      )
      val interactionSource = remember { MutableInteractionSource() }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .weight(1f)
          .minimumInteractiveComponentSize()
          .clip(MaterialTheme.shapes.medium)
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = level.color),
            role = Role.Button,
            onClick = {
              tappedMood = level.score
              onSelectMood(level.score)
            },
          )
          .padding(vertical = 6.dp)
          .testTag("mood_button_${level.score}")
          .semantics {
            contentDescription = "Log mood ${level.score}: ${level.label}"
          },
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.size(orbSize + 8.dp),
        ) {
          if (isPulsing) {
            val haloAlpha = (1f - pulseProgress.value) * 0.55f
            val haloScale = 1f + pulseProgress.value * 0.32f
            Box(
              modifier = Modifier
                .size(orbSize)
                .scale(haloScale)
                .border(
                  width = 2.dp,
                  color = level.color.copy(alpha = haloAlpha),
                  shape = CircleShape,
                )
                .background(level.color.copy(alpha = haloAlpha * 0.25f), CircleShape),
            )
          }

          MoodFaceOrb(
            level = level,
            size = orbSize,
            selected = isSelected || isPulsing,
            modifier = Modifier.scale(scale),
          )
        }

        if (showLabels) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = level.score.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (isSelected) {
              if (colors.isDark) level.color else colors.textPrimary
            } else if (selectedMood == null) {
              colors.textSecondary
            } else {
              colors.textMuted
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
