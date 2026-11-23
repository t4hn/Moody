package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.MoodColor1
import com.example.ui.theme.MoodColor2
import com.example.ui.theme.MoodColor3
import com.example.ui.theme.MoodColor4
import com.example.ui.theme.MoodColor5
import com.example.ui.theme.MoodSpectrumStops
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.moodColorInterpolated
import com.example.viewmodel.HeatmapDayCell
import com.example.viewmodel.MoodAnalytics
import com.example.viewmodel.MoodGraphPoint
import com.example.viewmodel.TimeOfDayBucket
import com.example.viewmodel.TimeOfDayMode
import kotlin.math.abs

@Composable
fun MoodLineChart(
  points: List<MoodGraphPoint>,
  modifier: Modifier = Modifier,
) {
  var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

  if (points.isEmpty()) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .height(190.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "No mood entries in this window yet",
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted,
      )
    }
    return
  }

  val activePoint = selectedIndex?.let { points.getOrNull(it) } ?: points.last()
  val activeColor = moodColorInterpolated(activePoint.value)

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
        Box(
          modifier = Modifier
            .size(9.dp)
            .background(activeColor, CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = activePoint.label,
          style = MaterialTheme.typography.labelMedium,
          color = TextSecondary,
        )
      }
      Text(
        text = "${MoodAnalytics.formatScore(activePoint.value)} / 5.0" +
          if (activePoint.entryCount > 1) " · ${activePoint.entryCount} logs" else "",
        style = MaterialTheme.typography.labelSmall,
        color = activeColor,
      )
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(175.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Y-axis numbers 5..1
      Column(
        modifier = Modifier
          .height(155.dp)
          .padding(end = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
      ) {
        for (level in 5 downTo 1) {
          Text(
            text = level.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = moodColorInterpolated(level.toFloat()).copy(alpha = 0.78f),
          )
        }
      }

      Canvas(
        modifier = Modifier
          .weight(1f)
          .height(165.dp)
          .pointerInput(points) {
            detectTapGestures { offset ->
              val idx = findClosestPointIndex(offset.x, size.width.toFloat(), points.size)
              selectedIndex = idx
            }
          }
          .pointerInput(points) {
            detectHorizontalDragGestures { change, _ ->
              change.consume()
              val idx = findClosestPointIndex(change.position.x, size.width.toFloat(), points.size)
              selectedIndex = idx
            }
          },
      ) {
        val padTop = 14f
        val padBottom = 14f
        val padLeft = 10f
        val padRight = 10f
        val plotW = (size.width - padLeft - padRight).coerceAtLeast(1f)
        val plotH = (size.height - padTop - padBottom).coerceAtLeast(1f)

        // Horizontal dashed level guides 1..5
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f), 0f)
        for (lvl in 1..5) {
          val norm = (lvl - 1f) / 4f
          val y = padTop + (1f - norm) * plotH
          drawLine(
            color = Color.White.copy(alpha = 0.07f),
            start = Offset(padLeft, y),
            end = Offset(size.width - padRight, y),
            strokeWidth = 1.2f,
            pathEffect = dashEffect,
          )
        }

        val coords = points.mapIndexed { idx, pt ->
          val x = if (points.size == 1) {
            padLeft + plotW / 2f
          } else {
            padLeft + (idx.toFloat() / (points.size - 1).toFloat()) * plotW
          }
          val norm = ((pt.value - 1f) / 4f).coerceIn(0f, 1f)
          val y = padTop + (1f - norm) * plotH
          Offset(x, y)
        }

        if (coords.size >= 2) {
          val linePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 0 until coords.size - 1) {
              val p0 = coords[i]
              val p1 = coords[i + 1]
              val cx = (p0.x + p1.x) / 2f
              cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
          }

          val fillPath = Path().apply {
            addPath(linePath)
            lineTo(coords.last().x, padTop + plotH)
            lineTo(coords.first().x, padTop + plotH)
            close()
          }

          drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
              colors = listOf(
                MoodColor5.copy(alpha = 0.20f),
                MoodColor3.copy(alpha = 0.12f),
                MoodColor1.copy(alpha = 0.02f),
                Color.Transparent,
              ),
              startY = padTop,
              endY = padTop + plotH,
            ),
          )

          drawPath(
            path = linePath,
            brush = Brush.verticalGradient(
              colors = listOf(MoodColor5, MoodColor4, MoodColor3, MoodColor2, MoodColor1),
              startY = padTop,
              endY = padTop + plotH,
            ),
            style = Stroke(
              width = 5.5f,
              cap = StrokeCap.Round,
              join = StrokeJoin.Round,
            ),
          )
        }

        // Draw point dots
        val highlightIdx = selectedIndex ?: (coords.size - 1)
        coords.forEachIndexed { idx, offset ->
          val c = moodColorInterpolated(points[idx].value)
          val isHighlighted = idx == highlightIdx
          if (isHighlighted) {
            drawLine(
              color = c.copy(alpha = 0.35f),
              start = Offset(offset.x, padTop),
              end = Offset(offset.x, padTop + plotH),
              strokeWidth = 2f,
            )
            drawCircle(
              color = c.copy(alpha = 0.28f),
              radius = 18f,
              center = offset,
            )
          }
          drawCircle(
            color = c,
            radius = if (isHighlighted) 8.5f else 5f,
            center = offset,
          )
          if (isHighlighted) {
            drawCircle(
              color = Color(0xFF0C0E14),
              radius = 3.5f,
              center = offset,
            )
          }
        }
      }
    }

    // X-axis first / middle / last labels
    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 18.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = points.first().label,
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
      )
      if (points.size >= 3) {
        Text(
          text = points[points.size / 2].label,
          style = MaterialTheme.typography.labelSmall,
          color = TextMuted,
        )
      }
      if (points.size >= 2) {
        Text(
          text = points.last().label,
          style = MaterialTheme.typography.labelSmall,
          color = TextMuted,
        )
      }
    }
  }
}

private fun findClosestPointIndex(x: Float, width: Float, count: Int): Int {
  if (count <= 1) return 0
  val pad = 10f
  val plotW = (width - pad * 2).coerceAtLeast(1f)
  var bestIdx = 0
  var bestDist = Float.MAX_VALUE
  for (i in 0 until count) {
    val px = pad + (i.toFloat() / (count - 1).toFloat()) * plotW
    val d = abs(x - px)
    if (d < bestDist) {
      bestDist = d
      bestIdx = i
    }
  }
  return bestIdx
}

@Composable
fun MoodCalendarHeatmap(
  cells: List<HeatmapDayCell>,
  selectedDay: HeatmapDayCell?,
  onSelectDay: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val weeks = remember(cells) { cells.groupBy { it.weekColumn }.toSortedMap() }
  val weekDayLabels = listOf("M", "", "W", "", "F", "", "S")

  Column(modifier = modifier.fillMaxWidth()) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val colCount = weeks.size.coerceAtLeast(1)
      val labelWidth = 18.dp
      val spacing = 4.dp
      val cellSize = ((maxWidth - labelWidth - spacing * colCount) / colCount).coerceIn(14.dp, 26.dp)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        // Weekday labels column
        Column(
          modifier = Modifier.width(labelWidth),
          verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
          weekDayLabels.forEach { label ->
            Box(
              modifier = Modifier.height(cellSize),
              contentAlignment = Alignment.CenterStart,
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
              )
            }
          }
        }

        // Week columns
        weeks.values.forEach { weekCells ->
          Column(
            verticalArrangement = Arrangement.spacedBy(spacing),
          ) {
            weekCells.sortedBy { it.dayOfWeekRow }.forEach { cell ->
              val isSelected = selectedDay?.dayStartMillis == cell.dayStartMillis
              val fillColor = when {
                cell.isFuture -> Color.Transparent
                cell.averageMood != null -> moodColorInterpolated(cell.averageMood)
                else -> Color.White.copy(alpha = 0.05f)
              }

              Box(
                modifier = Modifier
                  .size(cellSize)
                  .clip(RoundedCornerShape(5.dp))
                  .background(
                    if (cell.averageMood != null) {
                      fillColor.copy(alpha = 0.85f)
                    } else {
                      fillColor
                    },
                  )
                  .then(
                    if (isSelected) {
                      Modifier.border(1.5.dp, TextPrimary, RoundedCornerShape(5.dp))
                    } else if (cell.isToday && !cell.isFuture) {
                      Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(5.dp))
                    } else {
                      Modifier
                    },
                  )
                  .then(
                    if (!cell.isFuture) {
                      Modifier.clickable { onSelectDay(cell.dayStartMillis) }
                    } else {
                      Modifier
                    },
                  ),
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Selected day readout + legend
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (selectedDay != null) {
        val avgText = selectedDay.averageMood?.let {
          "Avg ${MoodAnalytics.formatScore(it)} · ${selectedDay.entryCount} ${if (selectedDay.entryCount == 1) "entry" else "entries"}"
        } ?: "No entries"
        Text(
          text = "${com.example.util.DateTimeUtils.formatShortDate(selectedDay.dayStartMillis)} · $avgText",
          style = MaterialTheme.typography.bodySmall,
          color = selectedDay.averageMood?.let { moodColorInterpolated(it) } ?: TextSecondary,
        )
      } else {
        Spacer(modifier = Modifier.width(1.dp))
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text("Low", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        MoodSpectrumStops.forEach { color ->
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(color),
          )
        }
        Text("High", style = MaterialTheme.typography.labelSmall, color = TextMuted)
      }
    }
  }
}

@Composable
fun TimeOfDayChart(
  buckets: List<TimeOfDayBucket>,
  mode: TimeOfDayMode,
  modifier: Modifier = Modifier,
) {
  if (buckets.all { it.entryCount == 0 }) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .height(150.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "Log moods at different hours to see your daily rhythm",
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted,
      )
    }
    return
  }

  if (mode == TimeOfDayMode.PART_OF_DAY) {
    Row(
      modifier = modifier
        .fillMaxWidth()
        .height(165.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Bottom,
    ) {
      buckets.forEach { bucket ->
        val avg = bucket.averageMood
        val barColor = avg?.let { moodColorInterpolated(it) } ?: GlassSurfaceElevated
        val fraction = avg?.let { (it / 5f).coerceIn(0.15f, 1f) } ?: 0.08f

        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Bottom,
        ) {
          Text(
            text = MoodAnalytics.formatScore(avg),
            style = MaterialTheme.typography.labelSmall,
            color = if (avg != null) barColor else TextMuted,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth(0.68f)
              .height(92.dp),
            contentAlignment = Alignment.BottomCenter,
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
            )
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height((92f * fraction).dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                  Brush.verticalGradient(
                    colors = listOf(
                      barColor.copy(alpha = if (avg != null) 0.92f else 0.2f),
                      barColor.copy(alpha = if (avg != null) 0.42f else 0.1f),
                    ),
                  ),
                ),
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = bucket.label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
          )
          Text(
            text = "${bucket.entryCount} ${if (bucket.entryCount == 1) "log" else "logs"}",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
          )
        }
      }
    }
  } else {
    Column(modifier = modifier.fillMaxWidth()) {
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp),
      ) {
        val count = buckets.size.coerceAtLeast(1)
        val gap = 4f
        val barW = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2f)
        buckets.forEachIndexed { idx, bucket ->
          val x = idx * (barW + gap)
          val avg = bucket.averageMood
          val fraction = avg?.let { (it / 5f).coerceIn(0.12f, 1f) } ?: 0.06f
          val barH = size.height * fraction
          val color = avg?.let { moodColorInterpolated(it) } ?: Color.White.copy(alpha = 0.07f)

          drawRoundRect(
            color = Color.White.copy(alpha = 0.04f),
            topLeft = Offset(x, 0f),
            size = Size(barW, size.height),
            cornerRadius = CornerRadius(6f, 6f),
          )
          drawRoundRect(
            brush = Brush.verticalGradient(
              colors = listOf(color, color.copy(alpha = 0.45f)),
              startY = size.height - barH,
              endY = size.height,
            ),
            topLeft = Offset(x, size.height - barH),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(6f, 6f),
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        listOf("12a", "6a", "12p", "6p", "11p").forEach { lbl ->
          Text(text = lbl, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
      }
    }
  }
}
