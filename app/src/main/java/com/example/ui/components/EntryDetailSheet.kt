package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.model.MoodEntry
import com.example.model.MoodLevel
import com.example.ui.theme.CharcoalBase
import com.example.ui.theme.DangerRose
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DateTimeUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryDetailSheet(
  entry: MoodEntry,
  suggestedTags: List<String>,
  onSave: (MoodEntry) -> Unit,
  onDelete: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var mood by remember(entry.id) { mutableIntStateOf(entry.mood) }
  var timestamp by remember(entry.id) { mutableLongStateOf(entry.timestamp) }
  var note by remember(entry.id) { mutableStateOf(entry.note) }
  var tags by remember(entry.id) { mutableStateOf(entry.tags) }
  var newTagText by remember { mutableStateOf("") }

  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var pendingDateUtcMillis by remember { mutableLongStateOf(DateTimeUtils.toUtcDatePickerMillis(timestamp)) }
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  val moodLevel = MoodLevel.fromScore(mood)
  val moodColor = moodLevel.color

  fun addTag(raw: String) {
    val clean = raw.trim().lowercase().removePrefix("#")
    if (clean.isNotEmpty() && clean !in tags) {
      tags = tags + clean
    }
    newTagText = ""
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = CharcoalBase,
    contentColor = TextPrimary,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 24.dp)
        .navigationBarsPadding(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(
            text = "Entry Details",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
          )
          Text(
            text = "Feeling ${moodLevel.label}",
            style = MaterialTheme.typography.bodySmall,
            color = moodColor,
          )
        }

        IconButton(
          onClick = { showDeleteConfirmation = true },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("delete_entry_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = "Delete entry",
            tint = DangerRose,
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Mood selector
      MoodFaceSelectorRow(
        selectedMood = mood,
        onSelectMood = { mood = it },
        orbSize = 48.dp,
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Date & Time editors
      Text(
        text = "Timestamp",
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          onClick = { showDatePicker = true },
          shape = RoundedCornerShape(14.dp),
          color = GlassSurface,
          border = BorderStroke(1.dp, GlassBorder),
          modifier = Modifier
            .weight(1f)
            .minimumInteractiveComponentSize()
            .testTag("edit_entry_date"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.CalendarToday,
              contentDescription = "Change date",
              tint = moodColor,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = DateTimeUtils.formatFullDate(timestamp),
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
            )
          }
        }

        Surface(
          onClick = { showTimePicker = true },
          shape = RoundedCornerShape(14.dp),
          color = GlassSurface,
          border = BorderStroke(1.dp, GlassBorder),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("edit_entry_time"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Schedule,
              contentDescription = "Change time",
              tint = moodColor,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = DateTimeUtils.formatTime(timestamp),
              style = MaterialTheme.typography.labelSmall,
              color = TextPrimary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Note editor
      Text(
        text = "Note",
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
      )
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        placeholder = {
          Text(
            text = "What's shaping your mood right now?",
            color = TextMuted,
          )
        },
        minLines = 3,
        maxLines = 6,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = moodColor.copy(alpha = 0.7f),
          unfocusedBorderColor = GlassBorderBright,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          cursorColor = moodColor,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("entry_note_input"),
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Tags editor
      Text(
        text = "Tags & Emotions",
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
      )
      Spacer(modifier = Modifier.height(8.dp))

      if (tags.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          tags.forEach { tag ->
            Surface(
              onClick = { tags = tags - tag },
              shape = RoundedCornerShape(12.dp),
              color = moodColor.copy(alpha = 0.18f),
              border = BorderStroke(1.dp, moodColor.copy(alpha = 0.45f)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "#$tag",
                  style = MaterialTheme.typography.labelMedium,
                  color = TextPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Outlined.Close,
                  contentDescription = "Remove tag $tag",
                  tint = moodColor,
                  modifier = Modifier.size(14.dp),
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = newTagText,
          onValueChange = { newTagText = it },
          placeholder = { Text("Add tag (e.g. calm, social)", color = TextMuted) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { addTag(newTagText) }),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = moodColor.copy(alpha = 0.7f),
            unfocusedBorderColor = GlassBorderBright,
            focusedContainerColor = GlassSurface,
            unfocusedContainerColor = GlassSurface,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = moodColor,
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("entry_tag_input"),
        )
        Surface(
          onClick = { addTag(newTagText) },
          shape = RoundedCornerShape(14.dp),
          color = GlassSurfaceElevated,
          border = BorderStroke(1.dp, GlassBorderBright),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("add_tag_button"),
        ) {
          Box(
            modifier = Modifier.padding(14.dp),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.Add,
              contentDescription = "Add tag",
              tint = moodColor,
            )
          }
        }
      }

      val availableSuggestions = suggestedTags.filter { it !in tags }.take(12)
      if (availableSuggestions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Suggestions",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          availableSuggestions.forEach { suggestion ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(GlassSurface)
                .clickable { addTag(suggestion) }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
              Text(
                text = "+ $suggestion",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          onSave(
            entry.copy(
              mood = mood,
              timestamp = timestamp,
              note = note,
              tags = tags,
            ),
          )
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = moodColor,
          contentColor = ObsidianBg,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_entry_button"),
      ) {
        Text(
          text = "Save entry",
          style = MaterialTheme.typography.titleMedium,
        )
      }
    }
  }

  if (showDatePicker) {
    MoodyDatePickerDialog(
      initialTimestampMillis = timestamp,
      onConfirmDateUtcMillis = { utcMillis ->
        pendingDateUtcMillis = utcMillis
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        timestamp = DateTimeUtils.combineDateAndTime(
          dateUtcMillis = utcMillis,
          hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
          minute = cal.get(Calendar.MINUTE),
        )
        showDatePicker = false
        showTimePicker = true
      },
      onDismiss = { showDatePicker = false },
    )
  }

  if (showTimePicker) {
    MoodyTimePickerDialog(
      initialTimestampMillis = timestamp,
      onConfirmTime = { hour, minute ->
        val utcDate = DateTimeUtils.toUtcDatePickerMillis(timestamp)
        timestamp = DateTimeUtils.combineDateAndTime(utcDate, hour, minute)
        showTimePicker = false
      },
      onDismiss = { showTimePicker = false },
    )
  }

  if (showDeleteConfirmation) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmation = false },
      containerColor = GlassSurfaceElevated,
      title = {
        Text("Delete this entry?", color = TextPrimary)
      },
      text = {
        Text(
          text = "This mood log from ${DateTimeUtils.formatDateAndTime(timestamp)} will be permanently removed.",
          color = TextSecondary,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteConfirmation = false
            onDelete(entry.id)
          },
          modifier = Modifier.testTag("confirm_delete_entry"),
        ) {
          Text("Delete", color = DangerRose)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmation = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
    )
  }
}
