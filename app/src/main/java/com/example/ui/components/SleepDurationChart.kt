package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MoodyTheme
import com.example.viewmodel.MoodAnalytics
import com.example.viewmodel.SleepDurationPoint
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun SleepDurationLineChart(
  points: List<SleepDurationPoint>,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val hasRecorded = remember(points) { points.any { it.sleepDurationMinutes != null } }

  if (points.isEmpty() || !hasRecorded) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .height(150.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "No sleep duration recorded for this period.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textMuted,
        textAlign = TextAlign.Center,
      )
    }
    return
  }

  val defaultIndex = remember(points) {
    points.indexOfLast { it.sleepDurationMinutes != null }.coerceAtLeast(0)
  }
  var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
  val activeIdx = (selectedIndex ?: defaultIndex).coerceIn(0, points.lastIndex)
  val activePoint = points[activeIdx]
  val activeMinutes = activePoint.sleepDurationMinutes
  val lineColor = colors.accentPrimary
  val hasMissingDays = remember(points) { points.any { it.sleepDurationMinutes == null } }

  val (minHours, maxHours) = remember(points) {
    val recorded = points.mapNotNull { it.sleepDurationMinutes }
    val rawMinH = floor((recorded.minOrNull() ?: 360) / 60f).toInt()
    val rawMaxH = ceil((recorded.maxOrNull() ?: 540) / 60f).toInt()
    val minH = (rawMinH - 1).coerceAtLeast(0)
    val maxH = (rawMaxH + 1).coerceAtLeast(minH + 4).coerceAtMost(24)
    minH to maxH
  }
  val yTicks = remember(minHours, maxHours) {
    val step = ((maxHours - minHours) / 4f)
    (0..4).map { idx ->
      (maxHours - idx * step).roundToInt().coerceIn(minHours, maxHours)
    }
  }

  Column(modifier = modifier.fillMaxWidth()) {
    // Interactive readout bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (activeMinutes != null) {
          Box(
            modifier = Modifier
              .size(9.dp)
              .background(lineColor, CircleShape),
          )
        } else {
          Box(
            modifier = Modifier
              .size(9.dp)
              .border(1.5.dp, colors.textMuted, CircleShape),
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = activePoint.label,
          style = MaterialTheme.typography.labelMedium,
          color = colors.textSecondary,
        )
      }
      Text(
        text = if (activeMinutes != null) {
          MoodAnalytics.formatSleepDuration(activeMinutes)
        } else {
          "Not recorded"
        },
        style = MaterialTheme.typography.labelSmall,
        color = when {
          activeMinutes == null -> colors.textMuted
          colors.isDark -> lineColor
          else -> colors.textPrimary
        },
      )
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(175.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Y-axis hours labels
      Column(
        modifier = Modifier
          .height(155.dp)
          .padding(end = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
      ) {
        yTicks.forEach { hr ->
          Text(
            text = "${hr}h",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
          )
        }
      }

      Canvas(
        modifier = Modifier
          .weight(1f)
          .height(165.dp)
          .pointerInput(points) {
            detectTapGestures { offset ->
              selectedIndex = findClosestPointIndex(offset.x, size.width.toFloat(), points.size)
            }
          }
          .pointerInput(points) {
            detectHorizontalDragGestures { change, _ ->
              change.consume()
              selectedIndex = findClosestPointIndex(change.position.x, size.width.toFloat(), points.size)
            }
          },
      ) {
        val padTop = 14f
        val padBottom = 14f
        val padLeft = 10f
        val padRight = 10f
        val plotW = (size.width - padLeft - padRight).coerceAtLeast(1f)
        val plotH = (size.height - padTop - padBottom).coerceAtLeast(1f)

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f), 0f)
        for (i in 0..4) {
          val norm = i / 4f
          val y = padTop + norm * plotH
          drawLine(
            color = colors.chartGrid,
            start = Offset(padLeft, y),
            end = Offset(size.width - padRight, y),
            strokeWidth = 1.2f,
            pathEffect = dashEffect,
          )
        }

        val minMins = minHours * 60f
        val spanMins = ((maxHours - minHours) * 60f).coerceAtLeast(60f)

        val xForIndex: (Int) -> Float = { idx ->
          if (points.size == 1) {
            padLeft + plotW / 2f
          } else {
            padLeft + (idx.toFloat() / (points.size - 1).toFloat()) * plotW
          }
        }

        val coords: List<Offset?> = points.mapIndexed { idx, pt ->
          pt.sleepDurationMinutes?.let { mins ->
            val norm = ((mins - minMins) / spanMins).coerceIn(0f, 1f)
            Offset(xForIndex(idx), padTop + (1f - norm) * plotH)
          }
        }

        for (i in 0 until points.size - 1) {
          val p0 = coords[i]
          val p1 = coords[i + 1]
          if (p0 != null && p1 != null) {
            drawLine(
              brush = Brush.linearGradient(
                colors = listOf(lineColor.copy(alpha = 0.85f), lineColor),
                start = p0,
                end = p1,
              ),
              start = p0,
              end = p1,
              strokeWidth = 5f,
              cap = StrokeCap.Round,
            )
          }
        }

        val activeX = xForIndex(activeIdx)
        drawLine(
          color = (if (activeMinutes != null) lineColor else colors.textMuted).copy(alpha = 0.30f),
          start = Offset(activeX, padTop),
          end = Offset(activeX, padTop + plotH),
          strokeWidth = 2f,
        )

        points.forEachIndexed { idx, pt ->
          val x = xForIndex(idx)
          val offset = coords[idx]
          val isHighlighted = idx == activeIdx
          if (offset == null) {
            val baseOffset = Offset(x, padTop + plotH)
            drawCircle(
              color = if (isHighlighted) colors.textSecondary else colors.borderBright,
              radius = if (isHighlighted) 4.5f else 2.5f,
              center = baseOffset,
              style = Stroke(width = 1.6f),
            )
          } else {
            if (isHighlighted) {
              drawCircle(
                color = lineColor.copy(alpha = 0.28f),
                radius = 18f,
                center = offset,
              )
            }
            drawCircle(
              color = lineColor,
              radius = if (isHighlighted) 8.5f else 5f,
              center = offset,
            )
            if (isHighlighted) {
              drawCircle(
                color = colors.surfaceBase,
                radius = 3.5f,
                center = offset,
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 24.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = points.first().label,
        style = MaterialTheme.typography.labelSmall,
        color = colors.textMuted,
      )
      if (points.size >= 3) {
        Text(
          text = points[points.size / 2].label,
          style = MaterialTheme.typography.labelSmall,
          color = colors.textMuted,
        )
      }
      if (points.size >= 2) {
        Text(
          text = points.last().label,
          style = MaterialTheme.typography.labelSmall,
          color = colors.textMuted,
        )
      }
    }

    if (hasMissingDays) {
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .background(lineColor, CircleShape),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = "Recorded",
          style = MaterialTheme.typography.labelSmall,
          color = colors.textMuted,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
          modifier = Modifier
            .size(7.dp)
            .border(1.2.dp, colors.borderBright, CircleShape),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = "No entry",
          style = MaterialTheme.typography.labelSmall,
          color = colors.textMuted,
        )
      }
    }
  }
}
