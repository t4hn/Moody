package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import com.example.ui.components.MoodCalendarHeatmap
import com.example.ui.components.MoodLineChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.SleepQualityByWeekdayChart
import com.example.ui.components.SleepQualityLineChart
import com.example.ui.components.TimeOfDayChart
import com.example.ui.theme.MoodColor4
import com.example.ui.theme.MoodyTheme
import com.example.ui.theme.moodColorInterpolated
import com.example.viewmodel.InsightTimeRange
import com.example.viewmodel.MoodAnalytics
import com.example.viewmodel.MoodyUiState
import com.example.viewmodel.TimeOfDayMode

@Composable
fun InsightsScreen(
  uiState: MoodyUiState,
  onSelectRange: (InsightTimeRange) -> Unit,
  onSelectHeatmapDay: (Long) -> Unit,
  onSelectTimeOfDayMode: (TimeOfDayMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val avgColor = uiState.stats.averageMood?.let { moodColorInterpolated(it) } ?: colors.accentPrimary

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            avgColor.copy(alpha = if (colors.isDark) 0.12f else 0.08f),
            Color.Transparent,
          ),
          startY = 0f,
          endY = 580f,
        ),
      ),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
          Text(
            text = "INSIGHTS",
            style = MaterialTheme.typography.labelSmall,
            color = if (colors.isDark) avgColor else colors.accentPrimary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Emotional Patterns",
            style = MaterialTheme.typography.displaySmall,
            color = colors.textPrimary,
          )
        }
      }

      // Stats Row
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          StatGlassTile(
            label = "Avg Mood",
            value = MoodAnalytics.formatScore(uiState.stats.averageMood),
            accent = if (colors.isDark) avgColor else colors.accentPrimary,
            modifier = Modifier
              .weight(1f)
              .testTag("stat_avg_mood"),
          )
          StatGlassTile(
            label = "Entries",
            value = uiState.stats.totalEntries.toString(),
            accent = colors.textPrimary,
            modifier = Modifier
              .weight(1f)
              .testTag("stat_total_entries"),
          )
          StatGlassTile(
            label = "Streak",
            value = "${uiState.stats.currentStreakDays}d",
            accent = if (uiState.stats.currentStreakDays > 0) MoodColor4 else colors.textMuted,
            showStreakIcon = uiState.stats.currentStreakDays > 0,
            modifier = Modifier
              .weight(1f)
              .testTag("stat_streak"),
          )
        }
      }

      if (uiState.allEntries.isEmpty()) {
        item {
          GlassCard(cornerRadius = 24.dp) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Box(
                modifier = Modifier
                  .size(52.dp)
                  .background(colors.accentPrimary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.AutoGraph,
                  contentDescription = null,
                  tint = colors.accentPrimary,
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Nothing charted yet",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Log a few moods in Today to explore your emotional patterns and trends.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      } else {
        // Line Graph Card
        item {
          GlassCard(glowColor = avgColor, cornerRadius = 24.dp) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Mood Over Time",
                  style = MaterialTheme.typography.titleLarge,
                  color = colors.textPrimary,
                )
              }
              Spacer(modifier = Modifier.height(12.dp))

              // Day / Week / Month / All toggle
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(colors.chartTrack)
                  .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                InsightTimeRange.entries.forEach { range ->
                  val selected = uiState.insightRange == range
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (selected) colors.cardSurfaceElevated else Color.Transparent)
                      .clickable { onSelectRange(range) }
                      .padding(vertical = 8.dp)
                      .testTag("range_${range.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = range.label,
                      style = MaterialTheme.typography.labelMedium,
                      color = if (selected) colors.textPrimary else colors.textSecondary,
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(16.dp))
              MoodLineChart(points = uiState.lineGraphPoints)
            }
          }
        }

        // Calendar Heatmap Card
        item {
          GlassCard(cornerRadius = 24.dp) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Mood Calendar",
                  style = MaterialTheme.typography.titleLarge,
                  color = colors.textPrimary,
                )
                Text(
                  text = "12 weeks",
                  style = MaterialTheme.typography.labelSmall,
                  color = colors.textMuted,
                )
              }
              Spacer(modifier = Modifier.height(14.dp))
              MoodCalendarHeatmap(
                cells = uiState.heatmapCells,
                selectedDay = uiState.selectedHeatmapDay,
                onSelectDay = onSelectHeatmapDay,
              )
            }
          }
        }

        // Time of Day Card
        item {
          GlassCard(cornerRadius = 24.dp) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Time of Day",
                  style = MaterialTheme.typography.titleLarge,
                  color = colors.textPrimary,
                )
                Row(
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.chartTrack)
                    .padding(3.dp),
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  TimeOfDayMode.entries.forEach { mode ->
                    val selected = uiState.timeOfDayMode == mode
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) colors.cardSurfaceElevated else Color.Transparent)
                        .clickable { onSelectTimeOfDayMode(mode) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("tod_mode_${mode.name.lowercase()}"),
                    ) {
                      Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) colors.textPrimary else colors.textMuted,
                      )
                    }
                  }
                }
              }
              Spacer(modifier = Modifier.height(16.dp))
              TimeOfDayChart(
                buckets = uiState.timeOfDayBuckets,
                mode = uiState.timeOfDayMode,
              )
            }
          }
        }
      }

      if (uiState.allEntries.isNotEmpty() || uiState.sleepAnalytics.hasAnySleepData) {
        val sleep = uiState.sleepAnalytics

        item {
          Spacer(modifier = Modifier.height(6.dp))
          SectionHeader(
            title = "Sleep & Recovery",
            trailing = {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.cardSurfaceElevated,
                border = BorderStroke(1.dp, colors.border),
              ) {
                Text(
                  text = uiState.insightRange.label,
                  style = MaterialTheme.typography.labelSmall,
                  color = colors.textSecondary,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
              }
            },
            modifier = Modifier.testTag("sleep_recovery_header"),
          )
        }

        if (!sleep.hasAnySleepData) {
          item {
            GlassCard(
              cornerRadius = 24.dp,
              modifier = Modifier.testTag("sleep_empty_state"),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .background(colors.accentPrimary.copy(alpha = 0.14f), CircleShape),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Bedtime,
                    contentDescription = null,
                    tint = colors.accentPrimary,
                  )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "No sleep data recorded for this period.",
                  style = MaterialTheme.typography.titleMedium,
                  color = colors.textPrimary,
                  textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Add sleep duration and quality in Entry Details.",
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.textMuted,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
        } else {
          // A & C. Average Sleep Quality & Average Sleep Duration
          item {
            val qualityColor = sleep.averageQuality?.let { moodColorInterpolated(it) }
            val qualityTextAccent = when {
              qualityColor == null -> colors.textMuted
              colors.isDark -> qualityColor
              else -> colors.textPrimary
            }
            val qualityCountLabel = if (sleep.qualityNightCount > 0) {
              "${sleep.qualityNightCount} ${if (sleep.qualityNightCount == 1) "night" else "nights"}"
            } else {
              "No nights recorded"
            }
            val durationCountLabel = if (sleep.durationNightCount > 0) {
              "${sleep.durationNightCount} ${if (sleep.durationNightCount == 1) "night" else "nights"}"
            } else {
              "No nights recorded"
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              SleepStatTile(
                label = "Avg Sleep Quality",
                value = sleep.averageQuality?.let { "${MoodAnalytics.formatScore(it)} / 5" } ?: "—",
                subtitle = qualityCountLabel,
                valueColor = qualityTextAccent,
                indicatorColor = qualityColor,
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
                  .testTag("sleep_stat_avg_quality"),
              )
              SleepStatTile(
                label = "Avg Sleep Duration",
                value = MoodAnalytics.formatSleepDuration(sleep.averageDurationMinutes),
                subtitle = durationCountLabel,
                valueColor = if (sleep.averageDurationMinutes != null) colors.accentPrimary else colors.textMuted,
                indicatorColor = if (sleep.averageDurationMinutes != null) colors.accentPrimary else null,
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
                  .testTag("sleep_stat_avg_duration"),
              )
            }
          }

          // B. Sleep Quality History Graph
          item {
            GlassCard(
              cornerRadius = 24.dp,
              modifier = Modifier.testTag("sleep_quality_history_card"),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(18.dp),
              ) {
                Text(
                  text = "Sleep Quality History",
                  style = MaterialTheme.typography.titleLarge,
                  color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(14.dp))
                SleepQualityLineChart(points = sleep.qualityHistory)
              }
            }
          }

          // D. Sleep Quality by Weekday
          item {
            GlassCard(
              cornerRadius = 24.dp,
              modifier = Modifier.testTag("sleep_weekday_card"),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(18.dp),
              ) {
                Text(
                  text = "Sleep Quality by Weekday",
                  style = MaterialTheme.typography.titleLarge,
                  color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(14.dp))
                SleepQualityByWeekdayChart(weekdays = sleep.weekdayAverages)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SleepStatTile(
  label: String,
  value: String,
  subtitle: String,
  valueColor: Color,
  indicatorColor: Color?,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = colors.cardSurfaceElevated.copy(alpha = if (colors.isDark) 0.72f else 0.92f),
    border = BorderStroke(1.dp, colors.border),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (indicatorColor != null) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(indicatorColor, CircleShape),
          )
          Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
          text = label,
          style = MaterialTheme.typography.bodySmall,
          color = colors.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        color = valueColor,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = colors.textMuted,
      )
    }
  }
}

@Composable
private fun StatGlassTile(
  label: String,
  value: String,
  accent: Color,
  showStreakIcon: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = colors.cardSurfaceElevated.copy(alpha = if (colors.isDark) 0.72f else 0.92f),
    border = BorderStroke(1.dp, colors.border),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = label,
          style = MaterialTheme.typography.bodySmall,
          color = colors.textSecondary,
        )
        if (showStreakIcon) {
          Spacer(modifier = Modifier.padding(start = 4.dp))
          Icon(
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        color = accent,
      )
    }
  }
}
