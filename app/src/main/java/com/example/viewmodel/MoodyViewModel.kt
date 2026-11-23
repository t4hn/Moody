package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ImportValidationResult
import com.example.data.MoodBackupSerializer
import com.example.data.MoodRepository
import com.example.data.MoodyDatabase
import com.example.data.MoodyPreferences
import com.example.model.MoodEntry
import com.example.model.MoodLevel
import com.example.util.DateTimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MoodyViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: MoodRepository = MoodRepository(
    dao = MoodyDatabase.getInstance(application).moodEntryDao(),
    preferences = MoodyPreferences(application),
  )

  private data class ViewControls(
    val customQuickTimestamp: Long? = null,
    val lastLoggedPulseId: String? = null,
    val quickToast: QuickLogToastState? = null,
    val editingEntry: MoodEntry? = null,
    val insightRange: InsightTimeRange = InsightTimeRange.WEEK,
    val selectedHeatmapDayStart: Long? = null,
    val timeOfDayMode: TimeOfDayMode = TimeOfDayMode.PART_OF_DAY,
    val searchQuery: String = "",
    val selectedFilterTags: Set<String> = emptySet(),
    val dateFilter: HistoryDateFilter = HistoryDateFilter.ALL,
    val customFilterDayMillis: Long? = null,
    val importDialogState: ImportDialogState? = null,
    val statusMessage: String? = null,
  )

  private val controls = MutableStateFlow(ViewControls())
  private var toastDismissJob: Job? = null
  private var statusDismissJob: Job? = null

  val uiState: StateFlow<MoodyUiState> = combine(
    repository.allEntries,
    repository.demoEntryCount,
    controls,
  ) { entries, demoCount, ctrl ->
    val now = System.currentTimeMillis()
    val todayStart = DateTimeUtils.startOfDayMillis(now)
    val todayEnd = DateTimeUtils.endOfDayMillis(now)
    val todayEntries = entries.filter { it.timestamp in todayStart..todayEnd }

    val stats = MoodAnalytics.computeStats(entries, now)
    val linePoints = MoodAnalytics.computeLineGraphPoints(entries, ctrl.insightRange, now)
    val heatmap = MoodAnalytics.computeCalendarHeatmap(entries, weeks = 12, nowMillis = now)
    val selectedDayCell = ctrl.selectedHeatmapDayStart?.let { selectedStart ->
      heatmap.firstOrNull { it.dayStartMillis == selectedStart }
    } ?: heatmap.lastOrNull { !it.isFuture && it.entryCount > 0 }
    val todBuckets = MoodAnalytics.computeTimeOfDayBuckets(entries, ctrl.timeOfDayMode)
    val suggestedTags = MoodAnalytics.computeSuggestedTags(entries)
    val allUsedTags = entries
      .flatMap { it.tags }
      .map { it.trim().lowercase() }
      .filter { it.isNotEmpty() }
      .groupingBy { it }
      .eachCount()
      .entries
      .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
      .map { it.key }

    val filteredHistory = filterEntries(
      entries = entries,
      query = ctrl.searchQuery,
      selectedTags = ctrl.selectedFilterTags,
      dateFilter = ctrl.dateFilter,
      customDayMillis = ctrl.customFilterDayMillis,
      nowMillis = now,
    )

    MoodyUiState(
      allEntries = entries,
      todayEntries = todayEntries,
      demoEntryCount = demoCount,
      customQuickTimestamp = ctrl.customQuickTimestamp,
      lastLoggedPulseId = ctrl.lastLoggedPulseId,
      quickToast = ctrl.quickToast,
      editingEntry = ctrl.editingEntry,
      suggestedTags = suggestedTags,
      allUsedTags = allUsedTags,
      stats = stats,
      insightRange = ctrl.insightRange,
      lineGraphPoints = linePoints,
      heatmapCells = heatmap,
      selectedHeatmapDay = selectedDayCell,
      timeOfDayMode = ctrl.timeOfDayMode,
      timeOfDayBuckets = todBuckets,
      searchQuery = ctrl.searchQuery,
      selectedFilterTags = ctrl.selectedFilterTags,
      dateFilter = ctrl.dateFilter,
      customFilterDayMillis = ctrl.customFilterDayMillis,
      filteredHistoryEntries = filteredHistory,
      importDialogState = ctrl.importDialogState,
      statusMessage = ctrl.statusMessage,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = MoodyUiState(),
  )

  init {
    viewModelScope.launch {
      repository.seedDemoDataIfFirstLaunch()
    }
  }

  fun quickLogMood(moodScore: Int) {
    val now = System.currentTimeMillis()
    val chosenTimestamp = controls.value.customQuickTimestamp ?: now
    val newEntry = MoodEntry(
      id = UUID.randomUUID().toString(),
      mood = moodScore.coerceIn(1, 5),
      timestamp = chosenTimestamp,
      note = "",
      tags = emptyList(),
      isDemo = false,
      createdAt = now,
      updatedAt = now,
    )
    val level = MoodLevel.fromScore(newEntry.mood)
    viewModelScope.launch {
      repository.upsertEntry(newEntry)
      controls.value = controls.value.copy(
        customQuickTimestamp = null,
        lastLoggedPulseId = newEntry.id,
        quickToast = QuickLogToastState(
          entry = newEntry,
          message = "Logged ${level.label} · ${DateTimeUtils.formatTime(newEntry.timestamp)}",
        ),
      )
      scheduleToastDismiss()
    }
  }

  private fun scheduleToastDismiss() {
    toastDismissJob?.cancel()
    toastDismissJob = viewModelScope.launch {
      delay(4500)
      controls.value = controls.value.copy(quickToast = null)
    }
  }

  fun dismissQuickToast() {
    toastDismissJob?.cancel()
    controls.value = controls.value.copy(quickToast = null)
  }

  fun undoQuickToast() {
    val toast = controls.value.quickToast ?: return
    toastDismissJob?.cancel()
    viewModelScope.launch {
      repository.deleteEntry(toast.entry.id)
      controls.value = controls.value.copy(quickToast = null)
    }
  }

  fun openEntryDetailsFromToast() {
    val toast = controls.value.quickToast ?: return
    toastDismissJob?.cancel()
    controls.value = controls.value.copy(
      quickToast = null,
      editingEntry = toast.entry,
    )
  }

  fun setCustomQuickTimestamp(timestampMillis: Long?) {
    controls.value = controls.value.copy(customQuickTimestamp = timestampMillis)
  }

  fun openEntryEditor(entry: MoodEntry) {
    controls.value = controls.value.copy(editingEntry = entry)
  }

  fun closeEntryEditor() {
    controls.value = controls.value.copy(editingEntry = null)
  }

  fun saveEditedEntry(entry: MoodEntry) {
    viewModelScope.launch {
      val normalizedTags = entry.tags
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()
      repository.upsertEntry(
        entry.copy(
          tags = normalizedTags,
          note = entry.note.trim(),
          updatedAt = System.currentTimeMillis(),
        ),
      )
      controls.value = controls.value.copy(editingEntry = null)
    }
  }

  fun deleteEntry(entryId: String) {
    viewModelScope.launch {
      repository.deleteEntry(entryId)
      controls.value = controls.value.copy(editingEntry = null)
    }
  }

  fun setInsightRange(range: InsightTimeRange) {
    controls.value = controls.value.copy(insightRange = range)
  }

  fun selectHeatmapDay(dayStartMillis: Long) {
    controls.value = controls.value.copy(selectedHeatmapDayStart = dayStartMillis)
  }

  fun setTimeOfDayMode(mode: TimeOfDayMode) {
    controls.value = controls.value.copy(timeOfDayMode = mode)
  }

  fun setSearchQuery(query: String) {
    controls.value = controls.value.copy(searchQuery = query)
  }

  fun toggleFilterTag(tag: String) {
    val clean = tag.trim().lowercase()
    val current = controls.value.selectedFilterTags
    val updated = if (clean in current) current - clean else current + clean
    controls.value = controls.value.copy(selectedFilterTags = updated)
  }

  fun setDateFilter(filter: HistoryDateFilter, customDayMillis: Long? = controls.value.customFilterDayMillis) {
    controls.value = controls.value.copy(
      dateFilter = filter,
      customFilterDayMillis = if (filter == HistoryDateFilter.CUSTOM_DAY) customDayMillis else null,
    )
  }

  fun clearHistoryFilters() {
    controls.value = controls.value.copy(
      searchQuery = "",
      selectedFilterTags = emptySet(),
      dateFilter = HistoryDateFilter.ALL,
      customFilterDayMillis = null,
    )
  }

  fun buildJsonExport(): String = MoodBackupSerializer.exportJson(uiState.value.allEntries)

  fun buildCsvExport(): String = MoodBackupSerializer.exportCsv(uiState.value.allEntries)

  fun handleJsonImportRaw(rawContent: String) {
    when (val result = MoodBackupSerializer.parseAndValidateJson(rawContent)) {
      is ImportValidationResult.Valid -> {
        controls.value = controls.value.copy(
          importDialogState = ImportDialogState(validEntries = result.entries),
        )
      }
      is ImportValidationResult.Invalid -> {
        controls.value = controls.value.copy(
          importDialogState = ImportDialogState(errorMessage = result.errorMessage),
        )
      }
    }
  }

  fun confirmImport(replaceExisting: Boolean) {
    val entries = controls.value.importDialogState?.validEntries ?: return
    viewModelScope.launch {
      if (replaceExisting) {
        repository.replaceWithImportedEntries(entries)
        showStatus("Replaced journal with ${entries.size} imported entries.")
      } else {
        repository.mergeImportedEntries(entries)
        showStatus("Merged ${entries.size} entries into your journal.")
      }
      controls.value = controls.value.copy(importDialogState = null)
    }
  }

  fun dismissImportDialog() {
    controls.value = controls.value.copy(importDialogState = null)
  }

  fun clearDemoData() {
    viewModelScope.launch {
      repository.clearDemoData()
      showStatus("Demo entries cleared.")
    }
  }

  fun restoreDemoData() {
    viewModelScope.launch {
      repository.restoreDemoData()
      showStatus("Demo entries restored.")
    }
  }

  fun deleteAllEntries() {
    viewModelScope.launch {
      repository.deleteAllEntries()
      showStatus("All entries deleted.")
    }
  }

  fun showStatus(message: String) {
    controls.value = controls.value.copy(statusMessage = message)
    statusDismissJob?.cancel()
    statusDismissJob = viewModelScope.launch {
      delay(4000)
      controls.value = controls.value.copy(statusMessage = null)
    }
  }

  fun clearStatus() {
    statusDismissJob?.cancel()
    controls.value = controls.value.copy(statusMessage = null)
  }

  private fun filterEntries(
    entries: List<MoodEntry>,
    query: String,
    selectedTags: Set<String>,
    dateFilter: HistoryDateFilter,
    customDayMillis: Long?,
    nowMillis: Long,
  ): List<MoodEntry> {
    val q = query.trim().lowercase()
    val todayStart = DateTimeUtils.startOfDayMillis(nowMillis)
    val todayEnd = DateTimeUtils.endOfDayMillis(nowMillis)

    return entries.filter { entry ->
      val matchesQuery = if (q.isEmpty()) {
        true
      } else {
        entry.note.lowercase().contains(q) ||
          entry.tags.any { it.lowercase().contains(q) } ||
          entry.moodLevel.label.lowercase().contains(q)
      }

      val matchesTags = if (selectedTags.isEmpty()) {
        true
      } else {
        val entryTagsLower = entry.tags.map { it.lowercase() }.toSet()
        selectedTags.all { it in entryTagsLower }
      }

      val matchesDate = when (dateFilter) {
        HistoryDateFilter.ALL -> true
        HistoryDateFilter.TODAY -> entry.timestamp in todayStart..todayEnd
        HistoryDateFilter.LAST_7_DAYS -> entry.timestamp >= DateTimeUtils.daysAgoStartMillis(6, nowMillis)
        HistoryDateFilter.LAST_30_DAYS -> entry.timestamp >= DateTimeUtils.daysAgoStartMillis(29, nowMillis)
        HistoryDateFilter.CUSTOM_DAY -> {
          if (customDayMillis == null) true
          else {
            val dayStart = DateTimeUtils.startOfDayMillis(customDayMillis)
            val dayEnd = DateTimeUtils.endOfDayMillis(customDayMillis)
            entry.timestamp in dayStart..dayEnd
          }
        }
      }

      matchesQuery && matchesTags && matchesDate
    }
  }
}
