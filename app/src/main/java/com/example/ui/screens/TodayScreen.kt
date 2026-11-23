package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.MoodEntry
import com.example.ui.components.GlassCard
import com.example.ui.components.MoodEntryCard
import com.example.ui.components.MoodFaceSelectorRow
import com.example.ui.components.MoodyDatePickerDialog
import com.example.ui.components.MoodyTimePickerDialog
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.MoodColor2
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.moodColorInterpolated
import com.example.util.DateTimeUtils
import com.example.viewmodel.MoodyUiState
import java.util.Calendar

@Composable
fun TodayScreen(
  uiState: MoodyUiState,
  onQuickLogMood: (Int) -> Unit,
  onSetCustomTimestamp: (Long?) -> Unit,
  onOpenEntry: (MoodEntry) -> Unit,
  modifier: Modifier = Modifier,
) {
  var pulseTrigger by remember { mutableIntStateOf(0) }
  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var pendingDateUtcMillis by remember { mutableLongStateOf(DateTimeUtils.toUtcDatePickerMillis(System.currentTimeMillis())) }

  val now = remember { System.currentTimeMillis() }
  val greeting = remember(now) {
    when (DateTimeUtils.hourOfDay(now)) {
      in 5..11 -> "Good morning"
      in 12..16 -> "Good afternoon"
      in 17..20 -> "Good evening"
      else -> "Late night"
    }
  }

  val todayAvg = remember(uiState.todayEntries) {
    uiState.todayEntries.takeIf { it.isNotEmpty() }?.map { it.mood }?.average()?.toFloat()
  }
  val ambientColor = todayAvg?.let { moodColorInterpolated(it) } ?: MoodColor2

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            ambientColor.copy(alpha = 0.14f),
            Color.Transparent,
          ),
          startY = 0f,
          endY = 650f,
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
      // Header
      item {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "MOODY",
              style = MaterialTheme.typography.labelSmall,
              color = ambientColor,
            )
            Text(
              text = DateTimeUtils.formatHeaderDate(now),
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = greeting,
            style = MaterialTheme.typography.displaySmall,
            color = TextPrimary,
          )
        }
      }

      // Quick Entry Hero Card
      item {
        GlassCard(
          glowColor = ambientColor,
          cornerRadius = 26.dp,
          borderColor = ambientColor.copy(alpha = 0.25f),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 18.dp, vertical = 20.dp),
          ) {
            Text(
              text = "How are you feeling?",
              style = MaterialTheme.typography.headlineSmall,
              color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "One tap logs instantly. Details can wait.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(18.dp))

            MoodFaceSelectorRow(
              selectedMood = null,
              onSelectMood = { score ->
                pulseTrigger++
                onQuickLogMood(score)
              },
              pulseTrigger = pulseTrigger,
              orbSize = 52.dp,
              showLabels = true,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Change time / back-date pill
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              val customTs = uiState.customQuickTimestamp
              Surface(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                color = if (customTs != null) AccentPrimary.copy(alpha = 0.14f) else GlassSurfaceElevated.copy(alpha = 0.7f),
                border = BorderStroke(
                  1.dp,
                  if (customTs != null) AccentPrimary.copy(alpha = 0.45f) else GlassBorder,
                ),
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("change_time_button"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = "Change entry time",
                    tint = if (customTs != null) AccentPrimary else TextSecondary,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = if (customTs != null) {
                      "Logging for ${DateTimeUtils.formatDateAndTime(customTs)}"
                    } else {
                      "Change time · Now"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (customTs != null) AccentPrimary else TextSecondary,
                  )
                }
              }

              if (customTs != null) {
                IconButton(
                  onClick = { onSetCustomTimestamp(null) },
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("reset_time_button"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Reset time to now",
                    tint = TextSecondary,
                  )
                }
              }
            }
          }
        }
      }

      // Today's Timeline Header
      item {
        Spacer(modifier = Modifier.height(4.dp))
        SectionHeader(
          title = "Today's Timeline",
          trailing = {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = GlassSurfaceElevated,
              border = BorderStroke(1.dp, GlassBorder),
            ) {
              Text(
                text = "${uiState.todayEntries.size} ${if (uiState.todayEntries.size == 1) "entry" else "entries"}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          },
        )
      }

      if (uiState.todayEntries.isEmpty()) {
        item {
          GlassCard(cornerRadius = 22.dp) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .background(ambientColor.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.NightsStay,
                  contentDescription = null,
                  tint = ambientColor,
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Quiet slate today",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Tap any mood face above to capture your current state.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      } else {
        itemsIndexed(
          items = uiState.todayEntries,
          key = { _, entry -> entry.id },
        ) { index, entry ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
          ) {
            // Timeline node & connector rail
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .width(24.dp)
                .padding(top = 18.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .background(entry.moodLevel.color, CircleShape),
              )
              if (index < uiState.todayEntries.lastIndex) {
                Box(
                  modifier = Modifier
                    .width(1.5.dp)
                    .height(64.dp)
                    .padding(top = 6.dp)
                    .background(GlassBorder),
                )
              }
            }
            Spacer(modifier = Modifier.width(6.dp))
            MoodEntryCard(
              entry = entry,
              showDateInTimestamp = false,
              onClick = { onOpenEntry(entry) },
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }

  if (showDatePicker) {
    val baseTs = uiState.customQuickTimestamp ?: System.currentTimeMillis()
    MoodyDatePickerDialog(
      initialTimestampMillis = baseTs,
      onConfirmDateUtcMillis = { utcMillis ->
        pendingDateUtcMillis = utcMillis
        showDatePicker = false
        showTimePicker = true
      },
      onDismiss = { showDatePicker = false },
    )
  }

  if (showTimePicker) {
    val baseTs = uiState.customQuickTimestamp ?: System.currentTimeMillis()
    MoodyTimePickerDialog(
      initialTimestampMillis = baseTs,
      onConfirmTime = { hour, minute ->
        val chosen = DateTimeUtils.combineDateAndTime(pendingDateUtcMillis, hour, minute)
        val clamped = minOf(chosen, System.currentTimeMillis())
        onSetCustomTimestamp(clamped)
        showTimePicker = false
      },
      onDismiss = { showTimePicker = false },
    )
  }
}
