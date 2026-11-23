package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.MoodyTheme
import com.example.util.DateTimeUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodyDatePickerDialog(
  initialTimestampMillis: Long,
  maxDateMillis: Long? = System.currentTimeMillis(),
  onConfirmDateUtcMillis: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val colors = MoodyTheme.colors
  val initialUtc = DateTimeUtils.toUtcDatePickerMillis(initialTimestampMillis)
  val maxUtc = maxDateMillis?.let { DateTimeUtils.toUtcDatePickerMillis(it) }
  val state = rememberDatePickerState(
    initialSelectedDateMillis = initialUtc,
    selectableDates = object : SelectableDates {
      override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return maxUtc == null || utcTimeMillis <= maxUtc
      }
    },
  )

  DatePickerDialog(
    onDismissRequest = onDismiss,
    colors = DatePickerDefaults.colors(
      containerColor = colors.cardSurfaceElevated,
    ),
    confirmButton = {
      TextButton(
        onClick = {
          state.selectedDateMillis?.let(onConfirmDateUtcMillis)
        },
      ) {
        Text("Next", color = colors.accentPrimary)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = colors.textSecondary)
      }
    },
  ) {
    DatePicker(
      state = state,
      colors = DatePickerDefaults.colors(
        containerColor = colors.cardSurfaceElevated,
        titleContentColor = colors.textSecondary,
        headlineContentColor = colors.textPrimary,
        weekdayContentColor = colors.textSecondary,
        subheadContentColor = colors.textPrimary,
        yearContentColor = colors.textPrimary,
        currentYearContentColor = colors.accentPrimary,
        selectedYearContentColor = colors.onAccentPrimary,
        selectedYearContainerColor = colors.accentPrimary,
        dayContentColor = colors.textPrimary,
        selectedDayContentColor = colors.onAccentPrimary,
        selectedDayContainerColor = colors.accentPrimary,
        todayContentColor = colors.accentPrimary,
        todayDateBorderColor = colors.accentPrimary,
      ),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodyTimePickerDialog(
  initialTimestampMillis: Long,
  onConfirmTime: (hour: Int, minute: Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val colors = MoodyTheme.colors
  val cal = Calendar.getInstance().apply { timeInMillis = initialTimestampMillis }
  val state = rememberTimePickerState(
    initialHour = cal.get(Calendar.HOUR_OF_DAY),
    initialMinute = cal.get(Calendar.MINUTE),
    is24Hour = false,
  )

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = colors.cardSurfaceElevated,
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "Select time",
          style = MaterialTheme.typography.titleMedium,
          color = colors.textSecondary,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        TimePicker(
          state = state,
          colors = TimePickerDefaults.colors(
            clockDialColor = colors.background.copy(alpha = 0.7f),
            clockDialSelectedContentColor = colors.onAccentPrimary,
            clockDialUnselectedContentColor = colors.textPrimary,
            selectorColor = colors.accentPrimary,
            periodSelectorSelectedContainerColor = colors.accentPrimary.copy(alpha = 0.25f),
            periodSelectorSelectedContentColor = colors.accentPrimary,
            periodSelectorUnselectedContentColor = colors.textSecondary,
            timeSelectorSelectedContainerColor = colors.accentPrimary.copy(alpha = 0.2f),
            timeSelectorUnselectedContainerColor = colors.background.copy(alpha = 0.5f),
            timeSelectorSelectedContentColor = colors.accentPrimary,
            timeSelectorUnselectedContentColor = colors.textPrimary,
          ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = colors.textSecondary)
          }
          TextButton(onClick = { onConfirmTime(state.hour, state.minute) }) {
            Text("Set time", color = colors.accentPrimary)
          }
        }
      }
    }
  }
}
