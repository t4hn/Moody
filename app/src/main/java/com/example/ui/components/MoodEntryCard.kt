package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MoodEntry
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DateTimeUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodEntryCard(
  entry: MoodEntry,
  showDateInTimestamp: Boolean = false,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val moodColor = entry.moodLevel.color
  GlassCard(
    onClick = onClick,
    glowColor = moodColor,
    cornerRadius = 20.dp,
    borderColor = moodColor.copy(alpha = 0.22f),
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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = entry.moodLevel.label,
              style = MaterialTheme.typography.titleMedium,
              color = moodColor,
            )
            if (entry.isDemo) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = GlassSurfaceElevated,
                border = BorderStroke(1.dp, GlassBorder),
              ) {
                Text(
                  text = "DEMO",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextMuted,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                )
              }
            }
          }

          Text(
            text = if (showDateInTimestamp) {
              DateTimeUtils.formatDateAndTime(entry.timestamp)
            } else {
              DateTimeUtils.formatTime(entry.timestamp)
            },
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }

        if (entry.note.isNotBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = entry.note,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary.copy(alpha = 0.92f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        } else if (entry.tags.isEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Tap to add a note or tags",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
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
                  .background(moodColor.copy(alpha = 0.12f))
                  .padding(horizontal = 9.dp, vertical = 4.dp),
              ) {
                Text(
                  text = "#$tag",
                  style = MaterialTheme.typography.labelMedium,
                  color = moodColor.copy(alpha = 0.92f),
                )
              }
            }
          }
        }
      }
    }
  }
}
