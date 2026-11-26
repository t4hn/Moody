package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DailyContext
import com.example.model.MoodEntry
import com.example.model.MoodLevel
import com.example.model.PhysicalActivityLevel
import com.example.ui.theme.MoodyTheme
import com.example.util.DateTimeUtils
import java.util.Calendar
import kotlin.math.hypot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class EntrySheetBackAction {
  HIDE_KEYBOARD_ONLY,
  DISMISS_SHEET,
}

internal fun resolveEntrySheetBackAction(isImeVisible: Boolean): EntrySheetBackAction =
  if (isImeVisible) {
    EntrySheetBackAction.HIDE_KEYBOARD_ONLY
  } else {
    EntrySheetBackAction.DISMISS_SHEET
  }

@OptIn(ExperimentalMaterial3Api::class)
internal fun shouldConfirmEntrySheetValueChange(
  targetValue: SheetValue,
  isImeVisible: Boolean,
  isImeClosing: Boolean,
  isUserDraggingSheet: Boolean,
  isExplicitDismissRequested: Boolean,
): Boolean = when (targetValue) {
  SheetValue.PartiallyExpanded -> false
  SheetValue.Expanded -> true
  SheetValue.Hidden ->
    isExplicitDismissRequested ||
      isUserDraggingSheet ||
      (!isImeVisible && !isImeClosing)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryDetailSheet(
  entry: MoodEntry,
  suggestedTags: List<String>,
  dailyContexts: Map<String, DailyContext> = emptyMap(),
  onSave: (MoodEntry, updateDailySleep: Boolean) -> Unit,
  onDelete: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val colors = MoodyTheme.colors
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val scrollState = rememberScrollState()

  val isImeVisible = WindowInsets.isImeVisible
  val latestIsImeVisible = rememberUpdatedState(isImeVisible)
  var wasImeVisible by remember { mutableStateOf(isImeVisible) }
  val isImeClosingState = remember { mutableStateOf(false) }
  val isUserDraggingSheetState = remember(entry.id) { mutableStateOf(false) }
  val isExplicitDismissRequestedState = remember(entry.id) { mutableStateOf(false) }
  var imeClosingResetJob by remember { mutableStateOf<Job?>(null) }
  var dragResetJob by remember { mutableStateOf<Job?>(null) }

  var isNoteFocused by remember { mutableStateOf(false) }
  var isTagFocused by remember { mutableStateOf(false) }

  fun triggerImeClosingGuard() {
    imeClosingResetJob?.cancel()
    isImeClosingState.value = true
    imeClosingResetJob = scope.launch {
      delay(400L)
      isImeClosingState.value = false
    }
  }

  fun markUserDraggingSheet(dragging: Boolean) {
    dragResetJob?.cancel()
    if (dragging) {
      isUserDraggingSheetState.value = true
    } else {
      dragResetJob = scope.launch {
        delay(400L)
        isUserDraggingSheetState.value = false
      }
    }
  }

  LaunchedEffect(isImeVisible) {
    if (wasImeVisible && !isImeVisible) {
      triggerImeClosingGuard()
      focusManager.clearFocus()
    } else if (isImeVisible) {
      imeClosingResetJob?.cancel()
      isImeClosingState.value = false
    }
    wasImeVisible = isImeVisible
  }

  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true,
    confirmValueChange = { targetValue ->
      shouldConfirmEntrySheetValueChange(
        targetValue = targetValue,
        isImeVisible = latestIsImeVisible.value,
        isImeClosing = isImeClosingState.value,
        isUserDraggingSheet = isUserDraggingSheetState.value,
        isExplicitDismissRequested = isExplicitDismissRequestedState.value,
      )
    },
  )
  var mood by remember(entry.id) { mutableIntStateOf(entry.mood) }
  var timestamp by remember(entry.id) { mutableLongStateOf(entry.timestamp) }
  var note by remember(entry.id) { mutableStateOf(entry.note) }
  var tags by remember(entry.id) { mutableStateOf(entry.tags) }
  var newTagText by remember { mutableStateOf("") }

  // Collapsible Additional Context section (collapsed by default)
  var isContextExpanded by remember(entry.id) { mutableStateOf(false) }
  var sleepDurationMinutes by remember(entry.id) { mutableStateOf(entry.sleepDurationMinutes) }
  var sleepQuality by remember(entry.id) { mutableStateOf(entry.sleepQuality) }
  var sleepEdited by remember(entry.id) { mutableStateOf(false) }

  var energyLevel by remember(entry.id) { mutableStateOf(entry.energyLevel) }
  var stressLevel by remember(entry.id) { mutableStateOf(entry.stressLevel) }
  var anxietyLevel by remember(entry.id) { mutableStateOf(entry.anxietyLevel) }
  var physicalActivity by remember(entry.id) { mutableStateOf(entry.physicalActivity) }

  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  val moodLevel = MoodLevel.fromScore(mood)
  val moodColor = moodLevel.color
  val accentColor = if (colors.isDark) moodColor else colors.accentPrimary

  fun addTag(raw: String) {
    val clean = raw.trim().lowercase().removePrefix("#")
    if (clean.isNotEmpty() && clean !in tags) {
      tags = tags + clean
    }
    newTagText = ""
  }

  fun syncSleepForDateIfUnedited(newTimestamp: Long) {
    if (!sleepEdited) {
      val dayKey = DateTimeUtils.formatFileDate(newTimestamp)
      val dayCtx = dailyContexts[dayKey]
      sleepDurationMinutes = dayCtx?.sleepDurationMinutes
      sleepQuality = dayCtx?.sleepQuality
    }
  }

  val contextSummary = remember(
    sleepDurationMinutes,
    sleepQuality,
    energyLevel,
    stressLevel,
    anxietyLevel,
    physicalActivity,
  ) {
    buildList {
      sleepDurationMinutes?.let { mins ->
        val h = mins / 60
        val m = mins % 60
        add(if (m == 0) "${h}h sleep" else "${h}h ${m}m sleep")
      }
      sleepQuality?.let { add("Sleep $it/5") }
      energyLevel?.let { add("Energy $it/5") }
      stressLevel?.let { add("Stress $it/5") }
      anxietyLevel?.let { add("Anxiety $it/5") }
      physicalActivity?.let { add(it.label) }
    }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
  }

  ModalBottomSheet(
    onDismissRequest = {
      if (
        shouldConfirmEntrySheetValueChange(
          targetValue = SheetValue.Hidden,
          isImeVisible = latestIsImeVisible.value,
          isImeClosing = isImeClosingState.value,
          isUserDraggingSheet = isUserDraggingSheetState.value,
          isExplicitDismissRequested = isExplicitDismissRequestedState.value,
        )
      ) {
        onDismiss()
      }
    },
    sheetState = sheetState,
    properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    dragHandle = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .trackSheetDrag(
            isDragHandle = true,
            canScrollBackward = { false },
            onDragStart = { markUserDraggingSheet(true) },
            onDragEnd = { markUserDraggingSheet(false) },
          )
          .testTag("entry_sheet_drag_handle"),
        contentAlignment = Alignment.Center,
      ) {
        BottomSheetDefaults.DragHandle()
      }
    },
    containerColor = colors.surfaceBase,
    contentColor = colors.textPrimary,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    BackHandler(
      enabled = !showDatePicker && !showTimePicker && !showDeleteConfirmation,
    ) {
      when (resolveEntrySheetBackAction(isImeVisible = latestIsImeVisible.value)) {
        EntrySheetBackAction.HIDE_KEYBOARD_ONLY -> {
          triggerImeClosingGuard()
          keyboardController?.hide()
          focusManager.clearFocus()
        }
        EntrySheetBackAction.DISMISS_SHEET -> {
          isExplicitDismissRequestedState.value = true
          scope.launch {
            sheetState.hide()
          }.invokeOnCompletion {
            if (!sheetState.isVisible) {
              onDismiss()
            } else {
              isExplicitDismissRequestedState.value = false
            }
          }
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .trackSheetDrag(
          isDragHandle = false,
          canScrollBackward = { scrollState.canScrollBackward },
          onDragStart = { markUserDraggingSheet(true) },
          onDragEnd = { markUserDraggingSheet(false) },
        )
        .dismissKeyboardOnOutsideTap {
          if (latestIsImeVisible.value || isNoteFocused || isTagFocused) {
            triggerImeClosingGuard()
            keyboardController?.hide()
            focusManager.clearFocus()
          }
        }
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp)
        .padding(bottom = 24.dp)
        .navigationBarsPadding()
        .testTag("entry_detail_sheet_content"),
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
            color = colors.textPrimary,
          )
          Text(
            text = "Feeling ${moodLevel.label}",
            style = MaterialTheme.typography.bodySmall,
            color = if (colors.isDark) moodColor else colors.textSecondary,
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
            tint = colors.danger,
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
        color = colors.textSecondary,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          onClick = { showDatePicker = true },
          shape = RoundedCornerShape(14.dp),
          color = colors.cardSurface,
          border = BorderStroke(1.dp, colors.border),
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
              tint = accentColor,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = DateTimeUtils.formatFullDate(timestamp),
              style = MaterialTheme.typography.bodyMedium,
              color = colors.textPrimary,
            )
          }
        }

        Surface(
          onClick = { showTimePicker = true },
          shape = RoundedCornerShape(14.dp),
          color = colors.cardSurface,
          border = BorderStroke(1.dp, colors.border),
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
              tint = accentColor,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = DateTimeUtils.formatTime(timestamp),
              style = MaterialTheme.typography.labelSmall,
              color = colors.textPrimary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Note editor
      Text(
        text = "Note",
        style = MaterialTheme.typography.labelLarge,
        color = colors.textSecondary,
      )
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        placeholder = {
          Text(
            text = "What's shaping your mood right now?",
            color = colors.textMuted,
          )
        },
        minLines = 3,
        maxLines = 6,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = accentColor.copy(alpha = 0.75f),
          unfocusedBorderColor = colors.borderBright,
          focusedContainerColor = colors.cardSurface,
          unfocusedContainerColor = colors.cardSurface,
          focusedTextColor = colors.textPrimary,
          unfocusedTextColor = colors.textPrimary,
          cursorColor = accentColor,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .onFocusChanged { focusState ->
            isNoteFocused = focusState.isFocused
          }
          .testTag("entry_note_input"),
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Tags editor
      Text(
        text = "Tags & Emotions",
        style = MaterialTheme.typography.labelLarge,
        color = colors.textSecondary,
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
                  color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Outlined.Close,
                  contentDescription = "Remove tag $tag",
                  tint = accentColor,
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
          placeholder = { Text("Add tag (e.g. calm, social)", color = colors.textMuted) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { addTag(newTagText) }),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor.copy(alpha = 0.75f),
            unfocusedBorderColor = colors.borderBright,
            focusedContainerColor = colors.cardSurface,
            unfocusedContainerColor = colors.cardSurface,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = accentColor,
          ),
          modifier = Modifier
            .weight(1f)
            .onFocusChanged { focusState ->
              isTagFocused = focusState.isFocused
            }
            .testTag("entry_tag_input"),
        )
        Surface(
          onClick = { addTag(newTagText) },
          shape = RoundedCornerShape(14.dp),
          color = colors.cardSurfaceElevated,
          border = BorderStroke(1.dp, colors.borderBright),
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
              tint = accentColor,
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
          color = colors.textMuted,
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
                .background(colors.cardSurface)
                .clickable { addTag(suggestion) }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
              Text(
                text = "+ $suggestion",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Collapsible Additional Context Section (collapsed by default)
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderBright),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .clickable { isContextExpanded = !isContextExpanded }
              .padding(horizontal = 14.dp, vertical = 12.dp)
              .testTag("additional_context_toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Additional Context",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
              )
              Text(
                text = contextSummary ?: "Optional sleep, wellbeing & physical activity",
                style = MaterialTheme.typography.bodySmall,
                color = if (contextSummary != null) accentColor else colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = if (isContextExpanded) {
                Icons.Outlined.KeyboardArrowUp
              } else {
                Icons.Outlined.KeyboardArrowDown
              },
              contentDescription = if (isContextExpanded) {
                "Collapse Additional Context"
              } else {
                "Expand Additional Context"
              },
              tint = colors.textSecondary,
            )
          }

          AnimatedVisibility(
            visible = isContextExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              HorizontalDivider(color = colors.border)

              // Sleep Group
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "Sleep",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                  )
                  Text(
                    text = "Daily measurement",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                  )
                }

                SleepDurationEditor(
                  durationMinutes = sleepDurationMinutes,
                  accent = accentColor,
                  onUpdateDuration = { newMinutes ->
                    sleepDurationMinutes = newMinutes
                    sleepEdited = true
                  },
                )

                RatingFiveSelector(
                  label = "Sleep quality",
                  selectedValue = sleepQuality,
                  lowHint = "Poor",
                  highHint = "Restful",
                  accent = accentColor,
                  tagPrefix = "sleep_quality",
                  onSelect = { selected ->
                    sleepQuality = if (sleepQuality == selected) null else selected
                    sleepEdited = true
                  },
                )
              }

              HorizontalDivider(color = colors.border)

              // Wellbeing Group
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                  text = "Wellbeing",
                  style = MaterialTheme.typography.labelLarge,
                  color = colors.textSecondary,
                )

                RatingFiveSelector(
                  label = "Energy level",
                  selectedValue = energyLevel,
                  lowHint = "Drained",
                  highHint = "Vibrant",
                  accent = accentColor,
                  tagPrefix = "energy_level",
                  onSelect = { selected ->
                    energyLevel = if (energyLevel == selected) null else selected
                  },
                )

                RatingFiveSelector(
                  label = "Stress level",
                  selectedValue = stressLevel,
                  lowHint = "Calm",
                  highHint = "High",
                  accent = accentColor,
                  tagPrefix = "stress_level",
                  onSelect = { selected ->
                    stressLevel = if (stressLevel == selected) null else selected
                  },
                )

                RatingFiveSelector(
                  label = "Anxiety level",
                  selectedValue = anxietyLevel,
                  lowHint = "At ease",
                  highHint = "Intense",
                  accent = accentColor,
                  tagPrefix = "anxiety_level",
                  onSelect = { selected ->
                    anxietyLevel = if (anxietyLevel == selected) null else selected
                  },
                )
              }

              HorizontalDivider(color = colors.border)

              // Physical Activity Group
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = "Physical Activity",
                  style = MaterialTheme.typography.labelLarge,
                  color = colors.textSecondary,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  PhysicalActivityLevel.entries.forEach { level ->
                    val selected = physicalActivity == level
                    Surface(
                      onClick = {
                        physicalActivity = if (selected) null else level
                      },
                      shape = RoundedCornerShape(12.dp),
                      color = if (selected) accentColor.copy(alpha = 0.20f) else colors.cardSurfaceElevated,
                      border = BorderStroke(
                        1.dp,
                        if (selected) accentColor.copy(alpha = 0.7f) else colors.border,
                      ),
                      modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("activity_${level.name.lowercase()}"),
                    ) {
                      Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                      ) {
                        Text(
                          text = level.label,
                          style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                          ),
                          color = if (selected) accentColor else colors.textSecondary,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      Button(
        onClick = {
          onSave(
            entry.copy(
              mood = mood,
              timestamp = timestamp,
              note = note,
              tags = tags,
              sleepDurationMinutes = sleepDurationMinutes,
              sleepQuality = sleepQuality,
              energyLevel = energyLevel,
              stressLevel = stressLevel,
              anxietyLevel = anxietyLevel,
              physicalActivity = physicalActivity,
            ),
            sleepEdited,
          )
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = moodColor,
          contentColor = Color(0xFF07080C),
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
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        timestamp = DateTimeUtils.combineDateAndTime(
          dateUtcMillis = utcMillis,
          hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
          minute = cal.get(Calendar.MINUTE),
        )
        syncSleepForDateIfUnedited(timestamp)
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
        syncSleepForDateIfUnedited(timestamp)
        showTimePicker = false
      },
      onDismiss = { showTimePicker = false },
    )
  }

  if (showDeleteConfirmation) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmation = false },
      containerColor = colors.cardSurfaceElevated,
      title = {
        Text("Delete this entry?", color = colors.textPrimary)
      },
      text = {
        Text(
          text = "This mood log from ${DateTimeUtils.formatDateAndTime(timestamp)} will be permanently removed.",
          color = colors.textSecondary,
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
          Text("Delete", color = colors.danger)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmation = false }) {
          Text("Cancel", color = colors.textSecondary)
        }
      },
    )
  }
}

private fun Modifier.trackSheetDrag(
  isDragHandle: Boolean,
  canScrollBackward: () -> Boolean,
  onDragStart: () -> Unit,
  onDragEnd: () -> Unit,
): Modifier = pointerInput(isDragHandle) {
  val touchSlop = viewConfiguration.touchSlop
  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    var dragStarted = false
    var accumulatedDownwardDy = 0f
    while (true) {
      val event = awaitPointerEvent(PointerEventPass.Initial)
      val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
      if (change == null || !change.pressed) {
        break
      }
      val dy = change.position.y - change.previousPosition.y
      if (isDragHandle || !canScrollBackward()) {
        if (dy > 0f) {
          accumulatedDownwardDy += dy
          if (!dragStarted && accumulatedDownwardDy > touchSlop) {
            dragStarted = true
            onDragStart()
          }
        } else if (dy < 0f && !dragStarted) {
          accumulatedDownwardDy = (accumulatedDownwardDy + dy).coerceAtLeast(0f)
        }
      } else {
        accumulatedDownwardDy = 0f
      }
    }
    if (dragStarted) {
      onDragEnd()
    }
  }
}

private fun Modifier.dismissKeyboardOnOutsideTap(
  onTapOutside: () -> Unit,
): Modifier = pointerInput(Unit) {
  val touchSlop = viewConfiguration.touchSlop
  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    var moved = false
    var totalDx = 0f
    var totalDy = 0f
    while (true) {
      val event = awaitPointerEvent(PointerEventPass.Main)
      val change = event.changes.firstOrNull { it.id == down.id } ?: break
      totalDx += change.position.x - change.previousPosition.x
      totalDy += change.position.y - change.previousPosition.y
      if (hypot(totalDx, totalDy) > touchSlop) {
        moved = true
      }
      if (!change.pressed) {
        if (!moved && !change.isConsumed) {
          onTapOutside()
        }
        break
      }
    }
  }
}

@Composable
private fun SleepDurationEditor(
  durationMinutes: Int?,
  accent: Color,
  onUpdateDuration: (Int?) -> Unit,
) {
  val colors = MoodyTheme.colors
  val hours = durationMinutes?.div(60)
  val minutes = durationMinutes?.rem(60)

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Sleep duration",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textPrimary,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = if (durationMinutes != null) "${hours}h ${minutes}m" else "Not set",
          style = MaterialTheme.typography.labelMedium,
          color = if (durationMinutes != null) accent else colors.textMuted,
          modifier = Modifier.testTag("sleep_duration_readout"),
        )
        if (durationMinutes != null) {
          Text(
            text = "Clear",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable { onUpdateDuration(null) }
              .padding(horizontal = 6.dp, vertical = 4.dp)
              .testTag("clear_sleep_duration"),
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      CompactStepperPill(
        label = "Hours",
        valueText = if (hours != null) "${hours}h" else "--",
        onMinus = {
          val cur = durationMinutes ?: (7 * 60)
          val nextH = ((cur / 60) - 1).coerceAtLeast(0)
          onUpdateDuration(nextH * 60 + (cur % 60))
        },
        onPlus = {
          val cur = durationMinutes ?: (7 * 60)
          val nextH = ((cur / 60) + 1).coerceAtMost(23)
          onUpdateDuration(nextH * 60 + (cur % 60))
        },
        minusTag = "sleep_hours_minus",
        plusTag = "sleep_hours_plus",
        modifier = Modifier.weight(1f),
      )

      CompactStepperPill(
        label = "Minutes",
        valueText = if (minutes != null) "${minutes}m" else "--",
        onMinus = {
          val cur = durationMinutes ?: (7 * 60)
          val h = cur / 60
          val m = cur % 60
          val nextM = if (m >= 5) ((m - 1) / 5) * 5 else 55
          onUpdateDuration(h * 60 + nextM)
        },
        onPlus = {
          val cur = durationMinutes ?: (7 * 60)
          val h = cur / 60
          val m = cur % 60
          val nextM = ((m / 5) + 1) * 5 % 60
          onUpdateDuration(h * 60 + nextM)
        },
        minusTag = "sleep_minutes_minus",
        plusTag = "sleep_minutes_plus",
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun CompactStepperPill(
  label: String,
  valueText: String,
  onMinus: () -> Unit,
  onPlus: () -> Unit,
  minusTag: String,
  plusTag: String,
  modifier: Modifier = Modifier,
) {
  val colors = MoodyTheme.colors
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = colors.cardSurfaceElevated,
    border = BorderStroke(1.dp, colors.border),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(
        onClick = onMinus,
        modifier = Modifier
          .size(40.dp)
          .testTag(minusTag),
      ) {
        Icon(
          imageVector = Icons.Outlined.Remove,
          contentDescription = "Decrease $label",
          tint = colors.textSecondary,
          modifier = Modifier.size(18.dp),
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = valueText,
          style = MaterialTheme.typography.titleMedium,
          color = colors.textPrimary,
          textAlign = TextAlign.Center,
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = colors.textMuted,
        )
      }

      IconButton(
        onClick = onPlus,
        modifier = Modifier
          .size(40.dp)
          .testTag(plusTag),
      ) {
        Icon(
          imageVector = Icons.Outlined.Add,
          contentDescription = "Increase $label",
          tint = colors.textSecondary,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun RatingFiveSelector(
  label: String,
  selectedValue: Int?,
  lowHint: String,
  highHint: String,
  accent: Color,
  tagPrefix: String,
  onSelect: (Int) -> Unit,
) {
  val colors = MoodyTheme.colors
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textPrimary,
      )
      Text(
        text = selectedValue?.let { "$it / 5" } ?: "$lowHint – $highHint",
        style = MaterialTheme.typography.labelSmall,
        color = if (selectedValue != null) accent else colors.textMuted,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      (1..5).forEach { value ->
        val isSelected = selectedValue == value
        Surface(
          onClick = { onSelect(value) },
          shape = RoundedCornerShape(12.dp),
          color = if (isSelected) accent.copy(alpha = 0.22f) else colors.cardSurfaceElevated,
          border = BorderStroke(
            1.dp,
            if (isSelected) accent.copy(alpha = 0.75f) else colors.border,
          ),
          modifier = Modifier
            .weight(1f)
            .height(42.dp)
            .testTag("${tagPrefix}_$value"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = value.toString(),
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              ),
              color = if (isSelected) accent else colors.textSecondary,
            )
          }
        }
      }
    }
  }
}
