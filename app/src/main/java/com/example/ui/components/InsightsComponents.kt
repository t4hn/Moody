package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MoodyTheme
import com.example.viewmodel.InsightTimeRange

@Composable
fun TimeframeSelectorBar(
  selectedRange: InsightTimeRange,
  onSelectRange: (InsightTimeRange) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(colors.chartTrack)
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    InsightTimeRange.categoryRanges.forEach { range ->
      val selected = selectedRange == range
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
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun ChartObservationBanner(
  observation: String?,
  modifier: Modifier = Modifier,
) {
  if (observation.isNullOrBlank()) return
  val colors = MoodyTheme.colors
  Column(modifier = modifier.fillMaxWidth()) {
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = colors.border)
    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
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
        text = observation,
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
      )
    }
  }
}

@Composable
fun InsightsCategoryHeader(
  title: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  eyebrow: String = "INSIGHTS",
  accentColor: Color = MoodyTheme.colors.accentPrimary,
) {
  val colors = MoodyTheme.colors
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(
      onClick = onBack,
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .testTag("insights_back_button"),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
        contentDescription = "Back to Insights",
        tint = colors.textPrimary,
      )
    }
    Spacer(modifier = Modifier.width(4.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = eyebrow,
        style = MaterialTheme.typography.labelSmall,
        color = accentColor,
      )
      Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun InsightMetricTile(
  label: String,
  value: String,
  subtitle: String? = null,
  valueColor: Color,
  indicatorColor: Color? = null,
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
        if (showStreakIcon) {
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = valueColor,
            modifier = Modifier.size(14.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        color = valueColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (!subtitle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = colors.textMuted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
