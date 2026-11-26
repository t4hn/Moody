package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.ChartObservationBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.InsightMetricTile
import com.example.ui.components.InsightsCategoryHeader
import com.example.ui.components.MoodCalendarHeatmap
import com.example.ui.components.MoodLineChart
import com.example.ui.components.SleepDurationLineChart
import com.example.ui.components.SleepQualityByWeekdayChart
import com.example.ui.components.SleepQualityLineChart
import com.example.ui.components.TimeOfDayChart
import com.example.ui.components.TimeframeSelectorBar
import com.example.ui.components.WellbeingByWeekdayChart
import com.example.ui.components.WellbeingMetricLineChart
import com.example.ui.theme.MoodColor4
import com.example.ui.theme.MoodyTheme
import com.example.ui.theme.moodColorInterpolated
import com.example.viewmodel.InsightTimeRange
import com.example.viewmodel.MoodAnalytics
import com.example.viewmodel.MoodyUiState
import com.example.viewmodel.TimeOfDayMode
import com.example.viewmodel.WellbeingMetricSummary

@Composable
fun InsightsMoodScreen(
  uiState: MoodyUiState,
  onBack: () -> Unit,
  onSelectRange: (InsightTimeRange) -> Unit,
  onSelectHeatmapDay: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val avgColor = uiState.stats.averageMood?.let { moodColorInterpolated(it) } ?: colors.accentPrimary

  InsightsCategoryScaffold(
    title = "Mood & Emotional Patterns",
    glowAccent = avgColor,
    selectedRange = uiState.insightRange,
    onBack = onBack,
    onSelectRange = onSelectRange,
    modifier = modifier,
  ) {
    if (uiState.allEntries.isEmpty()) {
      item {
        CategoryEmptyCard(
          icon = Icons.Outlined.AutoGraph,
          title = "Nothing charted yet",
          subtitle = "Log a few moods in Today to explore your emotional patterns and trends.",
          testTag = "mood_empty_state",
        )
      }
    } else {
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          InsightMetricTile(
            label = "Avg Mood",
            value = uiState.stats.averageMood?.let { "${MoodAnalytics.formatScore(it)} / 5" } ?: "—",
            subtitle = "${uiState.stats.totalEntries} ${if (uiState.stats.totalEntries == 1) "entry" else "entries"}",
            valueColor = if (uiState.stats.averageMood != null) {
              if (colors.isDark) avgColor else colors.textPrimary
            } else {
              colors.textMuted
            },
            indicatorColor = uiState.stats.averageMood?.let { moodColorInterpolated(it) },
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("stat_avg_mood"),
          )
          InsightMetricTile(
            label = "Entries",
            value = uiState.stats.totalEntries.toString(),
            subtitle = uiState.insightRange.label,
            valueColor = colors.textPrimary,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("stat_total_entries"),
          )
          InsightMetricTile(
            label = "Streak",
            value = "${uiState.stats.currentStreakDays}d",
            subtitle = "Current",
            valueColor = if (uiState.stats.currentStreakDays > 0) MoodColor4 else colors.textMuted,
            showStreakIcon = uiState.stats.currentStreakDays > 0,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("stat_streak"),
          )
        }
      }

      item {
        GlassCard(
          glowColor = avgColor,
          cornerRadius = 24.dp,
          modifier = Modifier.testTag("mood_over_time_card"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
          ) {
            Text(
              text = "Mood Over Time",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            MoodLineChart(points = uiState.lineGraphPoints)
            ChartObservationBanner(observation = uiState.moodObservation)
          }
        }
      }

      item {
        GlassCard(
          cornerRadius = 24.dp,
          modifier = Modifier.testTag("mood_calendar_card"),
        ) {
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
    }
  }
}

@Composable
fun InsightsWellbeingScreen(
  uiState: MoodyUiState,
  onBack: () -> Unit,
  onSelectRange: (InsightTimeRange) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val wellbeing = uiState.wellbeingAnalytics

  InsightsCategoryScaffold(
    title = "Energy, Stress & Anxiety",
    glowAccent = colors.accentPrimary,
    selectedRange = uiState.insightRange,
    onBack = onBack,
    onSelectRange = onSelectRange,
    modifier = modifier,
  ) {
    if (!wellbeing.hasAnyWellbeingData) {
      item {
        CategoryEmptyCard(
          icon = Icons.Outlined.FavoriteBorder,
          title = "No wellbeing data recorded for this period.",
          subtitle = "Add energy, stress, and anxiety in Entry Details.",
          testTag = "wellbeing_empty_state",
        )
      }
    } else {
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          WellbeingCategoryTile(
            summary = wellbeing.energy,
            label = "Avg Energy",
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("wellbeing_stat_energy"),
          )
          WellbeingCategoryTile(
            summary = wellbeing.stress,
            label = "Avg Stress",
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("wellbeing_stat_stress"),
          )
          WellbeingCategoryTile(
            summary = wellbeing.anxiety,
            label = "Avg Anxiety",
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("wellbeing_stat_anxiety"),
          )
        }
      }

      item {
        GlassCard(
          cornerRadius = 24.dp,
          modifier = Modifier.testTag("wellbeing_history_energy"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
          ) {
            Text(
              text = "Energy Over Time",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            WellbeingMetricLineChart(summary = wellbeing.energy)
            ChartObservationBanner(observation = wellbeing.energy.observation)
          }
        }
      }

      item {
        GlassCard(
          cornerRadius = 24.dp,
          modifier = Modifier.testTag("wellbeing_history_stress"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
          ) {
            Text(
              text = "Stress Over Time",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            WellbeingMetricLineChart(summary = wellbeing.stress)
            ChartObservationBanner(observation = wellbeing.stress.observation)
          }
        }
      }

      item {
        GlassCard(
          cornerRadius = 24.dp,
          modifier = Modifier.testTag("wellbeing_history_anxiety"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
          ) {
            Text(
              text = "Anxiety Over Time",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            WellbeingMetricLineChart(summary = wellbeing.anxiety)
            ChartObservationBanner(observation = wellbeing.anxiety.observation)
          }
        }
      }

      item {
        GlassCard(
          cornerRadius = 24.dp,
          modifier = Modifier.testTag("wellbeing_weekday_card"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
          ) {
            Text(
              text = "Wellbeing by Weekday",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            WellbeingByWeekdayChart(weekdays = wellbeing.weekdayAverages)
          }
        }
      }
    }
  }
}

@Composable
fun InsightsSleepScreen(
  uiState: MoodyUiState,
  onBack: () -> Unit,
  onSelectRange: (InsightTimeRange) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val sleep = uiState.sleepAnalytics
  val qualityColor = sleep.averageQuality?.let { moodColorInterpolated(it) }

  InsightsCategoryScaffold(
    title = "Sleep & Recovery",
    glowAccent = qualityColor ?: colors.accentPrimary,
    selectedRange = uiState.insightRange,
    onBack = onBack,
    onSelectRange = onSelectRange,
    modifier = modifier,
  ) {
    if (!sleep.hasAnySleepData) {
      item {
        CategoryEmptyCard(
          icon = Icons.Outlined.Bedtime,
          title = "No sleep data recorded for this period.",
          subtitle = "Add sleep duration and quality in Entry Details.",
          testTag = "sleep_empty_state",
        )
      }
    } else {
      item {
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
          InsightMetricTile(
            label = "Avg Sleep Quality",
            value = sleep.averageQuality?.let { "${MoodAnalytics.formatScore(it)} / 5" } ?: "—",
            subtitle = qualityCountLabel,
            valueColor = when {
              qualityColor == null -> colors.textMuted
              colors.isDark -> qualityColor
              else -> colors.textPrimary
            },
            indicatorColor = qualityColor,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("sleep_stat_avg_quality"),
          )
          InsightMetricTile(
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
            ChartObservationBanner(observation = sleep.qualityObservation)
          }
        }
      }

      item {
        GlassCard(
          cornerRadius = 24.dp,
          modifier = Modifier.testTag("sleep_duration_history_card"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
          ) {
            Text(
              text = "Sleep Duration History",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            SleepDurationLineChart(points = sleep.durationHistory)
            ChartObservationBanner(observation = sleep.durationObservation)
          }
        }
      }

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

@Composable
fun InsightsHabitsScreen(
  uiState: MoodyUiState,
  onBack: () -> Unit,
  onSelectRange: (InsightTimeRange) -> Unit,
  onSelectTimeOfDayMode: (TimeOfDayMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val habits = uiState.habitsAnalytics

  InsightsCategoryScaffold(
    title = "Habits & Time of Day",
    glowAccent = colors.accentPrimary,
    selectedRange = uiState.insightRange,
    onBack = onBack,
    onSelectRange = onSelectRange,
    modifier = modifier,
  ) {
    if (habits.totalEntriesInRange == 0) {
      item {
        CategoryEmptyCard(
          icon = Icons.Outlined.Schedule,
          title = "No mood logs recorded for this period.",
          subtitle = "Log moods in Today to reveal your daily rhythm and consistency.",
          testTag = "habits_empty_state",
        )
      }
    } else {
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          InsightMetricTile(
            label = "Active Days",
            value = "${habits.activeDaysInRange}/${habits.windowDays}",
            subtitle = "Days logged",
            valueColor = colors.textPrimary,
            indicatorColor = colors.accentPrimary,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("habits_stat_active_days"),
          )
          InsightMetricTile(
            label = "Consistency",
            value = "${habits.consistencyPercent}%",
            subtitle = uiState.insightRange.label,
            valueColor = if (habits.consistencyPercent >= 50) MoodColor4 else colors.textPrimary,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("habits_stat_consistency"),
          )
          InsightMetricTile(
            label = "Streak",
            value = "${habits.currentStreakDays}d",
            subtitle = "Current",
            valueColor = if (habits.currentStreakDays > 0) MoodColor4 else colors.textMuted,
            showStreakIcon = habits.currentStreakDays > 0,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("habits_stat_streak"),
          )
        }
      }

      item {
        GlassCard(
          cornerRadius = 24.dp,
          modifier = Modifier.testTag("habits_time_of_day_card"),
        ) {
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
              buckets = habits.timeOfDayBuckets,
              mode = uiState.timeOfDayMode,
            )
            ChartObservationBanner(observation = habits.timeOfDayObservation)
          }
        }
      }

      item {
        GlassCard(
          cornerRadius = 24.dp,
          modifier = Modifier.testTag("habits_frequency_card"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "Logging Frequency",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            HabitStatRow(
              label = "Total entries in period",
              value = "${habits.totalEntriesInRange} ${if (habits.totalEntriesInRange == 1) "entry" else "entries"}",
            )
            HorizontalDivider(color = colors.border)
            HabitStatRow(
              label = "Average per active day",
              value = "${MoodAnalytics.formatScore(habits.averageEntriesPerActiveDay)} logs",
            )
            habits.mostActivePeriod?.let { period ->
              HorizontalDivider(color = colors.border)
              HabitStatRow(
                label = "Most active time",
                value = period,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun InsightsCategoryScaffold(
  title: String,
  glowAccent: Color,
  selectedRange: InsightTimeRange,
  onBack: () -> Unit,
  onSelectRange: (InsightTimeRange) -> Unit,
  modifier: Modifier = Modifier,
  content: LazyListScope.() -> Unit,
) {
  val colors = MoodyTheme.colors

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            glowAccent.copy(alpha = if (colors.isDark) 0.12f else 0.08f),
            Color.Transparent,
          ),
          startY = 0f,
          endY = 520f,
        ),
      ),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp)
        .testTag("insights_list"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        InsightsCategoryHeader(
          title = title,
          onBack = onBack,
          accentColor = if (colors.isDark) glowAccent else colors.accentPrimary,
        )
      }
      item {
        TimeframeSelectorBar(
          selectedRange = selectedRange,
          onSelectRange = onSelectRange,
        )
      }
      content()
    }
  }
}

@Composable
private fun WellbeingCategoryTile(
  summary: WellbeingMetricSummary,
  label: String,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val indicatorColor = summary.average?.let {
    moodColorInterpolated(it, invertScale = summary.metric.invertSemanticScale)
  }
  val countLabel = if (summary.entryCount > 0) {
    "${summary.entryCount} ${if (summary.entryCount == 1) "entry" else "entries"}"
  } else {
    "No entries"
  }

  InsightMetricTile(
    label = label,
    value = summary.average?.let { "${MoodAnalytics.formatScore(it)} / 5" } ?: "—",
    subtitle = countLabel,
    valueColor = when {
      indicatorColor == null -> colors.textMuted
      colors.isDark -> indicatorColor
      else -> colors.textPrimary
    },
    indicatorColor = indicatorColor,
    modifier = modifier,
  )
}

@Composable
private fun CategoryEmptyCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  testTag: String,
) {
  val colors = MoodyTheme.colors
  GlassCard(
    cornerRadius = 24.dp,
    modifier = Modifier.testTag(testTag),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(colors.accentPrimary.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = colors.accentPrimary,
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = colors.textPrimary,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = colors.textMuted,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun HabitStatRow(
  label: String,
  value: String,
) {
  val colors = MoodyTheme.colors
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = colors.textSecondary,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      color = colors.textPrimary,
    )
  }
}
