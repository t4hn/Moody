package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.ChartObservationBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.HashtagWordCloud
import com.example.ui.components.InsightMetricTile
import com.example.ui.components.MoodDistributionByTagContent
import com.example.ui.components.RankedTagsList
import com.example.ui.components.TagDetailBottomSheet
import com.example.ui.components.TagDistributionSelectorRow
import com.example.ui.components.TagTrendsList
import com.example.ui.theme.MoodyTheme
import com.example.ui.theme.moodColorInterpolated
import com.example.viewmodel.InsightTimeRange
import com.example.viewmodel.MoodyUiState
import kotlin.math.roundToInt

@Composable
fun InsightsTagsScreen(
  uiState: MoodyUiState,
  onBack: () -> Unit,
  onSelectRange: (InsightTimeRange) -> Unit,
  onSelectTag: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val tagsSummary = uiState.tagsAnalytics
  val selectedTagStat = remember(tagsSummary.topTags, uiState.selectedInsightTagKey) {
    tagsSummary.topTags.firstOrNull { it.normalizedTag == uiState.selectedInsightTagKey }
      ?: tagsSummary.topTags.firstOrNull()
  }
  var detailSheetTagKey by remember { mutableStateOf<String?>(null) }
  val detailSheetStat = remember(tagsSummary.topTags, detailSheetTagKey) {
    detailSheetTagKey?.let { key -> tagsSummary.topTags.firstOrNull { it.normalizedTag == key } }
  }

  val topTagMoodColor = tagsSummary.topTags.firstOrNull()?.averageMood?.let { moodColorInterpolated(it) }

  InsightsCategoryScaffold(
    title = "Tags & Themes",
    glowAccent = topTagMoodColor ?: colors.accentPrimary,
    selectedRange = uiState.insightRange,
    onBack = onBack,
    onSelectRange = onSelectRange,
    modifier = modifier,
  ) {
    if (!tagsSummary.hasAnyTags) {
      item {
        CategoryEmptyCard(
          icon = Icons.Outlined.Tag,
          title = "No tags recorded for this period.",
          subtitle = "Add hashtags in Entry Details to explore recurring themes in your journal.",
          testTag = "tags_empty_state",
        )
      }
    } else {
      item {
        val topTag = tagsSummary.topTags.firstOrNull()
        val taggedSharePct = if (tagsSummary.eligibleEntryCount > 0) {
          ((tagsSummary.taggedEntryCount.toFloat() / tagsSummary.eligibleEntryCount.toFloat()) * 100f).roundToInt()
        } else {
          0
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          InsightMetricTile(
            label = "Unique Tags",
            value = tagsSummary.distinctTagCount.toString(),
            subtitle = uiState.insightRange.label,
            valueColor = colors.textPrimary,
            indicatorColor = colors.accentPrimary,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("tags_stat_unique"),
          )
          InsightMetricTile(
            label = "Tagged Logs",
            value = "${tagsSummary.taggedEntryCount}/${tagsSummary.eligibleEntryCount}",
            subtitle = "$taggedSharePct% of entries",
            valueColor = colors.textPrimary,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("tags_stat_tagged_entries"),
          )
          InsightMetricTile(
            label = "Top Tag",
            value = topTag?.let { "#${it.displayTag}" } ?: "—",
            subtitle = topTag?.let {
              "${it.entryCount} ${if (it.entryCount == 1) "entry" else "entries"}"
            },
            valueColor = when {
              topTagMoodColor == null -> colors.textPrimary
              colors.isDark -> topTagMoodColor
              else -> colors.textPrimary
            },
            indicatorColor = topTagMoodColor,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("tags_stat_top_tag"),
          )
        }
      }

      // A. Hashtag Word Cloud
      item {
        GlassCard(
          cornerRadius = 24.dp,
          modifier = Modifier.testTag("tags_word_cloud_card"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Tag Cloud",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
              )
              Text(
                text = "Tap a tag",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HashtagWordCloud(
              tags = tagsSummary.wordCloudTags,
              selectedTagKey = selectedTagStat?.normalizedTag,
              onTagClick = { stat ->
                onSelectTag(stat.normalizedTag)
                detailSheetTagKey = stat.normalizedTag
              },
            )
            ChartObservationBanner(observation = tagsSummary.observation)
          }
        }
      }

      // B. Most Frequently Used Tags
      item {
        GlassCard(
          cornerRadius = 24.dp,
          modifier = Modifier.testTag("tags_ranked_list_card"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
          ) {
            Text(
              text = "Most Frequently Used Tags",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Share of ${tagsSummary.eligibleEntryCount} mood ${if (tagsSummary.eligibleEntryCount == 1) "entry" else "entries"} in ${uiState.insightRange.label.lowercase()}",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textMuted,
            )
            Spacer(modifier = Modifier.height(12.dp))
            RankedTagsList(
              tags = tagsSummary.topTags,
              selectedTagKey = selectedTagStat?.normalizedTag,
              onTagClick = { stat ->
                onSelectTag(stat.normalizedTag)
                detailSheetTagKey = stat.normalizedTag
              },
            )
          }
        }
      }

      // C. Tag Usage Trends
      item {
        GlassCard(
          cornerRadius = 24.dp,
          modifier = Modifier.testTag("tags_trends_card"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
          ) {
            Text(
              text = "Tag Usage Trends",
              style = MaterialTheme.typography.titleLarge,
              color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Compared with preceding equivalent period",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textMuted,
            )
            Spacer(modifier = Modifier.height(12.dp))
            TagTrendsList(
              trendItems = tagsSummary.trendItems,
              supportsPeriodComparison = tagsSummary.supportsPeriodComparison,
              hasInsufficientTrendData = tagsSummary.hasInsufficientTrendData,
              canOpenTag = { key -> tagsSummary.topTags.any { it.normalizedTag == key } },
              onSelectTrendTag = { key ->
                onSelectTag(key)
                detailSheetTagKey = key
              },
            )
          }
        }
      }

      // D. Mood Distribution by Tag
      if (selectedTagStat != null) {
        item {
          GlassCard(
            cornerRadius = 24.dp,
            modifier = Modifier.testTag("tags_mood_distribution_card"),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            ) {
              Text(
                text = "Mood Distribution by Tag",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
              )
              Spacer(modifier = Modifier.height(10.dp))
              TagDistributionSelectorRow(
                tags = tagsSummary.topTags,
                selectedTagKey = selectedTagStat.normalizedTag,
                onSelectTag = { stat -> onSelectTag(stat.normalizedTag) },
              )
              Spacer(modifier = Modifier.height(14.dp))
              MoodDistributionByTagContent(stat = selectedTagStat)
            }
          }
        }
      }
    }
  }

  detailSheetStat?.let { stat ->
    TagDetailBottomSheet(
      stat = stat,
      supportsPeriodComparison = tagsSummary.supportsPeriodComparison,
      onDismiss = { detailSheetTagKey = null },
    )
  }
}
