package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import com.example.ui.components.InsightMetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.TimeframeSelectorBar
import com.example.ui.theme.MoodyTheme
import com.example.ui.theme.moodColorInterpolated
import com.example.viewmodel.InsightTimeRange
import com.example.viewmodel.MoodAnalytics
import com.example.viewmodel.MoodyUiState

enum class InsightsCategory {
  MOOD,
  WELLBEING,
  SLEEP,
  HABITS,
  TAGS,
}

@Composable
fun InsightsScreen(
  uiState: MoodyUiState,
  onSelectRange: (InsightTimeRange) -> Unit,
  onSelectCategory: (InsightsCategory) -> Unit = {},
  listState: LazyListState = rememberLazyListState(),
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val avgMoodColor = uiState.stats.averageMood?.let { moodColorInterpolated(it) } ?: colors.accentPrimary
  val energySummary = uiState.wellbeingAnalytics.energy
  val avgEnergyColor = energySummary.average?.let { moodColorInterpolated(it) }
  val sleepSummary = uiState.sleepAnalytics
  val avgSleepColor = sleepSummary.averageQuality?.let { moodColorInterpolated(it) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            avgMoodColor.copy(alpha = if (colors.isDark) 0.12f else 0.08f),
            Color.Transparent,
          ),
          startY = 0f,
          endY = 560f,
        ),
      ),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp)
        .testTag("insights_overview_list"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
          Text(
            text = "INSIGHTS",
            style = MaterialTheme.typography.labelSmall,
            color = if (colors.isDark) avgMoodColor else colors.accentPrimary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Wellbeing Overview",
            style = MaterialTheme.typography.headlineLarge,
            color = colors.textPrimary,
          )
        }
      }

      item {
        TimeframeSelectorBar(
          selectedRange = uiState.insightRange,
          onSelectRange = onSelectRange,
        )
      }

      // A. Wellbeing Snapshot
      item {
        SectionHeader(
          title = "Snapshot",
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
          modifier = Modifier.testTag("insights_snapshot_header"),
        )
      }

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
            valueColor = when {
              uiState.stats.averageMood == null -> colors.textMuted
              colors.isDark -> avgMoodColor
              else -> colors.textPrimary
            },
            indicatorColor = uiState.stats.averageMood?.let { moodColorInterpolated(it) },
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("snapshot_avg_mood"),
          )
          InsightMetricTile(
            label = "Avg Energy",
            value = energySummary.average?.let { "${MoodAnalytics.formatScore(it)} / 5" } ?: "—",
            subtitle = if (energySummary.entryCount > 0) {
              "${energySummary.entryCount} ${if (energySummary.entryCount == 1) "entry" else "entries"}"
            } else {
              "No entries"
            },
            valueColor = when {
              avgEnergyColor == null -> colors.textMuted
              colors.isDark -> avgEnergyColor
              else -> colors.textPrimary
            },
            indicatorColor = avgEnergyColor,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("snapshot_avg_energy"),
          )
          InsightMetricTile(
            label = "Avg Sleep",
            value = sleepSummary.averageQuality?.let { "${MoodAnalytics.formatScore(it)} / 5" } ?: "—",
            subtitle = if (sleepSummary.qualityNightCount > 0) {
              "${sleepSummary.qualityNightCount} ${if (sleepSummary.qualityNightCount == 1) "night" else "nights"}"
            } else {
              "No nights"
            },
            valueColor = when {
              avgSleepColor == null -> colors.textMuted
              colors.isDark -> avgSleepColor
              else -> colors.textPrimary
            },
            indicatorColor = avgSleepColor,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("snapshot_avg_sleep"),
          )
        }
      }

      // B. Analytics Categories
      item {
        Spacer(modifier = Modifier.height(4.dp))
        SectionHeader(
          title = "Analytics Categories",
          modifier = Modifier.testTag("insights_categories_header"),
        )
      }

      item {
        InsightsCategoryCard(
          title = "Mood & Emotional Patterns",
          subtitle = "Mood trends, calendar heatmap & streak",
          icon = Icons.Outlined.AutoGraph,
          accentColor = avgMoodColor,
          onClick = { onSelectCategory(InsightsCategory.MOOD) },
          modifier = Modifier.testTag("category_mood"),
        )
      }

      item {
        InsightsCategoryCard(
          title = "Energy, Stress & Anxiety",
          subtitle = "Wellbeing averages, daily trends & weekday patterns",
          icon = Icons.Outlined.FavoriteBorder,
          accentColor = avgEnergyColor ?: colors.accentPrimary,
          onClick = { onSelectCategory(InsightsCategory.WELLBEING) },
          modifier = Modifier.testTag("category_wellbeing"),
        )
      }

      item {
        InsightsCategoryCard(
          title = "Sleep & Recovery",
          subtitle = "Sleep quality, duration history & weekday patterns",
          icon = Icons.Outlined.Bedtime,
          accentColor = avgSleepColor ?: colors.accentPrimary,
          onClick = { onSelectCategory(InsightsCategory.SLEEP) },
          modifier = Modifier.testTag("category_sleep"),
        )
      }

      item {
        InsightsCategoryCard(
          title = "Habits & Time of Day",
          subtitle = "Daily rhythm, logging frequency & consistency",
          icon = Icons.Outlined.Schedule,
          accentColor = colors.accentPrimary,
          onClick = { onSelectCategory(InsightsCategory.HABITS) },
          modifier = Modifier.testTag("category_habits"),
        )
      }

      item {
        val tagsSummary = uiState.tagsAnalytics
        val tagsSubtitle = if (tagsSummary.hasAnyTags) {
          val topDisplay = tagsSummary.topTags.firstOrNull()?.displayTag
          val countLabel = "${tagsSummary.distinctTagCount} ${if (tagsSummary.distinctTagCount == 1) "tag" else "tags"}"
          if (topDisplay != null) {
            "$countLabel · Top: #$topDisplay"
          } else {
            "Word cloud, top tags, trends & mood breakdown"
          }
        } else {
          "No tags recorded for this period."
        }
        val topTagColor = tagsSummary.topTags.firstOrNull()?.averageMood?.let { moodColorInterpolated(it) }

        InsightsCategoryCard(
          title = "Tags & Themes",
          subtitle = tagsSubtitle,
          icon = Icons.Outlined.Tag,
          accentColor = topTagColor ?: colors.accentPrimary,
          onClick = { onSelectCategory(InsightsCategory.TAGS) },
          modifier = Modifier.testTag("category_tags"),
        )
      }
    }
  }
}

@Composable
private fun InsightsCategoryCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val iconTint = if (colors.isDark) accentColor else colors.accentPrimary

  GlassCard(
    cornerRadius = 20.dp,
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .background(iconTint.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(21.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = colors.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = colors.textSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = "Open $title",
        tint = colors.textMuted,
      )
    }
  }
}
