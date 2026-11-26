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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MoodyTheme
import com.example.ui.theme.moodColorInterpolated
import com.example.viewmodel.MoodAnalytics
import com.example.viewmodel.WeekdayWellbeingAverage
import com.example.viewmodel.WellbeingMetric
import com.example.viewmodel.WellbeingMetricSummary

@Composable
fun WellbeingMetricLineChart(
  summary: WellbeingMetricSummary,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val points = summary.history
  val invertScale = summary.metric.invertSemanticScale
  val hasRecorded = remember(points) { points.any { it.averageValue != null } }

  if (!summary.hasData || points.isEmpty() || !hasRecorded) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .height(130.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "No ${summary.metric.label.lowercase()} data recorded for this period.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textMuted,
        textAlign = TextAlign.Center,
      )
    }
    return
  }

  val defaultIndex = remember(points) {
    points.indexOfLast { it.averageValue != null }.coerceAtLeast(0)
  }
  var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
  val activeIdx = (selectedIndex ?: defaultIndex).coerceIn(0, points.lastIndex)
  val activePoint = points[activeIdx]
  val activeValue = activePoint.averageValue
  val activeColor = activeValue?.let { moodColorInterpolated(it, invertScale = invertScale) }
  val hasMissingDays = remember(points) { points.any { it.averageValue == null } }

  Column(modifier = modifier.fillMaxWidth()) {
    // Interactive date & value readout bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (activeColor != null) {
          Box(
            modifier = Modifier
              .size(9.dp)
              .background(activeColor, CircleShape),
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
        text = if (activeValue != null) {
          "${MoodAnalytics.formatScore(activeValue)} / 5" +
            if (activePoint.entryCount > 1) " · ${activePoint.entryCount} logs" else ""
        } else {
          "Not recorded"
        },
        style = MaterialTheme.typography.labelSmall,
        color = when {
          activeColor == null -> colors.textMuted
          colors.isDark -> activeColor
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
      // Y-axis numbers 5..1 with semantic polarity per metric
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
            color = moodColorInterpolated(level.toFloat(), invertScale = invertScale)
              .copy(alpha = if (colors.isDark) 0.78f else 0.95f),
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
        for (lvl in 1..5) {
          val norm = (lvl - 1f) / 4f
          val y = padTop + (1f - norm) * plotH
          drawLine(
            color = colors.chartGrid,
            start = Offset(padLeft, y),
            end = Offset(size.width - padRight, y),
            strokeWidth = 1.2f,
            pathEffect = dashEffect,
          )
        }

        val xForIndex: (Int) -> Float = { idx ->
          if (points.size == 1) {
            padLeft + plotW / 2f
          } else {
            padLeft + (idx.toFloat() / (points.size - 1).toFloat()) * plotW
          }
        }

        val coords: List<Offset?> = points.mapIndexed { idx, pt ->
          pt.averageValue?.let { v ->
            val norm = ((v - 1f) / 4f).coerceIn(0f, 1f)
            Offset(xForIndex(idx), padTop + (1f - norm) * plotH)
          }
        }

        // Draw segments ONLY between consecutive recorded calendar days (never across gaps)
        for (i in 0 until points.size - 1) {
          val p0 = coords[i]
          val p1 = coords[i + 1]
          val v0 = points[i].averageValue
          val v1 = points[i + 1].averageValue
          if (p0 != null && p1 != null && v0 != null && v1 != null) {
            val c0 = moodColorInterpolated(v0, invertScale = invertScale)
            val c1 = moodColorInterpolated(v1, invertScale = invertScale)
            drawLine(
              brush = Brush.linearGradient(
                colors = listOf(c0, c1),
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

        // Vertical guide for selected/active day
        val activeX = xForIndex(activeIdx)
        drawLine(
          color = (activeColor ?: colors.textMuted).copy(alpha = 0.30f),
          start = Offset(activeX, padTop),
          end = Offset(activeX, padTop + plotH),
          strokeWidth = 2f,
        )

        // Plot recorded dots and baseline markers for unrecorded days
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
            val v = pt.averageValue ?: return@forEachIndexed
            val c = moodColorInterpolated(v, invertScale = invertScale)
            if (isHighlighted) {
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
                color = colors.surfaceBase,
                radius = 3.5f,
                center = offset,
              )
            }
          }
        }
      }
    }

    // X-axis date labels
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
      val legendDotColor = moodColorInterpolated(
        value = if (invertScale) 2f else 4f,
        invertScale = invertScale,
      )
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .background(legendDotColor, CircleShape),
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

@Composable
fun WellbeingByWeekdayChart(
  weekdays: List<WeekdayWellbeingAverage>,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val hasAnyData = remember(weekdays) { weekdays.any { it.hasAnyData } }

  if (!hasAnyData) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .height(140.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "No wellbeing data recorded for this period.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textMuted,
        textAlign = TextAlign.Center,
      )
    }
    return
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Key legend for the three horizontal bars per weekday
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      WellbeingMetric.entries.forEach { metric ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(5.dp),
            color = colors.cardSurfaceElevated,
          ) {
            Text(
              text = metric.shortTag,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = colors.textSecondary,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            )
          }
          Text(
            text = metric.label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
          )
        }
      }
    }

    weekdays.forEach { item ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = item.weekdayLabel,
          style = MaterialTheme.typography.bodySmall,
          color = if (item.hasAnyData) colors.textPrimary else colors.textMuted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.width(82.dp),
        )

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          WeekdayMetricBarRow(
            metric = WellbeingMetric.ENERGY,
            average = item.energyAverage,
            weekdayLabel = item.weekdayLabel,
          )
          WeekdayMetricBarRow(
            metric = WellbeingMetric.STRESS,
            average = item.stressAverage,
            weekdayLabel = item.weekdayLabel,
          )
          WeekdayMetricBarRow(
            metric = WellbeingMetric.ANXIETY,
            average = item.anxietyAverage,
            weekdayLabel = item.weekdayLabel,
          )
        }
      }
    }
  }
}

@Composable
private fun WeekdayMetricBarRow(
  metric: WellbeingMetric,
  average: Float?,
  weekdayLabel: String,
) {
  val colors = MoodyTheme.colors
  val barColor = average?.let {
    moodColorInterpolated(it, invertScale = metric.invertSemanticScale)
  }
  val fraction = average?.let { (it / 5f).coerceIn(0.08f, 1f) } ?: 0f
  val formatted = MoodAnalytics.formatScore(average)
  val a11yDescription = if (average != null) {
    "$weekdayLabel ${metric.label} average $formatted out of 5"
  } else {
    "$weekdayLabel ${metric.label} no recorded entries"
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .semantics(mergeDescendants = true) {
        contentDescription = a11yDescription
      },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = metric.shortTag,
      style = MaterialTheme.typography.labelSmall,
      color = colors.textMuted,
      modifier = Modifier.width(12.dp),
    )

    Box(
      modifier = Modifier
        .weight(1f)
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(colors.chartTrack)
        .border(0.5.dp, colors.border, RoundedCornerShape(4.dp)),
    ) {
      if (average != null && barColor != null && fraction > 0f) {
        Box(
          modifier = Modifier
            .fillMaxWidth(fraction)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  barColor.copy(alpha = 0.65f),
                  barColor,
                ),
              ),
            ),
        )
      }
    }

    Text(
      text = formatted,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = if (average != null) FontWeight.SemiBold else FontWeight.Normal,
      ),
      color = when {
        barColor == null -> colors.textMuted
        colors.isDark -> barColor
        else -> colors.textPrimary
      },
      textAlign = TextAlign.End,
      modifier = Modifier.width(30.dp),
    )
  }
}
