package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MoodEntry
import com.example.ui.theme.MoodyTheme
import com.example.util.DateTimeUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodEntryCard(
  entry: MoodEntry,
  showDateInTimestamp: Boolean = false,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  val moodColor = entry.moodLevel.color
  val contextSummary = buildContextSummary(entry)

  GlassCard(
    onClick = onClick,
    glowColor = moodColor,
    cornerRadius = 20.dp,
    borderColor = moodColor.copy(alpha = if (colors.isDark) 0.22f else 0.35f),
    modifier = modifier
      .fillMaxWidth()
      .testTag("entry_card_${entry.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.Top,
    ) {
      MoodFaceOrb(
        level = entry.moodLevel,
        size = 42.dp,
        selected = true,
      )
      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = entry.moodLevel.label,
            style = MaterialTheme.typography.titleMedium,
            color = if (colors.isDark) moodColor else colors.textPrimary,
          )

          Text(
            text = if (showDateInTimestamp) {
              DateTimeUtils.formatDateAndTime(entry.timestamp)
            } else {
              DateTimeUtils.formatTime(entry.timestamp)
            },
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
          )
        }

        if (entry.note.isNotBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = entry.note,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary.copy(alpha = 0.92f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        } else if (entry.tags.isEmpty() && contextSummary == null) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Tap to add a note or details",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
          )
        }

        if (entry.tags.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            entry.tags.forEach { tag ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(moodColor.copy(alpha = if (colors.isDark) 0.14f else 0.16f))
                  .padding(horizontal = 9.dp, vertical = 4.dp),
              ) {
                Text(
                  text = "#$tag",
                  style = MaterialTheme.typography.labelMedium,
                  color = if (colors.isDark) moodColor.copy(alpha = 0.94f) else colors.textPrimary,
                )
              }
            }
          }
        }

        if (contextSummary != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = contextSummary,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

private fun buildContextSummary(entry: MoodEntry): String? {
  val parts = buildList {
    entry.sleepDurationMinutes?.let { mins ->
      val h = mins / 60
      val m = mins % 60
      add(if (m == 0) "Sleep ${h}h" else "Sleep ${h}h ${m}m")
    }
    entry.sleepQuality?.let { add("Quality $it/5") }
    entry.energyLevel?.let { add("Energy $it/5") }
    entry.stressLevel?.let { add("Stress $it/5") }
    entry.anxietyLevel?.let { add("Anxiety $it/5") }
    entry.physicalActivity?.let { add("Activity: ${it.label}") }
  }
  return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
