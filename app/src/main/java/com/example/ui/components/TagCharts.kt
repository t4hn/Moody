package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MoodLevel
import com.example.ui.theme.MoodColor1
import com.example.ui.theme.MoodColor5
import com.example.ui.theme.MoodyTheme
import com.example.ui.theme.moodColorInterpolated
import com.example.viewmodel.MoodAnalytics
import com.example.viewmodel.TagFrequencyStat
import com.example.viewmodel.TagTrendDirection
import com.example.viewmodel.TagTrendItem
import kotlin.math.roundToInt

private const val INITIAL_RANKED_LIMIT = 5

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HashtagWordCloud(
  tags: List<TagFrequencyStat>,
  selectedTagKey: String?,
  onTagClick: (TagFrequencyStat) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  if (tags.isEmpty()) return

  val minCount = remember(tags) { tags.minOf { it.entryCount } }
  val maxCount = remember(tags) { tags.maxOf { it.entryCount } }

  FlowRow(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
  ) {
    tags.forEach { stat ->
      val weight = if (maxCount == minCount) {
        0.5f
      } else {
        ((stat.entryCount - minCount).toFloat() / (maxCount - minCount).toFloat()).coerceIn(0f, 1f)
      }
      val fontSizeSp = (13f + weight * 8f).sp
      val selected = stat.normalizedTag == selectedTagKey
      val moodDotColor = stat.averageMood?.let { moodColorInterpolated(it) } ?: colors.accentPrimary

      Surface(
        onClick = { onTagClick(stat) },
        shape = RoundedCornerShape(14.dp),
        color = when {
          selected -> colors.accentPrimary.copy(alpha = if (colors.isDark) 0.22f else 0.16f)
          weight >= 0.65f -> colors.cardSurfaceElevated
          else -> colors.chartTrack.copy(alpha = if (colors.isDark) 0.85f else 0.7f)
        },
        border = BorderStroke(
          width = if (selected) 1.5.dp else 1.dp,
          color = if (selected) colors.accentPrimary else colors.border,
        ),
        modifier = Modifier
          .semantics {
            contentDescription = "Hashtag #${stat.displayTag}, ${stat.entryCount} entries. Tap for details."
          }
          .testTag("word_cloud_tag_${stat.normalizedTag}"),
      ) {
        Row(
          modifier = Modifier.padding(
            horizontal = (10f + weight * 4f).dp,
            vertical = (6f + weight * 3f).dp,
          ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier = Modifier
              .size((6f + weight * 2.5f).dp)
              .background(moodDotColor, CircleShape),
          )
          Text(
            text = "#${stat.displayTag}",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontSize = fontSizeSp,
              lineHeight = (18f + weight * 8f).sp,
              fontWeight = if (selected || weight >= 0.6f) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = when {
              selected -> if (colors.isDark) colors.textPrimary else colors.accentPrimary
              weight >= 0.6f -> colors.textPrimary
              else -> colors.textSecondary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun RankedTagsList(
  tags: List<TagFrequencyStat>,
  selectedTagKey: String?,
  onTagClick: (TagFrequencyStat) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  if (tags.isEmpty()) return

  var expanded by remember(tags.size) { mutableStateOf(false) }
  val visibleTags = if (expanded || tags.size <= INITIAL_RANKED_LIMIT) {
    tags
  } else {
    tags.take(INITIAL_RANKED_LIMIT)
  }
  val maxEntryCount = remember(tags) { tags.maxOf { it.entryCount }.coerceAtLeast(1) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .animateContentSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    visibleTags.forEachIndexed { index, stat ->
      val selected = stat.normalizedTag == selectedTagKey
      val barColor = stat.averageMood?.let { moodColorInterpolated(it) } ?: colors.accentPrimary
      val fraction = (stat.entryCount.toFloat() / maxEntryCount.toFloat()).coerceIn(0.06f, 1f)
      val pctRounded = stat.percentageOfEntries.roundToInt()
      val countLabel = "${stat.entryCount} ${if (stat.entryCount == 1) "entry" else "entries"}"

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(
            if (selected) {
              colors.accentPrimary.copy(alpha = if (colors.isDark) 0.14f else 0.10f)
            } else {
              colors.chartTrack.copy(alpha = 0.45f)
            },
          )
          .border(
            width = if (selected) 1.dp else 0.5.dp,
            color = if (selected) colors.accentPrimary else colors.border,
            shape = RoundedCornerShape(14.dp),
          )
          .clickable { onTagClick(stat) }
          .minimumInteractiveComponentSize()
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .semantics(mergeDescendants = true) {
            contentDescription = "Rank ${index + 1}, #${stat.displayTag}, $countLabel, $pctRounded percent of entries"
          }
          .testTag("ranked_tag_${stat.normalizedTag}"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "${index + 1}.",
              style = MaterialTheme.typography.labelSmall,
              color = colors.textMuted,
            )
            Text(
              text = "#${stat.displayTag}",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = colors.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "$countLabel · $pctRounded%",
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
          )
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.chartTrack),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(fraction)
              .height(7.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(
                Brush.horizontalGradient(
                  colors = listOf(barColor.copy(alpha = 0.70f), barColor),
                ),
              ),
          )
        }
      }
    }

    if (tags.size > INITIAL_RANKED_LIMIT) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
      ) {
        TextButton(
          onClick = { expanded = !expanded },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("toggle_all_tags_button"),
        ) {
          Text(
            text = if (expanded) "Show top $INITIAL_RANKED_LIMIT" else "Show all ${tags.size} tags",
            style = MaterialTheme.typography.labelLarge,
            color = colors.accentPrimary,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.accentPrimary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
fun TagTrendsList(
  trendItems: List<TagTrendItem>,
  supportsPeriodComparison: Boolean,
  hasInsufficientTrendData: Boolean,
  onSelectTrendTag: (String) -> Unit,
  canOpenTag: (String) -> Boolean,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors

  if (!supportsPeriodComparison) {
    Text(
      text = "Select Week, Month, or 3 Months to compare tag usage against the preceding equivalent period.",
      style = MaterialTheme.typography.bodySmall,
      color = colors.textMuted,
      modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .testTag("trends_unavailable_all_range"),
    )
    return
  }

  if (trendItems.isEmpty() || hasInsufficientTrendData) {
    Text(
      text = "Not enough multi-entry tag data across consecutive periods to show usage trends yet.",
      style = MaterialTheme.typography.bodySmall,
      color = colors.textMuted,
      modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .testTag("trends_insufficient_data"),
    )
    return
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    trendItems.forEach { item ->
      val clickable = canOpenTag(item.normalizedTag)
      val deltaText = formatTrendDelta(item)
      val (badgeColor, icon) = when (item.direction) {
        TagTrendDirection.UP -> MoodColor5 to Icons.AutoMirrored.Outlined.TrendingUp
        TagTrendDirection.DOWN -> MoodColor1 to Icons.AutoMirrored.Outlined.TrendingDown
        TagTrendDirection.UNCHANGED -> colors.textMuted to Icons.Outlined.Remove
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(colors.chartTrack.copy(alpha = 0.45f))
          .border(0.5.dp, colors.border, RoundedCornerShape(14.dp))
          .then(
            if (clickable) {
              Modifier.clickable { onSelectTrendTag(item.normalizedTag) }
            } else {
              Modifier
            },
          )
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("trend_tag_${item.normalizedTag}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "#${item.displayTag}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${item.currentCount} now · ${item.previousCount} prior period",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = badgeColor.copy(alpha = if (colors.isDark) 0.16f else 0.14f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = badgeColor,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = deltaText,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = if (colors.isDark) badgeColor else colors.textPrimary,
            )
          }
        }
      }
    }
  }
}

internal fun formatTrendDelta(item: TagTrendItem): String = when (item.direction) {
  TagTrendDirection.UP -> {
    if (item.percentChange != null) {
      "+${item.deltaCount} (+${item.percentChange.roundToInt()}%)"
    } else {
      "+${item.deltaCount} · New"
    }
  }
  TagTrendDirection.DOWN -> {
    if (item.percentChange != null) {
      "${item.deltaCount} (${item.percentChange.roundToInt()}%)"
    } else {
      "${item.deltaCount}"
    }
  }
  TagTrendDirection.UNCHANGED -> "0 (0%)"
}

@Composable
fun TagDistributionSelectorRow(
  tags: List<TagFrequencyStat>,
  selectedTagKey: String?,
  onSelectTag: (TagFrequencyStat) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    tags.take(15).forEach { stat ->
      val selected = stat.normalizedTag == selectedTagKey
      Surface(
        onClick = { onSelectTag(stat) },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.accentPrimary.copy(alpha = 0.18f) else colors.chartTrack,
        border = BorderStroke(
          width = if (selected) 1.5.dp else 1.dp,
          color = if (selected) colors.accentPrimary else colors.border,
        ),
        modifier = Modifier.testTag("distribution_chip_${stat.normalizedTag}"),
      ) {
        Text(
          text = "#${stat.displayTag} (${stat.entryCount})",
          style = MaterialTheme.typography.labelMedium,
          color = if (selected) {
            if (colors.isDark) colors.textPrimary else colors.accentPrimary
          } else {
            colors.textSecondary
          },
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
      }
    }
  }
}

@Composable
fun MoodDistributionByTagContent(
  stat: TagFrequencyStat,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val avgColor = stat.averageMood?.let { moodColorInterpolated(it) }
  val matchLabel = "${stat.entryCount} matching ${if (stat.entryCount == 1) "entry" else "entries"}"

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "#${stat.displayTag}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = colors.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "$matchLabel · ${stat.percentageOfEntries.roundToInt()}% of period entries",
          style = MaterialTheme.typography.labelSmall,
          color = colors.textSecondary,
        )
      }
      if (stat.averageMood != null && avgColor != null) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = avgColor.copy(alpha = if (colors.isDark) 0.18f else 0.15f),
          border = BorderStroke(1.dp, avgColor.copy(alpha = 0.45f)),
        ) {
          Text(
            text = "Avg ${MoodAnalytics.formatScore(stat.averageMood)} / 5",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (colors.isDark) avgColor else colors.textPrimary,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
          )
        }
      }
    }

    Text(
      text = "Mood score distribution across entries containing #${stat.displayTag}.",
      style = MaterialTheme.typography.bodySmall,
      color = colors.textMuted,
    )

    // Render scores 5 down to 1 using fixed traffic-light semantic mood colors
    stat.moodDistribution
      .sortedByDescending { it.moodScore }
      .forEach { bucket ->
        val level = MoodLevel.fromScore(bucket.moodScore)
        val barColor = level.color
        val fraction = if (stat.entryCount > 0) {
          (bucket.count.toFloat() / stat.entryCount.toFloat()).coerceIn(0f, 1f)
        } else {
          0f
        }
        val pctRounded = bucket.percentage.roundToInt()

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
              contentDescription = "Mood ${bucket.moodScore}: ${bucket.count} entries, $pctRounded percent"
            }
            .testTag("mood_dist_row_${bucket.moodScore}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(CircleShape)
              .background(barColor.copy(alpha = if (colors.isDark) 0.22f else 0.20f))
              .border(1.dp, barColor, CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = bucket.moodScore.toString(),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (colors.isDark) barColor else colors.textPrimary,
            )
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .height(10.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(colors.chartTrack)
              .border(0.5.dp, colors.border, RoundedCornerShape(5.dp)),
          ) {
            if (bucket.count > 0 && fraction > 0f) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(fraction.coerceAtLeast(0.05f))
                  .height(10.dp)
                  .clip(RoundedCornerShape(5.dp))
                  .background(barColor),
              )
            }
          }

          Text(
            text = "${bucket.count} ($pctRounded%)",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = if (bucket.count > 0) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (bucket.count > 0) colors.textPrimary else colors.textMuted,
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp),
          )
        }
      }

    if (!stat.hasSufficientMoodData) {
      HorizontalDivider(color = colors.border)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("tag_mood_insufficient_data"),
        verticalAlignment = Alignment.Top,
      ) {
        Icon(
          imageVector = Icons.Outlined.Info,
          contentDescription = null,
          tint = colors.textMuted,
          modifier = Modifier
            .padding(top = 2.dp)
            .size(14.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Only $matchLabel recorded for #${stat.displayTag} in this period. More entries are needed for a representative mood distribution.",
          style = MaterialTheme.typography.bodySmall,
          color = colors.textSecondary,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailBottomSheet(
  stat: TagFrequencyStat,
  supportsPeriodComparison: Boolean,
  onDismiss: () -> Unit,
) {
  val colors = MoodyTheme.colors
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.surfaceBase,
    contentColor = colors.textPrimary,
    dragHandle = { BottomSheetDefaults.DragHandle(color = colors.borderBright) },
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(horizontal = 20.dp)
        .padding(bottom = 24.dp)
        .testTag("tag_detail_sheet"),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "TAG INSIGHT",
            style = MaterialTheme.typography.labelSmall,
            color = colors.accentPrimary,
          )
          Text(
            text = "#${stat.displayTag}",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("tag_detail_close_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Close tag details",
            tint = colors.textSecondary,
          )
        }
      }

      if (supportsPeriodComparison && stat.previousCount != null && stat.deltaCount != null && stat.trendDirection != null) {
        val trendItem = TagTrendItem(
          normalizedTag = stat.normalizedTag,
          displayTag = stat.displayTag,
          currentCount = stat.entryCount,
          previousCount = stat.previousCount,
          deltaCount = stat.deltaCount,
          percentChange = stat.percentChange,
          direction = stat.trendDirection,
        )
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = colors.cardSurfaceElevated,
          border = BorderStroke(1.dp, colors.border),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Vs preceding period (${stat.previousCount} prior)",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary,
            )
            Text(
              text = formatTrendDelta(trendItem),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = colors.textPrimary,
            )
          }
        }
      }

      MoodDistributionByTagContent(stat = stat)
    }
  }
}
